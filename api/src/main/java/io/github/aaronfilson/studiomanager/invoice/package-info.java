/**
 * The supplies inbox: supplier invoices uploaded by staff, read into a draft by an {@link
 * io.github.aaronfilson.studiomanager.invoice.InvoiceReader}, matched to supply items, checked to
 * add up, and confirmed by a person into stock (docs/planning/16, ADR 18).
 *
 * <p>Written on Hibernate from the start (ADR 21): entities with {@code @TenantId} and {@code
 * Version}, Spring Data repositories, and SQL only in {@code InvoiceQueries}, each with its reason.
 */
@NullMarked
package io.github.aaronfilson.studiomanager.invoice;

import org.jspecify.annotations.NullMarked;
