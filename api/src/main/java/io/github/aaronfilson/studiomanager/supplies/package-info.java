/**
 * Clay and supplies: items with par levels, stock movements, purchase requests, and sales to
 * members.
 *
 * <p>Data access follows ADR 21: Hibernate entities with {@code @TenantId} and {@code @Version},
 * Spring Data repositories, the application's clock, and no SQL of its own. Stock on hand is a sum
 * of movements, worked out in JPQL.
 */
@NullMarked
package io.github.aaronfilson.studiomanager.supplies;

import org.jspecify.annotations.NullMarked;
