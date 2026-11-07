package io.github.aaronfilson.studiomanager.ai;

import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.UnauthorizedException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * The studio's own AI key: set, checked and removed by the owner. The key goes in encrypted and
 * never comes back out of the API; only its last four characters are shown.
 */
@Service
public class AiKeys {

  public enum Provider {
    ANTHROPIC
  }

  public enum Status {
    UNTESTED,
    WORKING,
    REJECTED
  }

  public record KeyStatus(
      Provider provider,
      String lastFour,
      Status status,
      @Nullable String reason,
      @Nullable Instant checkedAt,
      boolean allowPlatformReaders,
      Instant setAt) {}

  /**
   * The studio's key, decrypted for one read. Its {@code toString} never shows the key, so it can't
   * leak into a log line or an exception message by accident.
   */
  public record StudioKey(String secret) {
    @Override
    public String toString() {
      return "StudioKey[redacted]";
    }
  }

  private final AiProviderKeyRepository keys;
  private final AiKeyQueries queries;
  private final TransactionOperations transactions;
  private final AiKeyCipher cipher;
  private final ClaudeClients claude;
  private final Clock clock;

  AiKeys(
      AiProviderKeyRepository keys,
      AiKeyQueries queries,
      TransactionOperations transactions,
      AiKeyCipher cipher,
      ClaudeClients claude,
      Clock clock) {
    this.keys = keys;
    this.queries = queries;
    this.transactions = transactions;
    this.cipher = cipher;
    this.claude = claude;
    this.clock = clock;
  }

  public Optional<KeyStatus> status() {
    UUID studioId = requireOwner().requireStudioId();
    return Objects.requireNonNull(
        transactions.execute(tx -> keys.findById(studioId).map(AiProviderKey::toStatus)));
  }

  /** Saves (or replaces) the key, untested. Deliberately not echoed back. */
  public KeyStatus set(Provider provider, @Nullable String apiKey, boolean allowPlatformReaders) {
    TenantContext caller = requireOwner();
    UUID studioId = caller.requireStudioId();
    String key = apiKey == null ? "" : apiKey.strip();
    if (!key.startsWith("sk-ant-")
        || key.length() < 20
        || key.length() > 300
        || key.chars().anyMatch(Character::isWhitespace)) {
      throw new InvalidInputException(
          "That doesn't look like an Anthropic API key. It starts with sk-ant-.");
    }
    return Objects.requireNonNull(
        transactions.execute(tx -> {
          queries.save(
              studioId,
              provider,
              cipher.encrypt(studioId, key),
              key.substring(key.length() - 4),
              allowPlatformReaders,
              caller.accountId(),
              clock.instant());
          return keys.findById(studioId).orElseThrow().toStatus();
        }));
  }

  /** Deletes the key; the next invoice is read another way, or typed in. */
  public void remove() {
    UUID studioId = requireOwner().requireStudioId();
    transactions.executeWithoutResult(tx -> keys.findById(studioId).ifPresent(keys::delete));
  }

  /**
   * One small call with the key (listing models, which spends no tokens) to find out whether it
   * works. A key the provider refuses is marked so, with the provider's reason.
   */
  public KeyStatus test() {
    UUID studioId = requireOwner().requireStudioId();
    String key =
        Objects.requireNonNull(
            transactions.execute(tx ->
                keys.findById(studioId)
                    .map(stored -> cipher.decrypt(studioId, stored.keyCiphertext()))
                    .orElseThrow(() -> new NotFoundException("The studio has no AI key yet."))));
    Status status;
    @Nullable String reason;
    // The call happens outside any transaction: no database connection held while waiting on the
    // network
    var client = claude.forKey(key);
    try {
      client.models().list();
      status = Status.WORKING;
      reason = null;
    } catch (UnauthorizedException | PermissionDeniedException e) {
      status = Status.REJECTED;
      reason =
          "Anthropic refused the key (%d). Check it's active and has API access."
              .formatted(e.statusCode());
    } catch (AnthropicServiceException e) {
      status = Status.UNTESTED;
      reason = "Anthropic answered %d. Try again in a moment.".formatted(e.statusCode());
    } catch (AnthropicIoException e) {
      status = Status.UNTESTED;
      reason = "Couldn't reach Anthropic. Try again in a moment.";
    } finally {
      client.close();
    }
    Status checked = status;
    @Nullable String why = reason;
    return Objects.requireNonNull(
        transactions.execute(tx -> {
          // Removed while the provider was being asked: nothing left to record against
          AiProviderKey stored =
              keys.findById(studioId)
                  .orElseThrow(() -> new NotFoundException("The studio has no AI key yet."));
          stored.checked(checked, why, clock.instant());
          keys.flush();
          return stored.toStatus();
        }));
  }

  /**
   * The bound studio's key, for reading one invoice: empty when there is none, or when Anthropic
   * has refused it. For the invoice reader, which runs as a system job, so no owner check here; the
   * key never leaves the server.
   */
  public Optional<StudioKey> keyForReading() {
    UUID studioId = TenantContext.require().requireStudioId();
    return Objects.requireNonNull(
        transactions.execute(tx ->
            keys.findById(studioId)
                .filter(stored -> !stored.refused())
                .map(stored -> new StudioKey(cipher.decrypt(studioId, stored.keyCiphertext())))));
  }

  /**
   * Whether the bound studio's owner consents to invoices going to the platform's own readers
   * (Google Document AI, Azure AI Document Intelligence). False when the studio has no key row: the
   * consent is saved with the key, so a studio that never set one hasn't given it.
   */
  public boolean platformReadersAllowed() {
    UUID studioId = TenantContext.require().requireStudioId();
    return Boolean.TRUE.equals(
        transactions.execute(
            tx -> keys.findById(studioId).map(AiProviderKey::allowsPlatformReaders).orElse(false)));
  }

  /** Marks the bound studio's key refused, after the provider answered 401 or 403 to a read. */
  public void markRefused(String reason) {
    UUID studioId = TenantContext.require().requireStudioId();
    transactions.executeWithoutResult(tx ->
        keys.findById(studioId)
            .ifPresent(stored -> {
              stored.checked(Status.REJECTED, reason, clock.instant());
              keys.flush();
            }));
  }

  private static TenantContext requireOwner() {
    TenantContext caller = TenantContext.require();
    if (caller.role() != StudioRole.OWNER) {
      throw new AccessDeniedException("Only the studio's owner can manage its AI key.");
    }
    return caller;
  }
}
