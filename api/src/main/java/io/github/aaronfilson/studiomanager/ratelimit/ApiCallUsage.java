package io.github.aaronfilson.studiomanager.ratelimit;

import io.github.aaronfilson.studiomanager.platform.tenancy.PlatformAdmin;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Channel;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-tenant API &amp; MCP usage overview for the platform admin page (docs/planning/20, section
 * 7). Reads {@code api_call_log} through SECURITY DEFINER functions that enforce {@code
 * app.is_platform_admin()} (V53).
 */
@Service
public class ApiCallUsage {

  private static final Duration HOUR = Duration.ofHours(1);
  private static final Duration DAY = Duration.ofHours(24);

  /**
   * Calls, rate-limit refusals and errors for one studio and channel over the last hour and day.
   */
  public record StudioUsage(
      UUID studioId,
      String studioName,
      Channel channel,
      long callsLastHour,
      long refusalsLastHour,
      long errorsLastHour,
      long callsLastDay,
      long refusalsLastDay,
      long errorsLastDay) {}

  /**
   * Calls, rate-limit refusals, errors and distinct active hours (in the last 24h) for one personal
   * API token.
   */
  public record TokenUsage(
      UUID tokenId,
      String tokenName,
      UUID studioId,
      String studioName,
      long callsLastHour,
      long refusalsLastHour,
      long errorsLastHour,
      long callsLastDay,
      long refusalsLastDay,
      long errorsLastDay,
      long activeHoursLastDay) {}

  /** Usage snapshot across all studios and personal API tokens. */
  public record Overview(Instant asOf, List<StudioUsage> studios, List<TokenUsage> tokens) {}

  private final JdbcClient jdbc;
  private final Clock clock;

  ApiCallUsage(JdbcClient jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /** Returns MCP &amp; token calls, refusals and active hours for the last hour and last 24h. */
  @Transactional(readOnly = true)
  public Overview overview() {
    if (!PlatformAdmin.isBound()) {
      throw new AccessDeniedException("Only a platform admin can see API usage across studios.");
    }
    Instant now = clock.instant();
    var sinceHour = now.minus(HOUR).atOffset(ZoneOffset.UTC);
    var sinceDay = now.minus(DAY).atOffset(ZoneOffset.UTC);
    List<StudioUsage> studios =
        jdbc.sql(
                """
                select studio_id, studio_name, channel,
                       calls_hour, refusals_hour, errors_hour,
                       calls_day, refusals_day, errors_day
                from app.admin_api_usage_by_studio(:sinceHour, :sinceDay)
                """)
            .param("sinceHour", sinceHour)
            .param("sinceDay", sinceDay)
            .query((rs, row) ->
                new StudioUsage(
                    rs.getObject("studio_id", UUID.class),
                    rs.getString("studio_name"),
                    Channel.valueOf(rs.getString("channel")),
                    rs.getLong("calls_hour"),
                    rs.getLong("refusals_hour"),
                    rs.getLong("errors_hour"),
                    rs.getLong("calls_day"),
                    rs.getLong("refusals_day"),
                    rs.getLong("errors_day")))
            .list();
    List<TokenUsage> tokens =
        jdbc.sql(
                """
                select token_id, token_name, studio_id, studio_name,
                       calls_hour, refusals_hour, errors_hour,
                       calls_day, refusals_day, errors_day, active_hours_day
                from app.admin_api_usage_by_token(:sinceHour, :sinceDay)
                """)
            .param("sinceHour", sinceHour)
            .param("sinceDay", sinceDay)
            .query((rs, row) ->
                new TokenUsage(
                    rs.getObject("token_id", UUID.class),
                    rs.getString("token_name"),
                    rs.getObject("studio_id", UUID.class),
                    rs.getString("studio_name"),
                    rs.getLong("calls_hour"),
                    rs.getLong("refusals_hour"),
                    rs.getLong("errors_hour"),
                    rs.getLong("calls_day"),
                    rs.getLong("refusals_day"),
                    rs.getLong("errors_day"),
                    rs.getLong("active_hours_day")))
            .list();
    return new Overview(now, studios, tokens);
  }
}
