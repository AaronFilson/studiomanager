package io.github.aaronfilson.studiomanager.glaze;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Raised inside the transaction when a studio glaze bucket is mixed. Webhooks and the Kafka stream
 * listen to this to publish {@code glaze.mixed}.
 */
public record GlazeMixed(
    UUID studioId,
    UUID mixId,
    UUID recipeId,
    String recipeName,
    UUID mixedByAccountId,
    Instant mixedAt,
    BigDecimal dryWeightG,
    @Nullable BigDecimal waterG,
    @Nullable BigDecimal specificGravity,
    @Nullable BigDecimal targetSg,
    @Nullable String notes) {}
