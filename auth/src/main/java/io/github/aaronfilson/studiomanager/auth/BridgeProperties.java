package io.github.aaronfilson.studiomanager.auth;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The bridge's settings.
 *
 * @param issuer this bridge's public origin, which is the {@code iss} of every token it signs
 * @param entraClientId the client ID Entra presents at the token endpoint
 * @param entraClientSecret the client secret Entra presents (client_secret_post)
 * @param entraRedirectUris Entra's federation callbacks, matched exactly
 * @param signingKeys PKCS#8 PEM RSA private keys; the first signs, the rest are only published
 * @param githubEmailsUri GitHub's list of the user's email addresses
 * @param authorizeRequestsPerMinute how many sign-ins this replica starts per minute
 */
@ConfigurationProperties("studiomanager.bridge")
public record BridgeProperties(
    URI issuer,
    String entraClientId,
    String entraClientSecret,
    List<URI> entraRedirectUris,
    String signingKeys,
    URI githubEmailsUri,
    int authorizeRequestsPerMinute) {}
