package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's payouts; Hibernate adds the studio to every query ({@code @TenantId}). */
interface PayoutRepository extends JpaRepository<PayoutEntity, UUID> {

  @Query("select p from PayoutEntity p order by p.arrivalOn desc, p.importedAt desc")
  List<PayoutEntity> findNewestFirst(Limit limit);

  @Query(
      "select p from PayoutEntity p where p.provider = :provider and p.externalId = :externalId")
  Optional<PayoutEntity> findImported(Provider provider, String externalId);
}
