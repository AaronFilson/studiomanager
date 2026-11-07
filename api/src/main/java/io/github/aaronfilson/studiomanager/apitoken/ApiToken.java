package io.github.aaronfilson.studiomanager.apitoken;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.TokenScope;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * A personal API token. Only its SHA-256 is stored. Hibernate fills {@code studio_id} (ADR 21).
 *
 * <p>Signing in with a token notes when and where it was used, through {@code app.use_api_token}
 * ({@link ApiTokenQueries}), so updates here write only the columns that changed ({@code
 * DynamicUpdate}) and a revoke can't overwrite that.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "api_token")
@DynamicUpdate
class ApiToken {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID accountId;

  private String name;

  private byte[] tokenHash;

  private String lastFour;

  private String[] scopes;

  private Instant createdAt;

  /**
   * The expiry it was given. It also stops working once unused for {@link ApiTokens#UNUSED_DAYS}
   * days.
   */
  private Instant expiresAt;

  private @Nullable Instant lastUsedAt;

  private @Nullable String lastUsedFrom;

  private @Nullable Instant revokedAt;

  @SuppressWarnings("NullAway.Init")
  protected ApiToken() {}

  ApiToken(
      UUID accountId,
      String name,
      byte[] tokenHash,
      String lastFour,
      Set<TokenScope> scopes,
      Instant createdAt,
      Instant expiresAt) {
    this.id = Uuids.v7();
    this.accountId = accountId;
    this.name = name;
    this.tokenHash = tokenHash;
    this.lastFour = lastFour;
    this.scopes = scopes.stream().map(TokenScope::wireName).sorted().toArray(String[]::new);
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  UUID id() {
    return id;
  }

  UUID accountId() {
    return accountId;
  }

  String name() {
    return name;
  }

  /** Stops it working. Returns false if it already had been revoked. */
  boolean revoke(Instant now) {
    if (revokedAt != null) {
      return false;
    }
    revokedAt = now;
    return true;
  }

  /**
   * When it stops working: its set expiry, or {@code unusedFor} after its last use (or its
   * creation, if never used), whichever comes first. {@code app.use_api_token} applies the same
   * rule.
   */
  Instant stopsWorkingAt(Duration unusedFor) {
    Instant lastUsed = lastUsedAt;
    Instant unusedUntil = (lastUsed == null ? createdAt : lastUsed).plus(unusedFor);
    return unusedUntil.isBefore(expiresAt) ? unusedUntil : expiresAt;
  }

  ApiTokens.TokenView toView(@Nullable String ownerName, Duration unusedFor) {
    Set<TokenScope> scopeSet = EnumSet.noneOf(TokenScope.class);
    Arrays.stream(scopes)
        .map(TokenScope::fromWireName)
        .flatMap(Optional::stream)
        .forEach(scopeSet::add);
    return new ApiTokens.TokenView(
        id,
        name,
        accountId,
        ownerName,
        scopeSet,
        lastFour,
        createdAt,
        stopsWorkingAt(unusedFor),
        lastUsedAt,
        lastUsedFrom,
        revokedAt);
  }
}
