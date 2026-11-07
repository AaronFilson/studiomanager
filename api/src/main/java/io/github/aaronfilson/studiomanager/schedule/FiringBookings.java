package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.kiln.FiringEnded;
import io.github.aaronfilson.studiomanager.kiln.FiringKind;
import io.github.aaronfilson.studiomanager.kiln.FiringStarted;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceService;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Puts firings on the calendar by themselves. When a kiln starts, every calendar resource that is
 * that kiln (linked to its asset) gets a FIRING booking; when it's unloaded the booking ends then,
 * and a cancelled load's booking goes. Staff no longer type firings in twice, and members see when
 * a kiln is busy.
 *
 * <p>A firing is a fact, not a request, so it's booked even over other bookings: the calendar then
 * shows the clash for staff to sort out.
 *
 * <p>These run in the kiln's transaction, alongside other listeners that use SQL, so each change is
 * flushed before it returns (ADR 21).
 */
@Component
class FiringBookings {

  /** Long enough for a glaze firing and cooling; trimmed to the real unload time. */
  static final Duration EXPECTED_LENGTH = Duration.ofHours(24);

  private final BookableResourceRepository resources;
  private final CalendarBookingRepository bookings;
  private final MaintenanceService maintenance;

  FiringBookings(
      BookableResourceRepository resources,
      CalendarBookingRepository bookings,
      MaintenanceService maintenance) {
    this.resources = resources;
    this.bookings = bookings;
    this.maintenance = maintenance;
  }

  @EventListener
  void onStarted(FiringStarted firing) {
    UUID asset = maintenance.assetOfKiln(firing.kilnId()).orElse(null);
    if (asset == null) {
      return;
    }
    String title = "%s firing".formatted(describe(firing.kind()));
    List<CalendarBooking> booked =
        resources.findByAsset(asset).stream()
            .map(kiln ->
                CalendarBooking.ofFiring(
                    kiln, firing.firingId(), title, firing.startedAt(), EXPECTED_LENGTH))
            .toList();
    bookings.saveAllAndFlush(booked);
  }

  @EventListener
  void onEnded(FiringEnded firing) {
    List<CalendarBooking> booked = bookings.findByFiringId(firing.firingId());
    if (firing.completed()) {
      booked.forEach(booking -> booking.endFiringAt(firing.endedAt()));
    } else {
      bookings.deleteAll(booked);
    }
    bookings.flush();
  }

  private static String describe(FiringKind kind) {
    return switch (kind) {
      case BISQUE -> "Bisque";
      case GLAZE -> "Glaze";
    };
  }
}
