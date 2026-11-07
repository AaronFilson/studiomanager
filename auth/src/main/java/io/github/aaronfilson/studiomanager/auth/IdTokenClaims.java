package io.github.aaronfilson.studiomanager.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * Shapes every token the bridge signs. ID tokens carry the GitHub user's {@code sub}, {@code
 * email}, {@code email_verified}, {@code name} and {@code picture}, and are valid for 5 minutes:
 * Entra reads them once, straight after the code exchange (plan 18). Every token names the current
 * signing key, so the encoder never has to choose among the published keys.
 */
@Component
public class IdTokenClaims implements OAuth2TokenCustomizer<JwtEncodingContext> {

  static final Duration TOKEN_LIFETIME = Duration.ofMinutes(5);

  private final Clock clock;
  private final SigningKeys signingKeys;

  public IdTokenClaims(Clock clock, SigningKeys signingKeys) {
    this.clock = clock;
    this.signingKeys = signingKeys;
  }

  @Override
  public void customize(JwtEncodingContext context) {
    context.getJwsHeader().keyId(signingKeys.signingKey().getKeyID());
    Instant now = clock.instant();
    context.getClaims().issuedAt(now).expiresAt(now.plus(TOKEN_LIFETIME));

    if (!OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
      return;
    }
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

  private static String requiredAttribute(OAuth2User user, String name) {
    if (user.getAttributes().get(name) instanceof String value) {
      return value;
    }
    throw new IllegalStateException("The signed-in GitHub user has no " + name + ".");
  }
}
