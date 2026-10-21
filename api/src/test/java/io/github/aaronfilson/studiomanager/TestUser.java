package io.github.aaronfilson.studiomanager;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * A person signing in to the API. Subjects are unique per instance, so tests that share a
 * database never see each other's accounts.
 */
public record TestUser(String subject, String name) {

    public static final String ISSUER = "https://issuer.test/";

    public static TestUser named(String name) {
        return new TestUser(name.toLowerCase() + "-" + UUID.randomUUID(), name);
    }

    /** An access token for the API, carrying its {@code access_as_user} scope. */
    public RequestPostProcessor token() {
        return jwt().jwt(token -> token.issuer(ISSUER).subject(subject).claim("name", name))
                .authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"));
    }

    /** A valid token issued for some other API. */
    public RequestPostProcessor tokenWithoutApiScope() {
        return jwt().jwt(token -> token.issuer(ISSUER).subject(subject))
                .authorities(new SimpleGrantedAuthority("SCOPE_something_else"));
    }

}
