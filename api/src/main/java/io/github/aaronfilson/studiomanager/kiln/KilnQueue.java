package io.github.aaronfilson.studiomanager.kiln;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * The queue rule studios publish most: longest-waiting first, with at most N pieces per member
 * in one load, so a production potter can't fill the kiln. Pure, so it is tested on its own.
 */
public final class KilnQueue {

    private KilnQueue() {
    }

    /**
     * Marks which of {@code waiting} (already ordered longest-waiting first) fit in the next load
     * under the per-member cap. Order is kept; pieces over the cap wait for a later load.
     */
    public static <T> List<Entry<T>> plan(List<T> waiting, Function<T, UUID> maker, @Nullable Integer maxPerMember) {
        Map<UUID, Integer> taken = new HashMap<>();
        List<Entry<T>> entries = new ArrayList<>(waiting.size());
        for (T piece : waiting) {
            int count = taken.merge(maker.apply(piece), 1, Integer::sum);
            entries.add(new Entry<>(piece, maxPerMember == null || count <= maxPerMember));
        }
        return entries;
    }

    /** One piece in the queue, and whether the cap lets it into the next load. */
    public record Entry<T>(T piece, boolean fitsNextLoad) {
    }

}
