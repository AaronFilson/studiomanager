package io.github.aaronfilson.studiomanager.apitoken;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.ApiTokenResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.platform.tenancy.TokenScope;
import jakarta.persistence.EntityManager;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Personal API tokens: staff make them to let a script or an AI assistant act as them in one
 * studio. A token is {@code smpat_} and 43 base62 characters (256 random bits). Only its SHA-256 is
 * stored, so a leaked database doesn't leak working tokens, and the token is shown only once.
 */
@Service
public class ApiTokens implements ApiTokenResolver {

  static final int MAX_LIVE_PER_PERSON = 10;
  static final int DEFAULT_DAYS = 90;

  /** A token unused this long stops working (V30). */
  static final int UNUSED_DAYS = 90;

  private static final Duration UNUSED_FOR = Duration.ofDays(UNUSED_DAYS);
  private static final int SECRET_LENGTH = 43;
  private static final String ALPHABET =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final ApiTokenRepository tokens;
  private final ApiTokenQueries queries;
  private final EntityManager entityManager;
  private final AccountDirectory accounts;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  ApiTokens(
      ApiTokenRepository tokens,
      ApiTokenQueries queries,
      EntityManager entityManager,
      AccountDirectory accounts,
      ApplicationEventPublisher events,
      Clock clock) {
    this.tokens = tokens;
    this.queries = queries;
    this.entityManager = entityManager;
    this.accounts = accounts;
    this.events = events;
    this.clock = clock;
  }

  public record TokenView(
      UUID id,
      String name,
      UUID ownerAccountId,
      @Nullable String ownerName,
      Set<TokenScope> scopes,
      String lastFour,
      Instant createdAt,
      Instant expiresAt,
      @Nullable Instant lastUsedAt,
      @Nullable String lastUsedFrom,
      @Nullable Instant revokedAt) {}

  public record Created(TokenView token, String secret) {}

  @Override
  public Optional<ResolvedToken> resolve(String bearer, String fromAddress) {
    if (!bearer.startsWith(PREFIX) || bearer.length() != PREFIX.length() + SECRET_LENGTH) {
      return Optional.empty();
    }
    return queries.use(hash(bearer), clock.instant(), fromAddress);
  }

  /** The caller's own tokens; the studio's owner sees everyone's. */
  @Transactional(readOnly = true)
  public List<TokenView> list() {
    TenantContext caller = requireStaff();
    List<ApiToken> found =
        caller.role() == StudioRole.OWNER
            ? tokens.findAllNewestFirst()
            : tokens.findOwnNewestFirst(caller.accountId());
    Map<UUID, String> names =
        accounts.displayNames(found.stream().map(ApiToken::accountId).toList());
    return found.stream()
        .map(token -> token.toView(names.get(token.accountId()), UNUSED_FOR))
        .toList();
  }

  @Transactional
  public Created create(
      @Nullable String name, Set<TokenScope> scopes, @Nullable Integer expiresInDays) {
    TenantContext caller = requireStaff();
    String cleanName = name == null ? "" : name.strip();
    if (cleanName.isEmpty() || cleanName.length() > 80) {
      throw new InvalidInputException(
          "Name the token after what will use it, in up to 80 characters.");
    }
    if (scopes.isEmpty()) {
      throw new InvalidInputException("Choose at least one thing the token may do.");
    }
    int days = expiresInDays == null ? DEFAULT_DAYS : expiresInDays;
    if (days < 1 || days > 365) {
      throw new InvalidInputException("A token lasts between 1 and 365 days.");
    }
    Instant now = clock.instant();
    if (tokens.countLive(caller.accountId(), now, now.minus(UNUSED_FOR)) >= MAX_LIVE_PER_PERSON) {
      throw new InvalidInputException(
          "You have %d live tokens. Revoke one first.".formatted(MAX_LIVE_PER_PERSON));
    }
    String secret = newSecret();
    ApiToken token =
        new ApiToken(
            caller.accountId(),
            cleanName,
            hash(secret),
            secret.substring(secret.length() - 4),
            scopes,
            now,
            now.plus(Duration.ofDays(days)));
    // Persisted, not saved: a token has no version, so save would look it up first
    entityManager.persist(token);
    tokens.flush();
    return new Created(token.toView(ownerName(token), UNUSED_FOR), secret);
  }

  /** Stops a token working at once. Its owner can revoke it, and so can the studio's owner. */
  @Transactional
  public void revoke(UUID id) {
    TenantContext caller = requireStaff();
    ApiToken token =
        tokens.findAndLock(id).orElseThrow(() -> new NotFoundException("No such token."));
    if (!token.accountId().equals(caller.accountId()) && caller.role() != StudioRole.OWNER) {
      throw new AccessDeniedException("Only its owner, or the studio's owner, can revoke a token.");
    }
    if (token.revoke(clock.instant())) {
      tokens.flush();
      // Its owner hears about it, by email after the commit (ApiTokenRevokedEmails)
      events.publishEvent(
          new ApiTokenRevoked(
              token.id(),
              caller.requireStudioId(),
              token.accountId(),
              token.name(),
              caller.accountId()));
    }
  }

  private @Nullable String ownerName(ApiToken token) {
    return accounts.displayNames(List.of(token.accountId())).get(token.accountId());
  }

  private static TenantContext requireStaff() {
    TenantContext caller = TenantContext.require();
    StudioRole role = caller.role();
    if (role == null || !role.includes(StudioRole.STAFF)) {
      throw new AccessDeniedException("API tokens are for staff.");
    }
    return caller;
  }

  static String newSecret() {
    // 256 random bits as base62, left-padded to a fixed length
    BigInteger value = new BigInteger(1, randomBytes());
    StringBuilder out = new StringBuilder();
    BigInteger base = BigInteger.valueOf(ALPHABET.length());
    while (value.signum() > 0) {
      BigInteger[] division = value.divideAndRemainder(base);
      out.append(ALPHABET.charAt(division[1].intValue()));
      value = division[0];
    }
    while (out.length() < SECRET_LENGTH) {
      out.append('0');
    }
    return PREFIX + out.reverse();
  }

  private static byte[] randomBytes() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return bytes;
  }

  static byte[] hash(String token) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is always available", e);
    }
  }
}
