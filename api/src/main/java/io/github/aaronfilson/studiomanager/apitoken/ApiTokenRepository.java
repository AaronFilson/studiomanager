package io.github.aaronfilson.studiomanager.apitoken;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's API tokens ({@code @TenantId}). */
interface ApiTokenRepository extends JpaRepository<ApiToken, UUID> {

  @Query("select t from ApiToken t order by t.createdAt desc, t.id desc")
  List<ApiToken> findAllNewestFirst();

  @Query(
      "select t from ApiToken t where t.accountId = :accountId order by t.createdAt desc, t.id desc")
  List<ApiToken> findOwnNewestFirst(UUID accountId);

  /**
   * How many of a person's tokens still work at {@code now}: not revoked, not past their expiry,
   * and used (or made) since {@code unusedCutoff}.
   */
  @Query(
      """
      select count(t) from ApiToken t
      where t.accountId = :accountId and t.revokedAt is null and t.expiresAt > :now
        and coalesce(t.lastUsedAt, t.createdAt) > :unusedCutoff
      """)
  long countLive(UUID accountId, Instant now, Instant unusedCutoff);

  /** Locks the token until the transaction ends, so two revokes announce it once. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from ApiToken t where t.id = :tokenId")
  Optional<ApiToken> findAndLock(UUID tokenId);
}
