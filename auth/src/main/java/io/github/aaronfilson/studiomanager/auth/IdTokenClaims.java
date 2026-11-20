package io.github.aaronfilson.studiomanager.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * Shapes every token the bridge signs.
 *
 * <ul>
 *   <li>Entra ID tokens carry the GitHub user's {@code sub}, {@code email}, {@code email_verified},
 *       {@code name} and {@code picture}, valid for 5 minutes (plan 18, ADR 20).
 *   <li>MCP OAuth 2.1 access tokens carry {@code sub}, {@code studio_id}, space-delimited {@code
 *       scope}, and an RFC 8707 {@code aud} matching the authorized resource URI (plan 17 step 5,
 *       plan 18 step 3, ADR 26).
 * </ul>
 *
 * <p>Every token names the current signing key, so the encoder never has to choose among the
 * published keys.
 */
@Component
public class IdTokenClaims implements OAuth2TokenCustomizer<JwtEncodingContext> {

  static final Duration TOKEN_LIFETIME = Duration.ofMinutes(5);

  private final Clock clock;
  private final SigningKeys signingKeys;
  private final BridgeProperties properties;

  public IdTokenClaims(Clock clock, SigningKeys signingKeys, BridgeProperties properties) {
    this.clock = clock;
    this.signingKeys = signingKeys;
    this.properties = properties;
  }

  @Override
  public void customize(JwtEncodingContext context) {
    context.getJwsHeader().keyId(signingKeys.signingKey().getKeyID());
    Instant now = clock.instant();
    context.getClaims().issuedAt(now);

    if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
      customizeEntraIdToken(context, now);
      return;
    }

    if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())
        && !properties.entraClientId().equals(context.getRegisteredClient().getClientId())) {
      customizeMcpAccessToken(context, now);
      return;
    }

    context.getClaims().expiresAt(now.plus(TOKEN_LIFETIME));
  }

  private static void customizeEntraIdToken(JwtEncodingContext context, Instant now) {
    context.getClaims().expiresAt(now.plus(TOKEN_LIFETIME));
    Authentication principal = context.getPrincipal();
    if (principal == null || !(principal.getPrincipal() instanceof OAuth2User user)) {
      throw new IllegalStateException(
          "An ID token was requested for someone not signed in with GitHub.");
    }
    context
        .getClaims()
        .subject(principal.getName())
        .claim(StandardClaims.EMAIL, requiredAttribute(user, StandardClaims.EMAIL))
        // GitHubUserService only lets a verified primary address through
        .claim(StandardClaims.EMAIL_VERIFIED, true)
        .claim(StandardClaims.NAME, requiredAttribute(user, StandardClaims.NAME));
    if (user.getAttributes().get(StandardClaims.PICTURE) instanceof String picture) {
      context.getClaims().claim(StandardClaims.PICTURE, picture);
    }
  }

  private void customizeMcpAccessToken(JwtEncodingContext context, Instant now) {
    BridgeProperties.McpProperties mcp = properties.mcpSettings();
    OAuth2Authorization authorization = context.getAuthorization();
    if (authorization == null) {
      throw McpResources.invalidTarget("Missing authorization context for MCP access token.");
    }
    OAuth2AuthorizationRequest authRequest =
        authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
    if (authRequest == null) {
      throw McpResources.invalidTarget("Missing authorization request for MCP access token.");
    }
    McpResources.Target target =
        McpResources.validateAndResolve(
            McpConsentController.stringParameter(
                authRequest.getAdditionalParameters(), McpResources.RESOURCE_PARAMETER),
            McpConsentController.stringParameter(
                authRequest.getAdditionalParameters(), McpResources.STUDIO_ID_PARAMETER),
            mcp.effectiveAllowedResources());

    // RFC 8707 §2: if the token request also includes a resource parameter, it must match the
    // authorized resource.
    if (context.getAuthorizationGrant() instanceof OAuth2AuthorizationCodeAuthenticationToken grant) {
      String tokenResource =
          McpConsentController.stringParameter(
              grant.getAdditionalParameters(), McpResources.RESOURCE_PARAMETER);
      if (tokenResource != null
          && !tokenResource.isBlank()
          && !target.resource().toString().equals(tokenResource.strip())) {
        throw McpResources.invalidTarget(
            "The token request's resource parameter doesn't match the authorized resource.");
      }
    }

    Authentication principal = context.getPrincipal();
    if (principal == null || principal.getName().isBlank()) {
      throw new IllegalStateException("An MCP access token requires an authenticated principal.");
    }
    String scope = String.join(" ", new TreeSet<>(context.getAuthorizedScopes()));
    context
        .getClaims()
        .expiresAt(now.plus(mcp.effectiveAccessTokenTimeToLive()))
        .subject(principal.getName())
        .audience(List.of(target.resource().toString()))
        .claim(McpResources.STUDIO_ID_PARAMETER, target.studioId().toString())
        .claim("scope", scope)
        .claim("client_id", context.getRegisteredClient().getClientId());
  }

  private static String requiredAttribute(OAuth2User user, String name) {
    if (user.getAttributes().get(name) instanceof String value) {
      return value;
    }
    throw new IllegalStateException("The signed-in GitHub user has no " + name + ".");
  }
}
