package io.github.aaronfilson.studiomanager.firing;

import io.github.aaronfilson.studiomanager.platform.Money;

/**
 * The price of one piece under one schedule version, with the working shown, for example
 * {@code "12 × 10 × 8 in → 960 in³ × 0.05 = 48.00"}. Pieces store this as their fee snapshot.
 */
public record FeeQuote(Money amount, int scheduleVersion, String taxCategory, String breakdown) {
}
