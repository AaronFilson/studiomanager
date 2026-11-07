package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.studio.StudioService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import org.springframework.stereotype.Component;

/** The bound studio's calendar and currency, which its books are kept in. */
@Component
class StudioBooks {

  private final StudioService studios;
  private final Clock clock;

  StudioBooks(StudioService studios, Clock clock) {
    this.studios = studios;
    this.clock = clock;
  }

  Currency currency() {
    return studios.current().currency();
  }

  /** ISO 3166-1 alpha-2, for rules that depend on where the studio is. */
  String country() {
    return studios.current().country();
  }

  /** Today in the studio's own time zone, which is what bookkeeping is reported by. */
  LocalDate today() {
    return dateOf(clock.instant());
  }

  /** The studio's local date at {@code instant}. */
  LocalDate dateOf(Instant instant) {
    return instant.atZone(studios.current().timeZone()).toLocalDate();
  }
}
