package io.github.aaronfilson.studiomanager.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class DueRuleTests {

    private static final Instant DONE = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    void aYearlyCheckWarnsTwoWeeksAheadAndIsDueOnTheDay() {
        assertThat(DueRule.evaluate(365, null, DONE, 0, null, DONE.plus(Duration.ofDays(350))).status())
                .isEqualTo(DueStatus.OK);
        assertThat(DueRule.evaluate(365, null, DONE, 0, null, DONE.plus(Duration.ofDays(351))).status())
                .isEqualTo(DueStatus.DUE_SOON);
        DueRule.Due due = DueRule.evaluate(365, null, DONE, 0, null, DONE.plus(Duration.ofDays(365)));
        assertThat(due.status()).isEqualTo(DueStatus.DUE);
        assertThat(due.dueAt()).isEqualTo(DONE.plus(Duration.ofDays(365)));
        assertThat(due.firingsLeft()).isNull();
    }

    @Test
    void aWeeklyCheckWarnsADayAhead() {
        assertThat(DueRule.evaluate(7, null, DONE, 0, null, DONE.plus(Duration.ofDays(5))).status())
                .isEqualTo(DueStatus.OK);
        assertThat(DueRule.evaluate(7, null, DONE, 0, null, DONE.plus(Duration.ofDays(6))).status())
                .isEqualTo(DueStatus.DUE_SOON);
    }

    @Test
    void firingsCountFromWhenTheTaskWasLastDone() {
        // Done at firing 40, every 10: due at 50, warned one firing ahead
        assertThat(DueRule.evaluate(null, 10, DONE, 40, 48, DONE).status()).isEqualTo(DueStatus.OK);
        DueRule.Due soon = DueRule.evaluate(null, 10, DONE, 40, 49, DONE);
        assertThat(soon.status()).isEqualTo(DueStatus.DUE_SOON);
        assertThat(soon.firingsLeft()).isEqualTo(1);
        DueRule.Due due = DueRule.evaluate(null, 10, DONE, 40, 52, DONE);
        assertThat(due.status()).isEqualTo(DueStatus.DUE);
        assertThat(due.firingsLeft()).isEqualTo(-2);
    }

    @Test
    void whicheverComesFirst() {
        // "Every 30 firings or yearly": a quiet kiln still gets its yearly check
        assertThat(DueRule.evaluate(365, 30, DONE, 0, 3, DONE.plus(Duration.ofDays(366))).status())
                .isEqualTo(DueStatus.DUE);
        // and a busy one gets it at 30 firings
        assertThat(DueRule.evaluate(365, 30, DONE, 0, 30, DONE.plus(Duration.ofDays(60))).status())
                .isEqualTo(DueStatus.DUE);
    }

}
