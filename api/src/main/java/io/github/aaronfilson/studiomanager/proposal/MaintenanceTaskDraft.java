package io.github.aaronfilson.studiomanager.proposal;

import jakarta.persistence.Embeddable;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A proposed recurring maintenance task on one asset, due every so many days, firings, or both.
 * Approving it adds the task through {@code MaintenanceService.createTask}, unassigned and with no
 * checklist, which staff can add afterwards.
 */
@Embeddable
public record MaintenanceTaskDraft(
    UUID assetId, String title, @Nullable Integer everyDays, @Nullable Integer everyFirings) {}
