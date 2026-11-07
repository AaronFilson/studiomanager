/**
 * The studio calendar (resources and bookings) and the roster (shifts and credited hours).
 *
 * <p>On Hibernate (ADR 21), with no SQL of its own: a resource links to a maintained asset by id,
 * and firings reach the calendar through the kiln module's events.
 */
@NullMarked
package io.github.aaronfilson.studiomanager.schedule;

import org.jspecify.annotations.NullMarked;
