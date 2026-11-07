package io.github.aaronfilson.studiomanager.maintenance;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The maintenance module's SQL, each statement for one of the reasons in ADR 21. Everything else
 * goes through the entities.
 *
 * <p>Callers in a write transaction flush Hibernate first, since {@code JdbcClient} can't see
 * changes it hasn't written yet.
 */
@Component
class MaintenanceQueries {

  private final JdbcClient jdbc;

  MaintenanceQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Cross-tenant function (ADR 10): the studios with a task to remind about. Only a job may call
   * it, with no studio bound.
   */
  List<UUID> studiosWithMaintenanceToRemind() {
    return jdbc.sql("select studio_id from app.studios_with_maintenance_to_remind()")
        .query((rs, rowNumber) -> rs.getObject("studio_id", UUID.class))
        .list();
  }

  /**
   * Claim: takes a task's reminder for the cycle that began at {@code cycle}, in one atomic
   * test-and-set. Two API replicas can sweep at once; the row lock makes the second wait, then find
   * the cycle already claimed. Only this cycle: if the task was done meanwhile, its new cycle still
   * gets its own reminder.
   *
   * @return whether this call claimed it
   */
  boolean claimReminder(UUID taskId, Instant cycle) {
    return jdbc.sql(
                """
                update maintenance_task set reminded_for = last_done_at
                where id = :id and last_done_at = :cycle and reminded_for is distinct from last_done_at
                """)
            .param("id", taskId)
            .param("cycle", utc(cycle))
            .update()
        == 1;
  }

  /**
   * Report (the export): every record done in [{@code from}, {@code to}), oldest first, with its
   * asset's own name and its task's title, archived ones included, in one round trip.
   */
  List<ExportedRecord> recordsDoneBetween(Instant from, Instant to) {
    return jdbc.sql(
            """
            select r.asset_id, a.name, t.title, r.note, r.cost, r.checked, r.at_firing, r.done_by,
                   v.name as via_token_name, r.done_at
            from maintenance_record r
            join asset a on a.id = r.asset_id
            left join maintenance_task t on t.id = r.task_id
            left join api_token v on v.id = r.via_token_id
            where r.done_at >= :from and r.done_at < :to
            order by r.done_at, r.id
            """)
        .param("from", utc(from))
        .param("to", utc(to))
        .query((rs, rowNumber) ->
            new ExportedRecord(
                rs.getObject("asset_id", UUID.class),
                rs.getString("name"),
                rs.getString("title"),
                rs.getString("note"),
                rs.getBigDecimal("cost"),
                strings(rs.getArray("checked")),
                rs.getObject("at_firing", Integer.class),
                rs.getObject("done_by", UUID.class),
                rs.getString("via_token_name"),
                rs.getObject("done_at", OffsetDateTime.class).toInstant()))
        .list();
  }

  /**
   * @param ownName the asset's own name; null for a kiln, whose name lives on the kiln
   * @param taskTitle null for a repair
   * @param viaTokenName the personal API token the work was logged through, if any
   */
  record ExportedRecord(
      UUID assetId,
      @Nullable String ownName,
      @Nullable String taskTitle,
      @Nullable String note,
      @Nullable BigDecimal cost,
      List<String> checked,
      @Nullable Integer atFiring,
      UUID doneBy,
      @Nullable String viaTokenName,
      Instant doneAt) {}

  /**
   * Postgres-only feature: serializes photo changes on one record until the transaction ends.
   * Records are insert-only, so the app can't take a row lock on one (FOR UPDATE needs UPDATE); a
   * transaction-scoped advisory lock on its id does the same job.
   */
  void lockRecord(UUID recordId) {
    jdbc.sql("select pg_advisory_xact_lock(hashtextextended(:id, 0))")
        .param("id", recordId.toString())
        .query((rs, rowNumber) -> 0)
        .single();
  }

  /**
   * Cross-tenant function (ADR 10): removes record photo uploads started before {@code before} and
   * never finished, in every studio, and returns them so their images can be deleted.
   */
  List<AbandonedPhoto> takeAbandonedPhotoUploads(Instant before) {
    return jdbc.sql(
            "select studio_id, record_id, photo_id from app.take_abandoned_record_photo_uploads(:before)")
        .param("before", utc(before))
        .query((rs, rowNumber) ->
            new AbandonedPhoto(
                rs.getObject("studio_id", UUID.class),
                rs.getObject("record_id", UUID.class),
                rs.getObject("photo_id", UUID.class)))
        .list();
  }

  record AbandonedPhoto(UUID studioId, UUID recordId, UUID photoId) {}

  private static List<String> strings(@Nullable Array array) throws SQLException {
    return array == null ? List.of() : Arrays.asList((String[]) array.getArray());
  }

  private static OffsetDateTime utc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
