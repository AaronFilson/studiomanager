package io.github.aaronfilson.studiomanager.ai;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** The AI key module's SQL, for one of ADR 21's reasons. Everything else is Hibernate. */
@Component
class AiKeyQueries {

  private final JdbcClient jdbc;

  AiKeyQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Counters and upserts: saves the studio's key, replacing any it had, untested, race-free in one
   * statement ({@code on conflict}).
   */
  void save(
      UUID studioId,
      AiKeys.Provider provider,
      byte[] ciphertext,
      String lastFour,
      boolean allowPlatformReaders,
      UUID setBy,
      Instant now) {
    jdbc.sql(
            """
            insert into ai_provider_key (studio_id, provider, key_ciphertext, last_four, status,
                                         allow_platform_readers, set_by, set_at)
            values (:studio, :provider, :secret, :lastFour, 'UNTESTED', :platformReaders, :by, :now)
            on conflict (studio_id) do update set provider = excluded.provider,
                key_ciphertext = excluded.key_ciphertext, last_four = excluded.last_four, status = 'UNTESTED',
                status_reason = null, checked_at = null, allow_platform_readers = excluded.allow_platform_readers,
                set_by = excluded.set_by, set_at = excluded.set_at
            """)
        .param("studio", studioId)
        .param("provider", provider.name())
        .param("secret", ciphertext)
        .param("lastFour", lastFour)
        .param("platformReaders", allowPlatformReaders)
        .param("by", setBy)
        .param("now", now.atOffset(ZoneOffset.UTC))
        .update();
  }
}
