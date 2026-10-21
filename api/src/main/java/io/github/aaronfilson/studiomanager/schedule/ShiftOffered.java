package io.github.aaronfilson.studiomanager.schedule;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Whoever has a shift has asked for cover. Published in the transaction that asked; the notify
 * module tells everyone who could take it.
 *
 * @param recipients members with the shift's duty, or everyone if it has none; not the holder
 */
public record ShiftOffered(UUID studioId, UUID shiftId, String title, Instant startsAt, Instant endsAt,
        @Nullable String holderName, List<UUID> recipients) {
}
