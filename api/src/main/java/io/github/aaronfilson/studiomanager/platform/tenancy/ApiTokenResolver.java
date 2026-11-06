package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Finds the live personal API token behind a bearer value. Implemented by the apitoken module;
 * declared here so tenancy doesn't depend on it.
 */
@FunctionalInterface
public interface ApiTokenResolver {

  /**
   * Every personal API token starts with this, so it's told apart from a JWT, and secret scanners
   * can spot one.
   */
  String PREFIX = "smpat_";

  /** An unexpired, unrevoked token: whose it is, the one studio it works in, and what it may do. */
  record ResolvedToken(UUID tokenId, UUID accountId, UUID studioId, Set<TokenScope> scopes) {}

  Optional<ResolvedToken> resolve(String bearer);
}
