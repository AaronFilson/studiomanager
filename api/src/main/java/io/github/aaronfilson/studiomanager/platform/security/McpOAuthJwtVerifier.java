package io.github.aaronfilson.studiomanager.platform.security;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.McpOAuthTokenVerifier;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Shared RS256 JWKS verifier for MCP OAuth 2.1 tokens (ADR 26). Used both by {@link
 * McpOAuthAuthenticationFilter} on the MCP endpoint and by the account module when linking an
 * external MCP identity to an existing Studio Manager account.
 *
 * <p>Enables {@link McpOAuthProperties} here rather than only on {@link SecurityConfiguration}
 * (which is {@code @ConditionalOnWebApplication}) so the {@code migrate} and {@code sweep}
 * Container App jobs ({@code web-application-type: none}) can wire {@code AccountService}.
 */
@Component
@EnableConfigurationProperties(McpOAuthProperties.class)
class McpOAuthJwtVerifier implements McpOAuthTokenVerifier {

  private final McpOAuthProperties properties;
  private final @Nullable JwtDecoder decoder;

  McpOAuthJwtVerifier(McpOAuthProperties properties) {
    this.properties = properties;
    this.decoder = properties.enabled() ? buildDecoder(properties) : null;
  }

  @Override
  public boolean enabled() {
    return properties.enabled();
  }

  /** Decodes and validates a raw MCP OAuth JWT, throwing {@link JwtException} on failure. */
  Jwt decode(String bearer) {
    if (decoder == null) {
      throw new JwtException("MCP OAuth is not configured on this server.");
    }
    return decoder.decode(bearer);
  }

  @Override
  public VerifiedIdentity verify(String token) {
    if (!properties.enabled() || decoder == null) {
      throw new InvalidInputException("MCP OAuth is not enabled on this server.");
    }
    String raw = token.strip();
    if (raw.startsWith("Bearer ")) {
      raw = raw.substring("Bearer ".length()).strip();
    }
    if (raw.isEmpty()) {
      throw new InvalidInputException("Paste an MCP OAuth token to link.");
    }
    Jwt jwt;
    try {
      jwt = decoder.decode(raw);
    } catch (JwtException invalid) {
      throw new InvalidInputException("That MCP OAuth token isn't valid or has expired.");
    }
    String issuer = jwt.getClaimAsString("iss");
    String subject = jwt.getSubject();
    if (issuer == null
        || !properties.normalizedIssuer().equals(issuer)
        || SystemActor.ISSUER.equals(issuer)
        || subject == null
        || subject.isBlank()) {
      throw new InvalidInputException("That MCP OAuth token doesn't carry a valid identity.");
    }
    if (properties.configuredAudience().isPresent()) {
      List<String> audiences = jwt.getAudience();
      String configured = properties.configuredAudience().get();
      String origin = properties.resourceOrigin().orElse("");
      boolean matches =
          audiences != null
              && audiences.stream()
                  .anyMatch(aud ->
                      aud.equals(configured)
                          || (!origin.isEmpty()
                              && aud.startsWith(origin + "/api/v1/studios/")
                              && aud.endsWith("/mcp")));
      if (!matches) {
        throw new InvalidInputException("That MCP OAuth token wasn't issued for this server.");
      }
    }
    return new VerifiedIdentity(issuer, subject);
  }

  private static JwtDecoder buildDecoder(McpOAuthProperties properties) {
    NimbusJwtDecoder jwtDecoder =
        NimbusJwtDecoder.withJwkSetUri(properties.effectiveJwkSetUri())
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();
    jwtDecoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            List.of(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.normalizedIssuer()))));
    return jwtDecoder;
  }
}
