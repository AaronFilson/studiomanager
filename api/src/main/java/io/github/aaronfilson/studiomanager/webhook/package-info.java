/**
 * Outbound webhooks: endpoints, events, signed deliveries with retries, and the delivery log.
 *
 * <p>On Hibernate (ADR 21) for endpoints, new deliveries and the log. The SQL it keeps is in {@code
 * WebhookQueries}: the event insert's dedupe ({@code on conflict do nothing}), the delivery claim
 * and the recording of its outcome, and the two cross-tenant sweeps.
 */
@NullMarked
package io.github.aaronfilson.studiomanager.webhook;

import org.jspecify.annotations.NullMarked;
