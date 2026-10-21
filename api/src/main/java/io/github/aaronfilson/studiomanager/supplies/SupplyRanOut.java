package io.github.aaronfilson.studiomanager.supplies;

import java.util.UUID;

/**
 * An item with a substitute has just run out, so members should hear what to use instead
 * ("B-Mix 5 is out; use Speckled buff while we wait"). Published in the transaction that took
 * the last of it; the notify module tells every member.
 */
public record SupplyRanOut(UUID studioId, UUID itemId, String itemName, String substitute) {
}
