package io.github.aaronfilson.studiomanager.webhook;

import java.util.UUID;

/**
 * An endpoint kept failing, or said it was gone, so it was turned off. Published inside the
 * transaction that turned it off, so staff are told exactly once.
 */
public record WebhookEndpointDisabled(UUID studioId, UUID endpointId, String url, String reason) {}
