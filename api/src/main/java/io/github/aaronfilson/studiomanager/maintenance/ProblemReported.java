package io.github.aaronfilson.studiomanager.maintenance;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A member reported a problem. Published in the reporting transaction; the notify module turns it
 * into a notice (and so an email) to each recipient.
 *
 * @param assetName null for "somewhere else"
 * @param recipients the staff and technicians, leaving out whoever reported it
 */
public record ProblemReported(UUID studioId, UUID reportId, @Nullable String assetName, String description,
        @Nullable String reportedByName, List<UUID> recipients) {
}
