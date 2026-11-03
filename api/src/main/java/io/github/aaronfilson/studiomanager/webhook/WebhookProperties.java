package io.github.aaronfilson.studiomanager.webhook;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secretKey the AES-256 key, base64, that encrypts endpoints' signing secrets at rest. At
 *     deploy it comes from Key Vault through the managed identity, never from the repo.
 * @param allowPrivateNetworks lets webhooks go to {@code http://localhost} and other private
 *     addresses. Only for local work and tests: it turns the SSRF filter off.
 */
@ConfigurationProperties("studiomanager.webhooks")
record WebhookProperties(@Nullable String secretKey, boolean allowPrivateNetworks) {
}
