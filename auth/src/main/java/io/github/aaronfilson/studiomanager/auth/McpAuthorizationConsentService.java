package io.github.aaronfilson.studiomanager.auth;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;

/**
 * Never caches prior MCP consent across requests. Consent in Spring Authorization Server is keyed
 * only by client and principal (not studio or resource), so skipping storage ensures a client
 * authorized for one studio always prompts explicitly before receiving access to another studio
 * (ADR 26). Entra federation sets {@code requireAuthorizationConsent(false)} and never calls this.
 */
final class McpAuthorizationConsentService implements OAuth2AuthorizationConsentService {

  @Override
  public void save(OAuth2AuthorizationConsent authorizationConsent) {
    // Deliberately not persisted: each MCP authorization shows the consent screen.
  }

  @Override
  public void remove(OAuth2AuthorizationConsent authorizationConsent) {
    // Nothing was saved.
  }

  @Override
  public @Nullable OAuth2AuthorizationConsent findById(
      String registeredClientId, String principalName) {
    return null;
  }
}
