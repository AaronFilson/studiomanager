package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Shift;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** A slot someone must cover, such as "Monitor, Tuesday 6–9pm". */
@Entity
@Table(name = "shift")
@EntityListeners(AuditingEntityListener.class)
class RosterShift {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private String title;

  @Enumerated(EnumType.STRING)
  private @Nullable Duty duty;

  private Instant startsAt;

  private Instant endsAt;

  private @Nullable UUID accountId;

  private @Nullable Instant workedAt;

  private @Nullable Instant offeredAt;

  @CreatedBy private @Nullable UUID createdBy;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected RosterShift() {}

  RosterShift(String title, @Nullable Duty duty, Instant startsAt, Instant endsAt) {
    this.id = Uuids.v7();
    this.title = title;
    this.duty = duty;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
  }

  long version() {
    return Objects.requireNonNull(version, "A shift has a version once it's saved");
  }

  /** Gives the shift to someone, or with null opens it up again. Any offer of cover ends. */
  void assign(@Nullable UUID newHolder) {
    this.accountId = newHolder;
    this.offeredAt = null;
  }

  /** Asks for cover from {@code at}, or with null stops asking. */
  void setOfferedAt(@Nullable Instant at) {
    this.offeredAt = at;
  }

  void markWorked(Instant at) {
    this.workedAt = at;
  }

  /**
   * @param accountName the holder's name, looked up by the caller
   */
  Shift toView(@Nullable String accountName) {
    return new Shift(
        id, title, duty, startsAt, endsAt, accountId, accountName, workedAt, offeredAt, version());
  }

  @Nullable UUID accountId() {
    return accountId;
  }
}
