package io.github.aaronfilson.studiomanager.platform.security;

import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.ApiTokenAuthentication;
import io.github.aaronfilson.studiomanager.platform.tenancy.ApiTokenAuthenticationFilter;
import io.github.aaronfilson.studiomanager.platform.tenancy.ApiTokenResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.MembershipLookup;
import io.github.aaronfilson.studiomanager.platform.tenancy.PlatformAdmin;
import io.github.aaronfilson.studiomanager.platform.tenancy.PlatformAdminFilter;
import io.github.aaronfilson.studiomanager.platform.tenancy.RolePermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContextFilter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * The API is an OAuth 2.0 resource server: it accepts Microsoft Entra External ID access tokens
 * (issuer and audience are validated from configuration) that carry the API's {@code
 * access_as_user} scope.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityConfiguration.CorsProperties.class)
class SecurityConfiguration {

  static final String API_SCOPE = "SCOPE_access_as_user";

  @Bean
  SecurityFilterChain apiSecurity(
      HttpSecurity http,
      AccountResolver accounts,
      MembershipLookup memberships,
      RolePermissions rolePermissions,
      ApiTokenResolver apiTokens,
      JsonMapper json) {
    ProblemJsonSecurityHandler problems = new ProblemJsonSecurityHandler(json);
    DefaultBearerTokenResolver jwtBearer = new DefaultBearerTokenResolver();
    http.authorizeHttpRequests(requests ->
            requests
                .requestMatchers("/actuator/health/**", "/error")
                .permitAll()
                // Only people with the Platform.Admin app role; never an API token (ADR 24)
                .requestMatchers("/api/v1/admin/**")
                .access(
                    AuthorizationManagers.allOf(
                        AuthorityAuthorizationManager.hasAuthority(API_SCOPE),
                        AuthorityAuthorizationManager.hasAuthority(PlatformAdmin.AUTHORITY)))
                .requestMatchers("/api/**")
                .hasAnyAuthority(API_SCOPE, ApiTokenAuthentication.AUTHORITY)
                .anyRequest()
                .denyAll())
        .oauth2ResourceServer(oauth2 ->
            oauth2
                // Personal API tokens are opaque, not JWTs; their own filter signs them in
                .bearerTokenResolver(request ->
                    ApiTokenAuthenticationFilter.isApiToken(request)
                        ? null
                        : jwtBearer.resolve(request))
                .jwt(jwt -> jwt.jwtAuthenticationConverter(scopesAndAppRoles()))
                .authenticationEntryPoint(problems)
                .accessDeniedHandler(problems))
        .exceptionHandling(exceptions ->
            exceptions.authenticationEntryPoint(problems).accessDeniedHandler(problems))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Tokens travel in the Authorization header, never a cookie, so there is no CSRF to defend
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .addFilterBefore(
            new ApiTokenAuthenticationFilter(apiTokens, problems),
            BearerTokenAuthenticationFilter.class)
        .addFilterAfter(
            new TenantContextFilter(accounts, memberships, rolePermissions),
            AuthorizationFilter.class)
        .addFilterAfter(new PlatformAdminFilter(), TenantContextFilter.class);
    return http.build();
  }

  /**
   * Authorities from the access token: its scopes ({@code scp}) as {@code SCOPE_...}, as Spring's
   * default does, and its app roles ({@code roles}) as {@code APPROLE_...}.
   */
  static JwtAuthenticationConverter scopesAndAppRoles() {
    JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
    JwtGrantedAuthoritiesConverter appRoles = new JwtGrantedAuthoritiesConverter();
    appRoles.setAuthoritiesClaimName("roles");
    appRoles.setAuthorityPrefix("APPROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(token -> {
      List<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(token));
      authorities.addAll(appRoles.convert(token));
      return authorities;
    });
    return converter;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(properties.allowedOrigins());
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
    cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
    // Content-Disposition carries a download's file name, such as the studio export's
    cors.setExposedHeaders(List.of(HttpHeaders.LOCATION, HttpHeaders.CONTENT_DISPOSITION));
    cors.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors);
    return source;
  }

  /**
   * @param allowedOrigins origins of the web app, for example the Static Web App's URL
   */
  @ConfigurationProperties("studiomanager.cors")
  record CorsProperties(List<String> allowedOrigins) {}
}
