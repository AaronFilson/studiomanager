package io.github.aaronfilson.studiomanager.access;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** The access module's SQL, each statement with its reason (ADR 21). */
@Component
class AccessQueries {

  private final JdbcClient jdbc;

  AccessQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Grants a certification, or renews the grant already there. Upsert: race-free in one statement,
   * so two staff granting the same thing at once both succeed.
   */
  void grant(
      UUID studioId,
      UUID typeId,
      UUID accountId,
      UUID grantedBy,
      Instant grantedAt,
      @Nullable Instant expiresAt) {
    jdbc.sql(
            """
            insert into certification (studio_id, type_id, account_id, granted_by, granted_at, expires_at)
            values (:studioId, :typeId, :accountId, :by, :now, :expires)
            on conflict (type_id, account_id) do update
                set granted_by = excluded.granted_by, granted_at = excluded.granted_at, expires_at = excluded.expires_at
            """)
        .param("studioId", studioId)
        .param("typeId", typeId)
        .param("accountId", accountId)
        .param("by", grantedBy)
        .param("now", grantedAt.atOffset(ZoneOffset.UTC))
        .param("expires", expiresAt == null ? null : expiresAt.atOffset(ZoneOffset.UTC))
        .update();
  }
}
