package io.github.aaronfilson.studiomanager.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Two filter chains. The first serves the OpenID Connect provider's endpoints (discovery, JWKS,
 * authorize, token, userinfo) to its single client, Entra. The second signs people in with GitHub,
 * which is where an unauthenticated /oauth2/authorize sends the browser.
 */
@Configuration
public class BridgeSecurity {

  static final String GITHUB_SIGN_IN = "/oauth2/authorization/github";
  private static final Duration AUTHORIZATION_CODE_LIFETIME = Duration.ofMinutes(1);

  @Bean
  @Order(1)
  SecurityFilterChain authorizationServer(HttpSecurity http, BridgeProperties properties)
      throws Exception {
    http.oauth2AuthorizationServer(
            server -> {
              http.securityMatcher(server.getEndpointsMatcher());
              server.oidc(Customizer.withDefaults());
            })
        .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
        .exceptionHandling(exceptions ->
            exceptions.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint(GITHUB_SIGN_IN),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
        .addFilterBefore(
            new AuthorizeRateLimit(properties.authorizeRequestsPerMinute()),
            SecurityContextHolderFilter.class)
        .headers(BridgeSecurity::securityHeaders);
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain gitHubSignIn(
      HttpSecurity http,
      GitHubUserService gitHubUsers,
      ClientRegistrationRepository clientRegistrations)
      throws Exception {
    http.authorizeHttpRequests(requests ->
            requests
                .requestMatchers("/actuator/health/**", "/error")
                .permitAll()
                .anyRequest()
                .authenticated())
        .oauth2Login(login ->
            login
                .authorizationEndpoint(endpoint ->
                    endpoint.authorizationRequestResolver(withPkce(clientRegistrations)))
                .userInfoEndpoint(userInfo -> userInfo.userService(gitHubUsers))
                .authorizedClientRepository(new ForgetGitHubTokens())
                .failureHandler(new SignInRefused()))
        .headers(BridgeSecurity::securityHeaders);
    return http.build();
  }

  /** PKCE on the GitHub leg too, on top of the state check Spring Security always does. */
  private static OAuth2AuthorizationRequestResolver withPkce(
      ClientRegistrationRepository clientRegistrations) {
    DefaultOAuth2AuthorizationRequestResolver resolver =
        new DefaultOAuth2AuthorizationRequestResolver(clientRegistrations, "/oauth2/authorization");
    resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    return resolver;
  }

  /** The bridge serves no content of its own, so its pages may load and frame nothing. */
  private static void securityHeaders(HeadersConfigurer<HttpSecurity> headers) {
    headers
        .contentSecurityPolicy(
            policy -> policy.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
        .referrerPolicy(
            referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
  }

  /**
   * Entra, the only client. A confidential client with client_secret_post (Entra doesn't support
   * client_secret_basic), exact redirect URIs, no consent screen, and PKCE checked whenever Entra
   * sends a challenge. The secret is kept only as a bcrypt hash.
   */
  @Bean
  RegisteredClientRepository registeredClients(BridgeProperties properties) {
    RegisteredClient.Builder entra =
        RegisteredClient.withId(
                UUID.nameUUIDFromBytes(properties.entraClientId().getBytes(StandardCharsets.UTF_8))
                    .toString())
            .clientId(properties.entraClientId())
            .clientSecret(
                PasswordEncoderFactories.createDelegatingPasswordEncoder()
                    .encode(properties.entraClientSecret()))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .clientSettings(
                ClientSettings.builder()
                    .requireAuthorizationConsent(false)
                    .requireProofKey(false)
                    .build())
            .tokenSettings(
                TokenSettings.builder()
                    .authorizationCodeTimeToLive(AUTHORIZATION_CODE_LIFETIME)
                    .accessTokenTimeToLive(IdTokenClaims.TOKEN_LIFETIME)
                    .build());
    for (URI redirectUri : properties.entraRedirectUris()) {
      entra.redirectUri(redirectUri.toString());
    }
    return new InMemoryRegisteredClientRepository(entra.build());
  }

  /**
   * Codes and the tokens issued for them, in memory. A code lives a minute and is used once; the
   * bridge runs one replica, so nothing else needs to see it (ADR 20).
   */
  @Bean
  OAuth2AuthorizationService authorizations() {
    return new InMemoryOAuth2AuthorizationService();
  }

  @Bean
  AuthorizationServerSettings authorizationServerSettings(BridgeProperties properties) {
    return AuthorizationServerSettings.builder().issuer(properties.issuer().toString()).build();
  }

  @Bean
  SigningKeys signingKeys(BridgeProperties properties) {
    return SigningKeys.fromPem(properties.signingKeys());
  }

  @Bean
  JWKSource<SecurityContext> jwkSource(SigningKeys signingKeys) {
    return new ImmutableJWKSet<>(signingKeys.keySet());
  }

  /** Validates the bridge's own access tokens at userinfo. */
  @Bean
  JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
    return new NimbusJwtDecoder(processor);
  }
}
