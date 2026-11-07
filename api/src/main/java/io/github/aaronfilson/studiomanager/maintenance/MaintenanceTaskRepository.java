package io.github.aaronfilson.studiomanager.maintenance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Tasks still tracked, on assets still tracked, always with their asset, since every view shows the
 * asset's name.
 */
interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, UUID> {

  @Query(
      """
      select t from MaintenanceTask t join fetch t.asset a
      where not t.archived and not a.archived
      """)
  List<MaintenanceTask> findTracked();

  @Query(
      """
      select t from MaintenanceTask t join fetch t.asset a
      where t.id = :taskId and not t.archived and not a.archived
      """)
  Optional<MaintenanceTask> findTracked(UUID taskId);

  @Query(
      "select lower(t.title) from MaintenanceTask t where t.asset.id = :assetId and not t.archived")
  List<String> findTitlesInLowerCase(UUID assetId);
}
