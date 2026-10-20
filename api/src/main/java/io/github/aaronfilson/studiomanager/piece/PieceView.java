package io.github.aaronfilson.studiomanager.piece;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.firing.PieceSize;
import io.github.aaronfilson.studiomanager.platform.Money;

import org.jspecify.annotations.Nullable;

/** A piece as the API shows it. The timeline is empty in lists and filled for a single piece. */
public record PieceView(UUID id, int number, UUID makerAccountId, @Nullable String makerName,
        @Nullable String description, @Nullable String clayBody, @Nullable String makerMark, PieceSize size,
        PieceStage stage, Money fee, @Nullable Integer feeScheduleVersion, @Nullable String feeTaxCategory,
        String feeBreakdown, Instant receivedAt, @Nullable Instant readyAt, @Nullable Instant holdUntil,
        long version, List<Event> timeline) {

    /** Ready, and past the date the studio holds it until. The hourly sweep will mark it unclaimed. */
    public boolean overdue(Instant now) {
        return stage == PieceStage.READY && holdUntil != null && !holdUntil.isAfter(now);
    }

    public record Event(@Nullable PieceStage from, PieceStage to, @Nullable String note,
            @Nullable UUID actorAccountId, @Nullable String actorName, Instant occurredAt) {
    }

}
