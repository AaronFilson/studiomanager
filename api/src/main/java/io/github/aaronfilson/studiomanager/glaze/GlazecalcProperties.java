package io.github.aaronfilson.studiomanager.glaze;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the Glazecalc share-link and live OAuth integration (ADR 17).
 *
 * @param baseUrl Glazecalc origin, defaulting to {@code https://glazecalcapp.com}
 * @param clientId OAuth client id registered with Glazecalc (blank disables OAuth connect)
 * @param clientSecret optional OAuth client secret (PKCE is always used)
 * @param redirectUri default OAuth callback URI in the Studio Manager web app
 * @param tokenEncryptionKey AES-256 keyring for encrypting OAuth tokens and OAuth state at rest
 * @param allowPrivateNetworks allows HTTP/localhost for tests against a local stub server
 */
@ConfigurationProperties("studiomanager.glazecalc")
record GlazecalcProperties(
    @Nullable String baseUrl,
    @Nullable String clientId,
    @Nullable String clientSecret,
    @Nullable String redirectUri,
    @Nullable String tokenEncryptionKey,
    boolean allowPrivateNetworks) {

  String resolvedBaseUrl() {
    String url = baseUrl == null || baseUrl.isBlank() ? "https://glazecalcapp.com" : baseUrl.strip();
    while (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    return url;
  }

  boolean oauthConfigured() {
    return clientId != null && !clientId.isBlank();
  }
}
