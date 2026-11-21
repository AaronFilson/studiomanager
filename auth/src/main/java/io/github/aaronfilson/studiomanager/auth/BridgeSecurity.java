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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationServerMetadata;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.oidc.OidcProviderConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Security filter chains for the OIDC bridge (ADR 20) and optional MCP OAuth 2.1 authorization
 * server (ADR 26).
 *
 * <ol>
 *   <li>Stateless, rate-limited RFC 7591 dynamic client registration ({@code /oauth2/register}).
 *   <li>OAuth 2.1 / OIDC Authorization Server endpoints (discovery, JWKS, authorize, token,
 *       userinfo) for Entra federation and MCP clients.
 *   <li>Explicit MCP consent page ({@code /oauth2/consent}) with strict CSP.
 *   <li>Upstream sign-in with GitHub OAuth.
 * </ol>
 */
@Configuration
public class BridgeSecurity {

  static final String GITHUB_SIGN_IN = "/oauth2/authorization/github";
  static final Duration AUTHORIZATION_CODE_LIFETIME = Duration.ofMinutes(1);

  @Bean
  @Order(1)
  SecurityFilterChain mcpClientRegistration(HttpSecurity http, BridgeProperties properties)
      throws Exception {
    http.securityMatcher(RegisterRateLimit.REGISTER_PATH)
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable)
        .addFilterBefore(
            new RegisterRateLimit(
                properties.mcpSettings().effectiveRegisterRequestsPerMinute(),
                properties.mcpSettings().effectiveRegisterRequestsPerMinutePerIp()),
            SecurityContextHolderFilter.class)
        .headers(BridgeSecurity::securityHeaders);
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain authorizationServer(HttpSecurity http, BridgeProperties properties)
      throws Exception {
    BridgeProperties.McpProperties mcp = properties.mcpSettings();
    http.oauth2AuthorizationServer(
            server -> {
              http.securityMatcher(server.getEndpointsMatcher());
              if (mcp.enabled()) {
                server
                    .authorizationEndpoint(endpoint ->
                        endpoint
                            .consentPage(McpConsentController.CONSENT_PATH)
                            .authenticationProviders(providers ->
                                configureMcpAuthorizationValidation(providers, properties)))
                    .authorizationServerMetadataEndpoint(meta ->
                        meta.authorizationServerMetadataCustomizer(
                            builder -> customizeServerMetadata(builder, properties)))
                    .oidc(oidc ->
                        oidc.providerConfigurationEndpoint(cfg ->
                            cfg.providerConfigurationCustomizer(
                                builder -> customizeOidcMetadata(builder, properties))));
              } else {
                server.oidc(Customizer.withDefaults());
              }
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
  @Order(3)
  SecurityFilterChain mcpConsent(HttpSecurity http) throws Exception {
    http.securityMatcher(McpConsentController.CONSENT_PATH, McpConsentController.CONSENT_CSS_PATH)
        .authorizeHttpRequests(requests ->
            requests
                .requestMatchers(McpConsentController.CONSENT_CSS_PATH)
                .permitAll()
                .anyRequest()
                .authenticated())
        .exceptionHandling(exceptions ->
            exceptions.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint(GITHUB_SIGN_IN),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
        .headers(BridgeSecurity::consentSecurityHeaders);
    return http.build();
  }

  @Bean
  @Order(4)
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

  private static void configureMcpAuthorizationValidation(
      List<AuthenticationProvider> providers, BridgeProperties properties) {
    Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> validator =
        OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR
            .andThen(OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR)
            .andThen(context -> validateMcpAuthorizationRequest(context, properties));
    for (AuthenticationProvider provider : providers) {
      if (provider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider codeProvider) {
        codeProvider.setAuthenticationValidator(validator);
      }
    }
  }

  private static void validateMcpAuthorizationRequest(
      OAuth2AuthorizationCodeRequestAuthenticationContext context, BridgeProperties properties) {
    RegisteredClient client = context.getRegisteredClient();
    if (properties.entraClientId().equals(client.getClientId())) {
      return;
    }
    OAuth2AuthorizationCodeRequestAuthenticationToken authentication = context.getAuthentication();
    try {
      McpResources.validateAndResolve(
          McpConsentController.stringParameter(
              authentication.getAdditionalParameters(), McpResources.RESOURCE_PARAMETER),
          McpConsentController.stringParameter(
              authentication.getAdditionalParameters(), McpResources.STUDIO_ID_PARAMETER),
          properties.mcpSettings().effectiveAllowedResources());
    } catch (OAuth2AuthenticationException invalid) {
      throw new OAuth2AuthorizationCodeRequestAuthenticationException(
          invalid.getError(), authentication);
    }
  }

  private static void customizeServerMetadata(
      OAuth2AuthorizationServerMetadata.Builder builder, BridgeProperties properties) {
    BridgeProperties.McpProperties mcp = properties.mcpSettings();
    builder
        .claim("registration_endpoint", registrationEndpoint(properties))
        .scopes(scopes -> {
          scopes.clear();
          scopes.addAll(mcp.effectiveScopes());
        })
        .codeChallengeMethods(methods -> {
          methods.clear();
          methods.add("S256");
        })
        .tokenEndpointAuthenticationMethods(methods -> {
          if (!methods.contains("none")) {
            methods.add("none");
          }
        });
  }

  private static void customizeOidcMetadata(
      OidcProviderConfiguration.Builder builder, BridgeProperties properties) {
    BridgeProperties.McpProperties mcp = properties.mcpSettings();
    builder
        .claim("registration_endpoint", registrationEndpoint(properties))
        .scopes(scopes -> {
          for (String scope : mcp.effectiveScopes()) {
            if (!scopes.contains(scope)) {
              scopes.add(scope);
            }
          }
        })
        .codeChallengeMethods(methods -> {
          methods.clear();
          methods.add("S256");
        })
        .tokenEndpointAuthenticationMethods(methods -> {
          if (!methods.contains("none")) {
            methods.add("none");
          }
        });
  }

  private static String registrationEndpoint(BridgeProperties properties) {
    String issuer = properties.issuer().toString();
    while (issuer.endsWith("/")) {
      issuer = issuer.substring(0, issuer.length() - 1);
    }
    return issuer + RegisterRateLimit.REGISTER_PATH;
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
   * Strict CSP for the MCP consent screen: own stylesheet only, no scripts, no framing. Omits
   * {@code form-action 'self'} so browsers allow the 302 redirect back to the OAuth client's
   * registered {@code redirect_uri}; CSRF protection still guards the form post.
   */
  private static void consentSecurityHeaders(HeadersConfigurer<HttpSecurity> headers) {
    headers
        .contentSecurityPolicy(policy ->
            policy.policyDirectives(
                "default-src 'none'; style-src 'self'; frame-ancestors 'none'"))
        .referrerPolicy(
            referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
  }

  /**
   * Entra (confidential client with {@code client_secret_post}) plus any optional pre-registered
   * public PKCE MCP client, wrapped in a repository that also stores dynamically registered MCP
   * clients when MCP OAuth is enabled.
   */
  @Bean
  RegisteredClientRepository registeredClients(BridgeProperties properties) {
    List<RegisteredClient> fixed = new ArrayList<>();
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
    fixed.add(entra.build());

    BridgeProperties.McpProperties mcp = properties.mcpSettings();
    String preRegisteredId = mcp.preRegisteredClientId();
    List<URI> preRegisteredRedirects = mcp.effectivePreRegisteredRedirectUris();
    if (mcp.enabled()
        && preRegisteredId != null
        && !preRegisteredId.isBlank()
        && !preRegisteredRedirects.isEmpty()) {
      RegisteredClient.Builder mcpClient =
          RegisteredClient.withId(
                  UUID.nameUUIDFromBytes(
                          ("mcp:" + preRegisteredId.strip()).getBytes(StandardCharsets.UTF_8))
                      .toString())
              .clientId(preRegisteredId.strip())
              .clientName("Studio Manager MCP Client")
              .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .clientSettings(
                  ClientSettings.builder()
                      .requireAuthorizationConsent(true)
                      .requireProofKey(true)
                      .build())
              .tokenSettings(
                  TokenSettings.builder()
                      .authorizationCodeTimeToLive(AUTHORIZATION_CODE_LIFETIME)
                      .accessTokenTimeToLive(mcp.effectiveAccessTokenTimeToLive())
                      .build());
      for (URI redirectUri : preRegisteredRedirects) {
        mcpClient.redirectUri(redirectUri.toString());
      }
      for (String scope : mcp.effectiveScopes()) {
        mcpClient.scope(scope);
      }
      fixed.add(mcpClient.build());
    }

    return new McpClientRepository(properties.entraClientId(), fixed);
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
  OAuth2AuthorizationConsentService authorizationConsentService() {
    return new McpAuthorizationConsentService();
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
