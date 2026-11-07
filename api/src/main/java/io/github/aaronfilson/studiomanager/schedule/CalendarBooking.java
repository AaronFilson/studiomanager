package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.BookingKind;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A booking on the calendar. Bookings aren't edited, only made and cancelled (a firing's is trimmed
 * when the kiln is unloaded), so the table has no version; {@link Persistable} tells Spring Data a
 * new one apart from a loaded one, so saving it inserts rather than merges.
 *
 * <p>A firing belongs to the kiln module, so it is held by id, not mapped (ADR 21).
 */
@Entity
@Table(name = "booking")
@EntityListeners(AuditingEntityListener.class)
class CalendarBooking implements Persistable<UUID> {

  /** The shortest a firing's booking gets, so one unloaded at once still shows. */
  private static final Duration SHORTEST_FIRING = Duration.ofMinutes(1);

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "resource_id")
  private BookableResource resource;

  @Enumerated(EnumType.STRING)
  private BookingKind kind;

  private String title;

  private Instant startsAt;

  private Instant endsAt;

  /** Whose it is: the member who reserved, or who a class or rental is for. */
  private @Nullable UUID accountId;

  /** Set for a firing's booking, which the kiln module's events keep up to date. */
  private @Nullable UUID firingId;

  @CreatedBy private @Nullable UUID createdBy;

  @CreatedDate private @Nullable Instant createdAt;

  @Transient private boolean isNew = true;

  @SuppressWarnings("NullAway.Init")
  protected CalendarBooking() {}

  private CalendarBooking(
      BookableResource resource,
      BookingKind kind,
      String title,
      Instant startsAt,
      Instant endsAt,
      @Nullable UUID accountId,
      @Nullable UUID firingId) {
    this.id = Uuids.v7();
    this.resource = resource;
    this.kind = kind;
    this.title = title;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.accountId = accountId;
    this.firingId = firingId;
  }

  /** A booking someone made, already checked against the resource's rules. */
  static CalendarBooking made(
      BookableResource resource,
      BookingKind kind,
      String title,
      Instant startsAt,
      Instant endsAt,
      @Nullable UUID accountId) {
    return new CalendarBooking(resource, kind, title, startsAt, endsAt, accountId, null);
  }

  /** A kiln's firing, from when it started for {@code expectedLength}, trimmed when it ends. */
  static CalendarBooking ofFiring(
      BookableResource kiln,
      UUID firingId,
      String title,
      Instant startedAt,
      Duration expectedLength) {
    return new CalendarBooking(
        kiln, BookingKind.FIRING, title, startedAt, startedAt.plus(expectedLength), null, firingId);
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostLoad
  @PostPersist
  void markStored() {
    this.isNew = false;
  }

  BookingKind kind() {
    return kind;
  }

  @Nullable UUID accountId() {
    return accountId;
  }

  /** Ends a firing's booking when the kiln was unloaded, but never shorter than a minute. */
  void endFiringAt(Instant unloadedAt) {
    Instant shortest = startsAt.plus(SHORTEST_FIRING);
    this.endsAt = unloadedAt.isAfter(shortest) ? unloadedAt : shortest;
  }

  /**
   * The booking as the calendar shows it, before the caller decides what the viewer may see.
   *
   * @param accountName the name of whoever it's for, looked up by the caller
   */
  ScheduleModel.Booking toView(@Nullable String accountName, boolean mine) {
    return new ScheduleModel.Booking(
        id,
        resource.id(),
        resource.name(),
        kind,
        title,
        startsAt,
        endsAt,
        accountId,
        accountName,
        mine);
  }
}
