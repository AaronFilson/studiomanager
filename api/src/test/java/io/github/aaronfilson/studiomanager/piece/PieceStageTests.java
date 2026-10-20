package io.github.aaronfilson.studiomanager.piece;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class PieceStageTests {

    @Test
    void followsTheFiringPath() {
        assertThat(PieceStage.RECEIVED.canMoveTo(PieceStage.BISQUE_FIRED)).isTrue();
        assertThat(PieceStage.BISQUE_FIRED.canMoveTo(PieceStage.GLAZED)).isTrue();
        assertThat(PieceStage.GLAZED.canMoveTo(PieceStage.READY)).isTrue();
        assertThat(PieceStage.READY.canMoveTo(PieceStage.PICKED_UP)).isTrue();
    }

    @Test
    void allowsSingleFiringAndBisqueOnlyWork() {
        assertThat(PieceStage.RECEIVED.canMoveTo(PieceStage.GLAZED)).isTrue();
        assertThat(PieceStage.BISQUE_FIRED.canMoveTo(PieceStage.READY)).isTrue();
    }

    @Test
    void refusesImpossibleJumps() {
        assertThat(PieceStage.RECEIVED.canMoveTo(PieceStage.PICKED_UP)).isFalse();
        assertThat(PieceStage.READY.canMoveTo(PieceStage.DISPOSED)).isFalse();
        assertThat(PieceStage.PICKED_UP.next()).isEmpty();
    }

    @Test
    void unclaimedWorkCanStillBeCollectedOrDisposedOf() {
        assertThat(PieceStage.UNCLAIMED.next()).containsExactlyInAnyOrder(PieceStage.PICKED_UP, PieceStage.DISPOSED);
        assertThat(PieceStage.DISPOSED.isFinal()).isTrue();
    }

    @Test
    void holdsThroughTheEndOfTheStudiosLocalDay() {
        ZoneId chicago = ZoneId.of("America/Chicago");
        // Ready at 22:30 local on 1 March; a 7-day hold runs to midnight local at the end of 8 March
        Instant readyAt = Instant.parse("2026-03-02T04:30:00Z");

        assertThat(PieceService.holdEnd(readyAt, 7, chicago)).isEqualTo(Instant.parse("2026-03-09T05:00:00Z"));
    }

    @Test
    void holdEndCrossesDaylightSavingCorrectly() {
        ZoneId chicago = ZoneId.of("America/Chicago");
        // Clocks go forward on 8 March 2026, so local midnight on 9 March is 05:00Z, not 06:00Z
        Instant readyAt = Instant.parse("2026-03-07T18:00:00Z");

        assertThat(PieceService.holdEnd(readyAt, 1, chicago)).isEqualTo(Instant.parse("2026-03-09T05:00:00Z"));
    }

}
