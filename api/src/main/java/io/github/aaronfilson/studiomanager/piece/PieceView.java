package io.github.aaronfilson.studiomanager.piece;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.firing.PieceSize;
import io.github.aaronfilson.studiomanager.platform.Money;

import org.jspecify.annotations.Nullable;

/**
 * A piece as the API shows it. Lists carry only its first photo and an empty timeline; a single
 * piece has every photo and its timeline.
 */
public record PieceView(UUID id, int number, UUID makerAccountId, @Nullable String makerName,
        @Nullable String description, @Nullable String clayBody, @Nullable String makerMark, PieceSize size,
        PieceStage stage, Money fee, @Nullable Integer feeScheduleVersion, @Nullable String feeTaxCategory,
        String feeBreakdown, Instant receivedAt, @Nullable Instant readyAt, @Nullable Instant holdUntil,
        long version, List<Photo> photos, List<Event> timeline) {

    /** Ready, and past the date the studio holds it until. The hourly sweep will mark it unclaimed. */
    public boolean overdue(Instant now) {
        return stage == PieceStage.READY && holdUntil != null && !holdUntil.isAfter(now);
    }

    /** A finished photo, with signed URLs that read it and its thumbnail. */
    public record Photo(UUID id, URI url, URI thumbnailUrl, UUID uploadedBy, Instant uploadedAt) {
    }

    public record Event(@Nullable PieceStage from, PieceStage to, @Nullable String note,
            @Nullable UUID actorAccountId, @Nullable String actorName, Instant occurredAt) {
    }

}
