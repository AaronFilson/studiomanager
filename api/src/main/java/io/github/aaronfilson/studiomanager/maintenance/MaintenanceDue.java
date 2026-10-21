package io.github.aaronfilson.studiomanager.maintenance;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A task has come due, or soon will, for the first time since it was last done. Published once
 * per cycle by the reminder sweep, inside the studio's transaction; the notify module turns it
 * into a notice (and so an email) to each recipient.
 *
 * @param recipients the named assignee; else whoever holds the duty; else the studio's staff
 * @param dueAt when the days interval runs out; null without one
 * @param firingsLeft firings until the firings interval runs out; null without one
 */
public record MaintenanceDue(UUID studioId, UUID taskId, String assetName, String taskTitle, DueStatus status,
        @Nullable Instant dueAt, @Nullable Integer firingsLeft, List<String> checklist, List<UUID> recipients) {
}
