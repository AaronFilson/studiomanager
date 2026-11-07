package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.HoursEntry;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

/**
 * Minutes credited to someone, for a shift worked or as a correction. Insert-only, so the table has
 * no version; {@link Persistable} tells Spring Data a new one apart from a loaded one.
 */
@Entity
@Table(name = "hours_entry")
class HoursCredit implements Persistable<UUID> {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID accountId;

  /** The shift worked; null for a correction. */
  private @Nullable UUID shiftId;

  private int minutes;

  private @Nullable String note;

  private UUID creditedBy;

  private Instant creditedAt;

  @Transient private boolean isNew = true;

  @SuppressWarnings("NullAway.Init")
  protected HoursCredit() {}

  HoursCredit(
      UUID accountId,
      @Nullable UUID shiftId,
      int minutes,
      String note,
      UUID creditedBy,
      Instant creditedAt) {
    this.id = Uuids.v7();
    this.accountId = accountId;
    this.shiftId = shiftId;
    this.minutes = minutes;
    this.note = note;
    this.creditedBy = creditedBy;
    this.creditedAt = creditedAt;
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

  UUID creditedBy() {
    return creditedBy;
  }

  /**
   * @param creditedByName the name of whoever credited it, looked up by the caller
   */
  HoursEntry toView(@Nullable String creditedByName) {
    return new HoursEntry(id, minutes, note, shiftId, creditedAt, creditedByName);
  }
}
