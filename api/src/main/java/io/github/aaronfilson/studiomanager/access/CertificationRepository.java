package io.github.aaronfilson.studiomanager.access;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * The bound studio's certifications and their types; Hibernate adds the studio to every query
 * ({@code @TenantId}).
 */
interface CertificationRepository
    extends JpaRepository<CertificationEntity, CertificationEntity.Key> {

  @Query(
      """
      select new io.github.aaronfilson.studiomanager.access.CertificationRow(
          c.key.typeId, t.name, c.key.accountId, c.grantedAt, c.expiresAt)
      from CertificationEntity c join CertificationTypeEntity t on t.id = c.key.typeId
      where c.key.accountId = :accountId
      order by t.name
      """)
  List<CertificationRow> findRowsFor(UUID accountId);

  @Query(
      """
      select new io.github.aaronfilson.studiomanager.access.CertificationRow(
          c.key.typeId, t.name, c.key.accountId, c.grantedAt, c.expiresAt)
      from CertificationEntity c join CertificationTypeEntity t on t.id = c.key.typeId
      order by t.name
      """)
  List<CertificationRow> findAllRows();

  @Query(
      """
      select count(c) > 0 from CertificationEntity c
      where c.key.accountId = :accountId and c.key.typeId = :typeId
        and (c.expiresAt is null or c.expiresAt > :at)
      """)
  boolean isCertified(UUID accountId, UUID typeId, Instant at);
}
