package io.github.aaronfilson.studiomanager.ai;

import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.UnauthorizedException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
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

  private final JdbcClient jdbc;
  private final TransactionOperations transactions;
  private final AiKeyCipher cipher;
  private final ClaudeClients claude;

  AiKeys(
      JdbcClient jdbc,
      TransactionOperations transactions,
      AiKeyCipher cipher,
      ClaudeClients claude) {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.cipher = cipher;
    this.claude = claude;
  }

  public Optional<KeyStatus> status() {
    requireOwner();
    return transactions.execute(tx -> find());
  }

  /** Saves (or replaces) the key, untested. Deliberately not echoed back. */
  public KeyStatus set(Provider provider, String apiKey, boolean allowPlatformReaders) {
    TenantContext caller = requireOwner();
    String key = apiKey == null ? "" : apiKey.strip();
    if (!key.startsWith("sk-ant-")
        || key.length() < 20
        || key.length() > 300
        || key.chars().anyMatch(Character::isWhitespace)) {
      throw new InvalidInputException(
          "That doesn't look like an Anthropic API key. It starts with sk-ant-.");
    }
    return transactions.execute(tx -> {
      jdbc.sql(
              """
              insert into ai_provider_key (studio_id, provider, key_ciphertext, last_four, status,
                                           allow_platform_readers, set_by, set_at)
              values (:studio, :provider, :secret, :lastFour, 'UNTESTED', :platformReaders, :by, :now)
              on conflict (studio_id) do update set provider = excluded.provider,
                  key_ciphertext = excluded.key_ciphertext, last_four = excluded.last_four, status = 'UNTESTED',
                  status_reason = null, checked_at = null, allow_platform_readers = excluded.allow_platform_readers,
                  set_by = excluded.set_by, set_at = excluded.set_at
              """)
          .param("studio", caller.studioId())
          .param("provider", provider.name())
          .param("secret", cipher.encrypt(caller.studioId(), key))
          .param("lastFour", key.substring(key.length() - 4))
          .param("platformReaders", allowPlatformReaders)
          .param("by", caller.accountId())
          .param("now", now().atOffset(ZoneOffset.UTC))
          .update();
      return find().orElseThrow();
    });
  }

  /** Deletes the key; the next invoice is read another way, or typed in. */
  public void remove() {
    requireOwner();
    transactions.executeWithoutResult(tx -> jdbc.sql("delete from ai_provider_key").update());
  }

  /**
   * One small call with the key (listing models, which spends no tokens) to find out whether it
   * works. A key the provider refuses is marked so, with the provider's reason.
   */
  public KeyStatus test() {
    TenantContext caller = requireOwner();
    String key =
        transactions.execute(tx ->
            jdbc.sql("select key_ciphertext from ai_provider_key")
                .query(byte[].class)
                .optional()
                .map(stored -> cipher.decrypt(caller.studioId(), stored))
                .orElseThrow(() -> new NotFoundException("The studio has no AI key yet.")));
    Status status;
    String reason;
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
    String why = reason;
    return transactions.execute(tx -> {
      jdbc.sql(
              "update ai_provider_key set status = :status, status_reason = :reason, checked_at = :now")
          .param("status", checked.name())
          .param("reason", why)
          .param("now", now().atOffset(ZoneOffset.UTC))
          .update();
      return find().orElseThrow();
    });
  }

  /**
   * The bound studio's key, for reading one invoice: empty when there is none, or when Anthropic
   * has refused it. For the invoice reader, which runs as a system job, so no owner check here; the
   * key never leaves the server.
   */
  public Optional<StudioKey> keyForReading() {
    UUID studioId = TenantContext.require().requireStudioId();
    return transactions.execute(tx ->
        jdbc.sql("select key_ciphertext from ai_provider_key where status <> 'REJECTED'")
            .query(byte[].class)
            .optional()
            .map(stored -> new StudioKey(cipher.decrypt(studioId, stored))));
  }

  /** Marks the bound studio's key refused, after the provider answered 401 or 403 to a read. */
  public void markRefused(String reason) {
    transactions.executeWithoutResult(tx ->
        jdbc.sql(
                "update ai_provider_key set status = 'REJECTED', status_reason = :reason, checked_at = :now")
            .param("reason", reason)
            .param("now", now().atOffset(ZoneOffset.UTC))
            .update());
  }

  private Optional<KeyStatus> find() {
    return jdbc.sql(
            """
            select provider, last_four, status, status_reason, checked_at, allow_platform_readers, set_at
            from ai_provider_key
            """)
        .query((rs, row) -> {
          OffsetDateTime checked = rs.getObject("checked_at", OffsetDateTime.class);
          return new KeyStatus(
              Provider.valueOf(rs.getString("provider")),
              rs.getString("last_four"),
              Status.valueOf(rs.getString("status")),
              rs.getString("status_reason"),
              checked == null ? null : checked.toInstant(),
              rs.getBoolean("allow_platform_readers"),
              rs.getObject("set_at", OffsetDateTime.class).toInstant());
        })
        .optional();
  }

  private static TenantContext requireOwner() {
    TenantContext caller = TenantContext.require();
    if (caller.role() != StudioRole.OWNER) {
      throw new AccessDeniedException("Only the studio's owner can manage its AI key.");
    }
    return caller;
  }

  private static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }
}
