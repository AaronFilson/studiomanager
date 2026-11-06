package io.github.aaronfilson.studiomanager.ratelimit;

import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Keeps the call log to its last 90 days, across studios, as its own system account. */
@Component
class ApiCallLogPurge {

  private static final Logger log = LoggerFactory.getLogger(ApiCallLogPurge.class);
  static final Duration KEEP = Duration.ofDays(90);

  private final JdbcClient jdbc;
  private final TransactionTemplate transactions;
  private final Clock clock;

  ApiCallLogPurge(JdbcClient jdbc, TransactionTemplate transactions, Clock clock) {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.clock = clock;
  }

  @Scheduled(initialDelayString = "PT20M", fixedDelayString = "PT24H")
  void run() {
    int purged = purgeBefore(clock.instant().minus(KEEP));
    if (purged > 0) {
      log.info("Purged {} call log rows older than {} days", purged, KEEP.toDays());
    }
  }

  /** Deletes the rows logged before {@code before}, and returns how many. */
  int purgeBefore(Instant before) {
    Integer purged =
        TenantContext.callAs(
            TenantContext.system(SystemActor.CALL_LOG_PURGE),
            () ->
                transactions.execute(tx ->
                    jdbc.sql("select app.purge_api_call_log(:before)")
                        .param("before", before.atOffset(ZoneOffset.UTC))
                        .query(Integer.class)
                        .single()));
    return purged == null ? 0 : purged;
  }
}
