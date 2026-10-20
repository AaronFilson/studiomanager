package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps a validated access token to its account, creating the account the first time the
 * person signs in. Implemented by the account module; declared here so tenancy doesn't
 * depend on it.
 */
@FunctionalInterface
public interface AccountResolver {

    UUID resolve(Jwt token);

}
