package io.github.aaronfilson.studiomanager.schedule;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Someone took a shift its holder had offered. The notify module tells the old holder. */
public record ShiftCovered(UUID studioId, UUID shiftId, String title, Instant startsAt, UUID previousHolder,
        @Nullable String coveredByName) {
}
