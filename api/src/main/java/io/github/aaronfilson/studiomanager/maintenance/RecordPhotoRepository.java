package io.github.aaronfilson.studiomanager.maintenance;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Photos on maintenance records. */
interface RecordPhotoRepository extends JpaRepository<RecordPhoto, UUID> {

  /** Photos that count against a record's limit: the ready ones, and uploads still in time. */
  @Query(
      """
      select count(p) from RecordPhoto p
      where p.recordId = :recordId
        and (p.status = io.github.aaronfilson.studiomanager.maintenance.RecordPhoto.Status.READY
             or p.createdAt > :windowStart)
      """)
  long countTaken(UUID recordId, Instant windowStart);

  @Query(
      """
      select count(p) from RecordPhoto p
      where p.recordId = :recordId
        and p.status = io.github.aaronfilson.studiomanager.maintenance.RecordPhoto.Status.READY
      """)
  long countReady(UUID recordId);

  Optional<RecordPhoto> findByIdAndRecordId(UUID photoId, UUID recordId);

  /** The ready photos of every record of an asset, oldest first: one query for the history. */
  @Query(
      """
      select p from RecordPhoto p join MaintenanceRecord r on r.id = p.recordId
      where r.asset.id = :assetId
        and p.status = io.github.aaronfilson.studiomanager.maintenance.RecordPhoto.Status.READY
      order by p.createdAt
      """)
  List<RecordPhoto> findReadyForAsset(UUID assetId);
}
