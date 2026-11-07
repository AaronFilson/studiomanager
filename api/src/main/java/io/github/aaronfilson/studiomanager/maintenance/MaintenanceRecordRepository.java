package io.github.aaronfilson.studiomanager.maintenance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The record history: written by {@link MaintenanceRecords}, read here. */
interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {

  /** An asset's records, newest first, each with its task, so a list reads no task per row. */
  @Query(
      """
      select r from MaintenanceRecord r left join fetch r.task
      where r.asset.id = :assetId
      order by r.doneAt desc, r.id desc
      """)
  List<MaintenanceRecord> findNewestForAsset(UUID assetId, Limit limit);

  /** The parts used in every record of an asset: one query for the whole history. */
  @Query("select p from RecordPart p where p.record.asset.id = :assetId")
  List<RecordPart> findPartsForAsset(UUID assetId);
}
