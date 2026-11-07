/**
 * Studios, the people in them, and inviting more.
 *
 * <p>On Hibernate (ADR 21). {@code Studio} and {@code StudioMembership} carry no {@code @TenantId},
 * so "my studios" works with no studio bound; invites do. The only SQL is accepting an invite,
 * which goes through a security-definer function ({@code StudioQueries}).
 */
@NullMarked
package io.github.aaronfilson.studiomanager.studio;

import org.jspecify.annotations.NullMarked;
