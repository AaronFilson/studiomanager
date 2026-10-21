package io.github.aaronfilson.studiomanager.maintenance;

import java.util.List;

import io.github.aaronfilson.studiomanager.kiln.KilnView.KilnType;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.AssetKind;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.TaskSettings;

import org.jspecify.annotations.Nullable;

/**
 * Tasks to suggest when an asset is added, only where an interval is published: by the kiln's
 * maker, a regulation or a standard. Every interval and its source is in
 * docs/planning/05-maintenance-research.md. Other machines (wheels, pug mills, slab rollers) have
 * no published schedule, so nothing is suggested for them rather than something made up.
 */
final class StarterTasks {

    private static final String LL = "L&L Kiln Mfg maintenance schedule (electric kilns)";

    private StarterTasks() {
    }

    record Suggestion(TaskSettings task, String source) {
    }

    /** @param kilnType the kiln's type, for a kiln asset; null otherwise */
    static List<Suggestion> forKind(AssetKind kind, @Nullable KilnType kilnType) {
        return switch (kind) {
            // L&L's schedule is for electric kilns; gas and wood kilns differ too much to guess
            case KILN -> kilnType == KilnType.ELECTRIC ? List.of(
                    suggest("Clean and inspect before firing", null, 1, LL,
                            "Vacuum the floor and element grooves", "Check for glaze or clay chips",
                            "Kiln-wash shelf tops where needed"),
                    suggest("Check elements and the circuit", null, 10, LL,
                            "Measure element resistance (replace when up 8–10%)",
                            "Feel the cord, receptacle and breaker at peak temperature", "Check the case is tight",
                            "Repair any brick"),
                    suggest("Wiring and terminal check", 365, 30, LL, "Check the wiring and terminals are tight"))
                    : List.of();
            case CLAY_TRAP -> List.of(
                    suggest("Check and empty the trap", 7, null, "Common studio practice (technician postings)",
                            "Is the level below the fill line?", "Scoop out the settled clay"));
            case SAFETY -> List.of(
                    suggest("Fire extinguisher visual check", 30, null, "OSHA 29 CFR 1910.157(e): monthly",
                            "Record the date of the check"),
                    suggest("Fire extinguisher annual maintenance", 365, null, "OSHA 29 CFR 1910.157(e): yearly"),
                    suggest("Activate the eyewash", 7, null, "ANSI Z358.1: weekly"),
                    suggest("Eyewash service", 365, null, "ANSI Z358.1: yearly"));
            case BUILDING -> List.of(
                    suggest("Roof inspection", 182, null, "NRCA: twice a year"));
            default -> List.of();
        };
    }

    private static Suggestion suggest(String title, @Nullable Integer everyDays, @Nullable Integer everyFirings,
            String source, String... checklist) {
        return new Suggestion(new TaskSettings(title, List.of(checklist), everyDays, everyFirings, null, null), source);
    }

}
