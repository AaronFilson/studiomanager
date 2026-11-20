package io.github.aaronfilson.studiomanager.glaze;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** The glaze module's PostgreSQL-specific queries (advisory lock and cross-tenant photo sweep). */
@Component
class GlazeQueries {

  private final JdbcClient jdbc;

  GlazeQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Serializes concurrent photo uploads on one recipe until the transaction ends. */
  void lockRecipe(UUID recipeId) {
    jdbc.sql("select pg_advisory_xact_lock(hashtextextended(:id, 0))")
        .param("id", recipeId.toString())
        .query((rs, row) -> 0)
        .single();
  }

  /** Cross-tenant sweep (ADR 10 & 16): removes abandoned glaze photo uploads across all studios. */
  List<AbandonedPhoto> takeAbandonedPhotoUploads(Instant before) {
    return jdbc.sql(
            "select studio_id, recipe_id, photo_id from app.take_abandoned_glaze_photo_uploads(:before)")
        .param("before", before.atOffset(ZoneOffset.UTC))
        .query((rs, row) ->
            new AbandonedPhoto(
                rs.getObject("studio_id", UUID.class),
                rs.getObject("recipe_id", UUID.class),
                rs.getObject("photo_id", UUID.class)))
        .list();
  }

  record AbandonedPhoto(UUID studioId, UUID recipeId, UUID photoId) {}
}
