package io.github.aaronfilson.studiomanager.platform.security;

import java.util.List;

import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.MembershipLookup;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContextFilter;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The API is an OAuth 2.0 resource server: it accepts Microsoft Entra External ID access
 * tokens (issuer and audience are validated from configuration) that carry the API's
 * {@code access_as_user} scope.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityConfiguration.CorsProperties.class)
class SecurityConfiguration {

    static final String API_SCOPE = "SCOPE_access_as_user";

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, AccountResolver accounts, MembershipLookup memberships,
            JsonMapper json) {
        ProblemJsonSecurityHandler problems = new ProblemJsonSecurityHandler(json);
        http.authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health/**", "/error").permitAll()
                        .requestMatchers("/api/**").hasAuthority(API_SCOPE)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Tokens travel in the Authorization header, never a cookie, so there is no CSRF to defend
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .addFilterAfter(new TenantContextFilter(accounts, memberships), AuthorizationFilter.class);
        return http.build();
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

    /** @param allowedOrigins origins of the web app, for example the Static Web App's URL */
    @ConfigurationProperties("studiomanager.cors")
    record CorsProperties(List<String> allowedOrigins) {
    }

}
