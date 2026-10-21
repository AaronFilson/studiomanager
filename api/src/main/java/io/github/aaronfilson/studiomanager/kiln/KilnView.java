package io.github.aaronfilson.studiomanager.kiln;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A kiln and its counters.
 *
 * @param maxPiecesPerMember the queue rule "at most N pieces per member in one load"; null for no cap
 * @param maintenanceEveryFirings how often the studio services it, counted in firings; null if untracked
 * @param downReason why it is out of service; null when it is available
 */
public record KilnView(UUID id, String name, @Nullable String site, KilnType kind,
        @Nullable Integer maxPiecesPerMember, @Nullable Integer maintenanceEveryFirings, int firingsSinceMaintenance,
        int totalFirings, @Nullable String downReason, long version) {

    public boolean maintenanceDue() {
        return maintenanceEveryFirings != null && firingsSinceMaintenance >= maintenanceEveryFirings;
    }

    public boolean available() {
        return downReason == null;
    }

    public enum KilnType {
        ELECTRIC,
        GAS,
        WOOD,
        RAKU,
        OTHER
    }

}
