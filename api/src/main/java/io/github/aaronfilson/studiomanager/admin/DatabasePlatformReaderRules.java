package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.invoice.PlatformReaderRules;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The invoice reader rules in force: the configured defaults, or what the admin saved. Kept and
 * refreshed like the rate limits (ADR 24): read again on the first call after the copy is {@link
 * #REFRESH} old, and at once after this replica saves a change.
 */
@Component
@EnableConfigurationProperties(InvoiceReaderDefaults.class)
class DatabasePlatformReaderRules implements PlatformReaderRules {

  static final Duration REFRESH = Duration.ofSeconds(15);

  private static final Logger log = LoggerFactory.getLogger(DatabasePlatformReaderRules.class);

  /** What was read, and when. {@code saved} is false while the defaults are in force. */
  record Snapshot(Rules rules, boolean saved, Instant readAt) {}

  private final PlatformSettings settings;
  private final InvoiceReaderDefaults defaults;
  private final JsonMapper json;
  private final Clock clock;
  private final ReentrantLock reading = new ReentrantLock();
  private volatile @Nullable Snapshot snapshot;

  DatabasePlatformReaderRules(
      PlatformSettings settings, InvoiceReaderDefaults defaults, JsonMapper json, Clock clock) {
    this.settings = settings;
    this.defaults = defaults;
    this.json = json;
    this.clock = clock;
  }

  @Override
  public Rules current() {
    return fresh().rules();
  }

  Snapshot fresh() {
    Instant now = clock.instant();
    Snapshot held = snapshot;
    if (held != null && now.isBefore(held.readAt().plus(REFRESH))) {
      return held;
    }
    reading.lock();
    try {
      held = snapshot;
      if (held != null && now.isBefore(held.readAt().plus(REFRESH))) {
        return held;
      }
      Snapshot read = read(now);
      snapshot = read;
      return read;
    } finally {
      reading.unlock();
    }
  }

  /** Reads the rules again now: after this replica saved a change. */
  Snapshot reread() {
    reading.lock();
    try {
      Snapshot read = read(clock.instant());
      snapshot = read;
      return read;
    } finally {
      reading.unlock();
    }
  }

  InvoiceReaderDefaults defaults() {
    return defaults;
  }

  String write(Rules rules) {
    return json.writeValueAsString(InvoiceReaderValues.StoredRules.of(rules));
  }

  private Snapshot read(Instant now) {
    PlatformSettings.Setting row = settings.readAll().get(InvoiceReaderValues.KEY);
    String stored = row == null ? null : row.json();
    if (row == null || stored == null) {
      return new Snapshot(defaults.rules(), false, now);
    }
    try {
      return new Snapshot(
          json.readValue(stored, InvoiceReaderValues.StoredRules.class).rules(), true, now);
    } catch (JacksonException | IllegalArgumentException unreadable) {
      log.warn("Ignoring the setting {} (version {})", row.key(), row.version(), unreadable);
      return new Snapshot(defaults.rules(), false, now);
    }
  }
}
