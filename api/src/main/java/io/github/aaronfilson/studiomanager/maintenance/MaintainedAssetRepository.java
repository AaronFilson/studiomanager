package io.github.aaronfilson.studiomanager.maintenance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's assets; Hibernate adds the studio to every query ({@code @TenantId}). */
interface MaintainedAssetRepository extends JpaRepository<MaintainedAsset, UUID> {

  @Query("select a from MaintainedAsset a where not a.archived")
  List<MaintainedAsset> findTracked();

  @Query("select a from MaintainedAsset a where a.id = :assetId and not a.archived")
  Optional<MaintainedAsset> findTracked(UUID assetId);

  @Query("select a.id from MaintainedAsset a where a.kilnId = :kilnId")
  Optional<UUID> findIdOfKiln(UUID kilnId);
}
