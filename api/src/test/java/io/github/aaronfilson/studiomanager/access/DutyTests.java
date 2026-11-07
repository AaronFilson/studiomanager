package io.github.aaronfilson.studiomanager.access;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import org.junit.jupiter.api.Test;

class DutyTests {

  @Test
  void everyDutyGrantsSomethingButNothingOwnerOnly() {
    for (Duty duty : Duty.values()) {
      assertThat(duty.grants()).as(duty.name()).isNotEmpty();
      assertThat(duty.grants()).as(duty.name()).doesNotContainAnyElementsOf(Permission.ownerOnly());
    }
  }

  @Test
  void theGrantsAreThePlansTable() {
    assertThat(Duty.TECHNICIAN.grants())
        .containsExactlyInAnyOrder(
            Permission.MAINTENANCE_COMPLETE_ASSIGNED, Permission.MAINTENANCE_REPORTS_RESOLVE);
    assertThat(Duty.MONITOR.grants())
        .containsExactlyInAnyOrder(
            Permission.SCHEDULE_SHIFTS_TAKE_DUTY, Permission.PIECES_INTAKE_FOR_OTHERS);
    assertThat(Duty.WORK_EXCHANGE.grants())
        .containsExactlyInAnyOrder(
            Permission.SCHEDULE_SHIFTS_TAKE_DUTY, Permission.SCHEDULE_HOURS_LOG_OWN);
    assertThat(Duty.INSTRUCTOR.grants()).containsExactly(Permission.SCHEDULE_SHIFTS_TAKE_DUTY);
  }
}
