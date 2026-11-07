package io.github.aaronfilson.studiomanager.apitoken;

import static java.util.Objects.requireNonNull;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.ApiTokenResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.platform.tenancy.TokenScope;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
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

  private static final int SECRET_LENGTH = 43;
  private static final String ALPHABET =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final JdbcClient jdbc;
  private final AccountDirectory accounts;
  private final ApplicationEventPublisher events;

  ApiTokens(JdbcClient jdbc, AccountDirectory accounts, ApplicationEventPublisher events) {
    this.jdbc = jdbc;
    this.accounts = accounts;
    this.events = events;
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
    return jdbc.sql(
            """
            select token_id, account_id, studio_id, scopes from app.use_api_token(:hash, :now, :from)
            """)
        .param("hash", hash(bearer))
        .param("now", now().atOffset(ZoneOffset.UTC))
        .param("from", fromAddress)
        .query((rs, row) ->
            new ResolvedToken(
                rs.getObject("token_id", UUID.class),
                rs.getObject("account_id", UUID.class),
                rs.getObject("studio_id", UUID.class),
                scopes(rs.getArray("scopes"))))
        .optional();
  }

  /** The caller's own tokens; the studio's owner sees everyone's. */
  @Transactional(readOnly = true)
  public List<TokenView> list() {
    TenantContext caller = requireStaff();
    boolean owner = caller.role() == StudioRole.OWNER;
    List<TokenView> tokens =
        jdbc.sql(
                SELECT
                    + (owner ? "" : " where account_id = :me")
                    + " order by created_at desc, id desc")
            .param("me", caller.accountId())
            .query((rs, row) -> view(rs, null))
            .list();
    Map<UUID, String> names =
        accounts.displayNames(tokens.stream().map(TokenView::ownerAccountId).toList());
    return tokens.stream().map(t -> named(t, names.get(t.ownerAccountId()))).toList();
  }

  @Transactional
  public Created create(String name, Set<TokenScope> scopes, @Nullable Integer expiresInDays) {
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
    Integer live =
        jdbc.sql(
                """
                select count(*) from api_token
                where account_id = :me and revoked_at is null and %s > :now
                """
                    .formatted(EXPIRES_AT))
            .param("me", caller.accountId())
            .param("now", now().atOffset(ZoneOffset.UTC))
            .query(Integer.class)
            .single();
    if (live >= MAX_LIVE_PER_PERSON) {
      throw new InvalidInputException(
          "You have %d live tokens. Revoke one first.".formatted(MAX_LIVE_PER_PERSON));
    }
    String secret = newSecret();
    UUID id = Uuids.v7();
    Instant now = now();
    jdbc.sql(
            """
            insert into api_token (id, studio_id, account_id, name, token_hash, last_four, scopes, created_at,
                                   expires_at)
            values (:id, :studioId, :me, :name, :hash, :lastFour, :scopes, :now, :expires)
            """)
        .param("id", id)
        .param("studioId", caller.studioId())
        .param("me", caller.accountId())
        .param("name", cleanName)
        .param("hash", hash(secret))
        .param("lastFour", secret.substring(secret.length() - 4))
        .param("scopes", scopes.stream().map(TokenScope::wireName).sorted().toArray(String[]::new))
        .param("now", now.atOffset(ZoneOffset.UTC))
        .param("expires", now.plus(Duration.ofDays(days)).atOffset(ZoneOffset.UTC))
        .update();
    return new Created(get(id), secret);
  }

  /** Stops a token working at once. Its owner can revoke it, and so can the studio's owner. */
  @Transactional
  public void revoke(UUID id) {
    TenantContext caller = requireStaff();
    TokenView token = get(id);
    if (!token.ownerAccountId().equals(caller.accountId()) && caller.role() != StudioRole.OWNER) {
      throw new AccessDeniedException("Only its owner, or the studio's owner, can revoke a token.");
    }
    int revoked =
        jdbc.sql("update api_token set revoked_at = :now where id = :id and revoked_at is null")
            .param("now", now().atOffset(ZoneOffset.UTC))
            .param("id", id)
            .update();
    if (revoked > 0) {
      // Its owner hears about it, by email after the commit (ApiTokenRevokedEmails)
      events.publishEvent(
          new ApiTokenRevoked(
              token.id(),
              requireNonNull(caller.studioId()),
              token.ownerAccountId(),
              token.name(),
              caller.accountId()));
    }
  }

  /**
   * The names of these tokens in the bound studio, for saying which token did something ("through
   * 'Claude desktop'"). Revoked tokens keep their names; ids not found are left out.
   */
  @Transactional(readOnly = true)
  public Map<UUID, String> names(Collection<UUID> tokenIds) {
    if (tokenIds.isEmpty()) {
      return Map.of();
    }
    return jdbc
        .sql("select id, name from api_token where id in (:ids)")
        .param("ids", List.copyOf(tokenIds))
        .query((rs, row) -> Map.entry(rs.getObject("id", UUID.class), rs.getString("name")))
        .list()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private TokenView get(UUID id) {
    TokenView token =
        jdbc.sql(SELECT + " where id = :id")
            .param("id", id)
            .query((rs, row) -> view(rs, null))
            .optional()
            .orElseThrow(() -> new NotFoundException("No such token."));
    return named(
        token, accounts.displayNames(List.of(token.ownerAccountId())).get(token.ownerAccountId()));
  }

  /**
   * When a token stops working: its set expiry, or 90 days after its last use (or its creation, if
   * never used), whichever comes first. {@code app.use_api_token} applies the same rule.
   */
  private static final String EXPIRES_AT =
      "least(expires_at, coalesce(last_used_at, created_at) + interval '%d days')"
          .formatted(UNUSED_DAYS);

  private static final String SELECT =
      """
      select id, name, account_id, scopes, last_four, created_at, %s as expires_at, last_used_at,
             last_used_from, revoked_at
      from api_token
      """
          .formatted(EXPIRES_AT);

  private static TokenView view(ResultSet rs, @Nullable String ownerName) throws SQLException {
    return new TokenView(
        rs.getObject("id", UUID.class),
        rs.getString("name"),
        rs.getObject("account_id", UUID.class),
        ownerName,
        scopes(rs.getArray("scopes")),
        rs.getString("last_four"),
        instant(rs, "created_at"),
        instant(rs, "expires_at"),
        instantOrNull(rs, "last_used_at"),
        rs.getString("last_used_from"),
        instantOrNull(rs, "revoked_at"));
  }

  private static TokenView named(TokenView t, @Nullable String ownerName) {
    return new TokenView(
        t.id(),
        t.name(),
        t.ownerAccountId(),
        ownerName,
        t.scopes(),
        t.lastFour(),
        t.createdAt(),
        t.expiresAt(),
        t.lastUsedAt(),
        t.lastUsedFrom(),
        t.revokedAt());
  }

  private static Set<TokenScope> scopes(Array array) throws SQLException {
    Set<TokenScope> scopes = EnumSet.noneOf(TokenScope.class);
    Arrays.stream((String[]) array.getArray())
        .map(TokenScope::fromWireName)
        .flatMap(Optional::stream)
        .forEach(scopes::add);
    return scopes;
  }

  private static TenantContext requireStaff() {
    TenantContext caller = TenantContext.require();
    if (caller.role() == null || !caller.role().includes(StudioRole.STAFF)) {
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

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    return rs.getObject(column, OffsetDateTime.class).toInstant();
  }

  private static @Nullable Instant instantOrNull(ResultSet rs, String column) throws SQLException {
    OffsetDateTime at = rs.getObject(column, OffsetDateTime.class);
    return at == null ? null : at.toInstant();
  }

  private static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }
}
