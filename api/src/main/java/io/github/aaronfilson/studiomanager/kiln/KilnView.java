package io.github.aaronfilson.studiomanager.kiln;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A kiln and its firing count. Its maintenance lives in the maintenance module, which reads
 * {@code totalFirings} for tasks counted in firings.
 *
 * @param maxPiecesPerMember the queue rule "at most N pieces per member in one load"; null for no cap
 * @param downReason why it is out of service; null when it is available
 */
public record KilnView(UUID id, String name, @Nullable String site, KilnType kind,
        @Nullable Integer maxPiecesPerMember, int totalFirings, @Nullable String downReason, long version) {

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
