package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps a validated access token to its account, creating the account the first time the person
 * signs in. Implemented by the account module; declared here so tenancy doesn't depend on it.
 */
@FunctionalInterface
public interface AccountResolver {

  UUID resolve(Jwt token);

  /**
   * Looks up an existing person account for an MCP OAuth token subject without creating one (ADR
   * 26). Never matches a system account.
   */
  default Optional<UUID> findBySubject(String tokenIssuer, String subject) {
    return Optional.empty();
  }
}
