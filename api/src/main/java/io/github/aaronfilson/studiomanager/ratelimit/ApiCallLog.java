package io.github.aaronfilson.studiomanager.ratelimit;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Channel;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Every MCP tool call, and every REST call made with a personal API token: who, through which
 * token, what, how it went and how long it took. The admin usage page and the tuning of the limits
 * read it.
 *
 * <p>An append-only hot path, so each call is one plain insert in its own short transaction, in the
 * caller's studio. A failure to log is logged and never fails the call.
 */
@Component
public class ApiCallLog {

  private static final Logger log = LoggerFactory.getLogger(ApiCallLog.class);

  /** The most of a call's arguments kept, in characters. */
  static final int MAX_ARGUMENTS = 2048;

  /** How a logged call went. */
  public enum Outcome {
    OK,
    ERROR,
    LIMITED
  }

  /**
   * One call to log.
   *
   * @param target the tool's name, or the HTTP method and route pattern
   * @param arguments the tool's arguments as JSON, cut to 2 KB; null for REST calls
   * @param status the HTTP status, for REST calls
   */
  public record Call(
      UUID studioId,
      UUID accountId,
      Channel channel,
      String target,
      @Nullable String arguments,
      Outcome outcome,
      @Nullable Integer status,
      Duration duration) {}

  private final JdbcClient jdbc;
  private final TransactionTemplate transactions;
  private final Clock clock;

  ApiCallLog(JdbcClient jdbc, TransactionTemplate transactions, Clock clock) {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.clock = clock;
  }

  /**
   * The personal API token the current request signed in with, if it did: the same token the
   * histories name as "via".
   */
  public static Optional<UUID> currentTokenId() {
    return TenantContext.current().map(TenantContext::viaTokenId);
  }

  /** Logs {@code call}, made by the current request. */
  public void record(Call call) {
    UUID tokenId = currentTokenId().orElse(null);
    try {
      transactions.executeWithoutResult(tx ->
          jdbc.sql(
                  """
                  insert into api_call_log (id, studio_id, account_id, token_id, channel, target, arguments,
                                            outcome, status, duration_ms, created_at)
                  values (:id, :studioId, :accountId, :tokenId, :channel, :target, :arguments, :outcome,
                          :status, :durationMs, :createdAt)
                  """)
              .param("id", Uuids.v7())
              .param("studioId", call.studioId())
              .param("accountId", call.accountId())
              .param("tokenId", tokenId)
              .param("channel", call.channel().name())
              .param("target", cut(call.target(), 200))
              .param(
                  "arguments",
                  call.arguments() == null ? null : cut(call.arguments(), MAX_ARGUMENTS))
              .param("outcome", call.outcome().name())
              .param("status", call.status())
              .param("durationMs", (int) Math.min(Integer.MAX_VALUE, call.duration().toMillis()))
              .param("createdAt", clock.instant().atOffset(ZoneOffset.UTC))
              .update());
    } catch (DataAccessException failure) {
      // Not the target: on MCP it's the tool name the caller sent, which mustn't reach the log
      log.warn("Couldn't record a {} call in the call log", call.channel(), failure);
    }
  }

  private static String cut(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max);
  }
}
