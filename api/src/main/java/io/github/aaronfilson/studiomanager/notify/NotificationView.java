package io.github.aaronfilson.studiomanager.notify;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** A message to one member in one studio. */
public record NotificationView(UUID id, Kind kind, String title, String body, Instant createdAt,
        @Nullable Instant readAt) {

    public enum Kind {
        PIECES_READY,
        STUDIO_NOTICE,
        MAINTENANCE_DUE,
        PROBLEM_REPORTED
    }

}
