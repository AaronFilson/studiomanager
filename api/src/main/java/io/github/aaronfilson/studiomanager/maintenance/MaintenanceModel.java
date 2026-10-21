package io.github.aaronfilson.studiomanager.maintenance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.access.Duty;

import org.jspecify.annotations.Nullable;

/** What the maintenance module hands to its controller and to Today. */
public final class MaintenanceModel {

    private MaintenanceModel() {
    }

    public enum AssetKind {
        KILN,
        WHEEL,
        PUG_MILL,
        SLAB_ROLLER,
        EXTRUDER,
        CLAY_TRAP,
        SPRAY_BOOTH,
        VENTILATION,
        BUILDING,
        SAFETY,
        OTHER
    }

    /**
     * @param name for a kiln, the kiln's name
     * @param downReason for a kiln, the kiln's own reason
     * @param totalFirings for a kiln, its firings so far; null otherwise
     * @param tasks soonest due first
     */
    public record Asset(UUID id, AssetKind kind, String name, @Nullable UUID kilnId, @Nullable String location,
            @Nullable String notes, @Nullable String downReason, @Nullable Integer totalFirings, long version,
            List<Task> tasks) {

        public boolean available() {
            return downReason == null;
        }

    }

    public record Task(UUID id, UUID assetId, String assetName, String title, List<String> checklist,
            @Nullable Integer everyDays, @Nullable Integer everyFirings, @Nullable Duty assignedDuty,
            @Nullable UUID assignedTo, @Nullable String assignedToName, Instant lastDoneAt, @Nullable Instant dueAt,
            @Nullable Integer firingsLeft, DueStatus status, long version) {
    }

    /** @param parts spare parts taken from supplies for this work */
    public record Record(UUID id, UUID assetId, @Nullable UUID taskId, @Nullable String taskTitle,
            @Nullable String note, @Nullable BigDecimal cost, List<String> checked, @Nullable Integer atFiring,
            UUID doneBy, @Nullable String doneByName, Instant doneAt, List<PartUsed> parts) {
    }

    /** A spare part drawn from supplies; name and unit as the supply item has them now. */
    public record PartUsed(UUID itemId, String itemName, String unit, BigDecimal quantity) {
    }

    /** A part to draw from supplies when logging work. */
    public record PartUse(UUID itemId, BigDecimal quantity) {
    }

    /**
     * Something a member noticed, such as "wheel 3 grinds". Open until staff resolve it.
     *
     * @param assetId null for "somewhere else", or when the asset is no longer tracked
     */
    public record ProblemReport(UUID id, @Nullable UUID assetId, @Nullable String assetName, String description,
            UUID reportedBy, @Nullable String reportedByName, Instant reportedAt, @Nullable UUID resolvedBy,
            @Nullable String resolvedByName, @Nullable Instant resolvedAt, @Nullable String resolution, long version) {

        public boolean open() {
            return resolvedAt == null;
        }

    }

    /** What staff set on a task. Validated by the service, which knows the asset. */
    public record TaskSettings(String title, List<String> checklist, @Nullable Integer everyDays,
            @Nullable Integer everyFirings, @Nullable Duty assignedDuty, @Nullable UUID assignedTo) {
    }

}
