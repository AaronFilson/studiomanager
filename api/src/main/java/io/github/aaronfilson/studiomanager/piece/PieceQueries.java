package io.github.aaronfilson.studiomanager.piece;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The piece module's SQL, each statement for one of ADR 21's reasons. Everything else is Hibernate.
 *
 * <p>In a write transaction, flush before calling these: {@code JdbcClient} can't see changes
 * Hibernate hasn't written yet.
 */
@Component
class PieceQueries {

  private final JdbcClient jdbc;

  PieceQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Counters and upserts: the studio's next piece number, race-free in one statement. */
  int nextNumber(UUID studioId) {
    return jdbc.sql(
            """
            insert into piece_counter (studio_id, last_number) values (:studioId, 1)
            on conflict (studio_id) do update set last_number = piece_counter.last_number + 1
            returning last_number
            """)
        .param("studioId", studioId)
        .query(Integer.class)
        .single();
  }

  /** Counters and upserts: the studio's hold days, read from the row the upsert below keeps. */
  Optional<Integer> holdDays() {
    return jdbc.sql("select hold_days from piece_policy").query(Integer.class).optional();
  }

  /** Counters and upserts: sets the studio's hold days, race-free in one statement. */
  void saveHoldDays(UUID studioId, int days, Instant now) {
    jdbc.sql(
            """
            insert into piece_policy (studio_id, hold_days, updated_at) values (:studioId, :days, :now)
            on conflict (studio_id) do update set hold_days = excluded.hold_days, updated_at = excluded.updated_at
            """)
        .param("studioId", studioId)
        .param("days", days)
        .param("now", utc(now))
        .update();
  }

  /**
   * Set-based moves: moves every one of {@code pieceIds} still in {@code from} to {@code to}, in
   * one statement, and returns how many moved. Reaching READY starts each piece's hold.
   */
  int advance(
      Collection<UUID> pieceIds,
      PieceStage from,
      PieceStage to,
      Instant now,
      @Nullable Instant holdUntil) {
    return jdbc.sql(
            """
            update piece
            set stage = :to,
                ready_at = case when :to = 'READY' then :now else ready_at end,
                hold_until = case when :to = 'READY' then :holdUntil else hold_until end,
                version = version + 1
            where id = any(:ids) and stage = :from
            """)
        .param("to", to.name())
        .param("from", from.name())
        .param("now", utc(now))
        .param("holdUntil", holdUntil == null ? null : utc(holdUntil))
        .param("ids", pieceIds.toArray(UUID[]::new))
        .update();
  }

  /**
   * Postgres-only features: the pieces in {@code stage}, longest-waiting first, each with when it
   * reached that stage (a {@code lateral} join on the timeline).
   */
  List<Waiting> waitingIn(PieceStage stage, int fetchSize) {
    return jdbc.sql(
            """
            select p.id, e.since
            from piece p
            join lateral (select max(occurred_at) as since from piece_event
                          where piece_id = p.id and to_stage = p.stage) e on true
            where p.stage = :stage
            order by e.since, p.number
            limit :fetchSize
            """)
        .param("stage", stage.name())
        .param("fetchSize", fetchSize)
        .query((rs, row) ->
            new Waiting(
                rs.getObject("id", UUID.class),
                rs.getObject("since", OffsetDateTime.class).toInstant()))
        .list();
  }

  /** A piece waiting in a stage, and since when. */
  record Waiting(UUID pieceId, Instant since) {}

  /** Postgres-only features: each piece's first finished photo, for lists ({@code distinct on}). */
  List<FirstPhoto> firstReadyPhotos(Collection<UUID> pieceIds) {
    return jdbc.sql(
            """
            select distinct on (piece_id) id, piece_id, uploaded_by, ready_at
            from piece_photo
            where piece_id = any(:ids) and status = 'READY'
            order by piece_id, created_at, id
            """)
        .param("ids", pieceIds.toArray(UUID[]::new))
        .query((rs, row) ->
            new FirstPhoto(
                rs.getObject("id", UUID.class),
                rs.getObject("piece_id", UUID.class),
                rs.getObject("uploaded_by", UUID.class),
                rs.getObject("ready_at", OffsetDateTime.class).toInstant()))
        .list();
  }

  /** A piece's first finished photo. */
  record FirstPhoto(UUID photoId, UUID pieceId, UUID uploadedBy, Instant readyAt) {}

  /**
   * Cross-tenant functions: marks every ready piece whose hold ended by {@code now} as unclaimed,
   * in every studio ({@code app.mark_unclaimed_pieces}, ADR 10). Returns how many.
   */
  int markUnclaimed(Instant now) {
    return jdbc.sql("select app.mark_unclaimed_pieces(:now)")
        .param("now", utc(now))
        .query(Integer.class)
        .single();
  }

  /**
   * Cross-tenant functions: removes photo uploads started before {@code before} and never finished,
   * in every studio, and returns them ({@code app.take_abandoned_photo_uploads}).
   */
  List<AbandonedPhoto> takeAbandonedPhotoUploads(Instant before) {
    return jdbc.sql(
            "select studio_id, piece_id, photo_id from app.take_abandoned_photo_uploads(:before)")
        .param("before", utc(before))
        .query((rs, row) ->
            new AbandonedPhoto(
                rs.getObject("studio_id", UUID.class),
                rs.getObject("piece_id", UUID.class),
                rs.getObject("photo_id", UUID.class)))
        .list();
  }

  /** A photo upload that was started and never finished. */
  record AbandonedPhoto(UUID studioId, UUID pieceId, UUID photoId) {}

  private static OffsetDateTime utc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
