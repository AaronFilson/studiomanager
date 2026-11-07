package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Optional;
import java.util.UUID;

/**
 * The caller's role in a studio, if they belong to it. Implemented by the studio module; declared
 * here so tenancy doesn't depend on it.
 */
@FunctionalInterface
public interface MembershipLookup {

  /** The role row {@code accountId} holds in {@code studioId}, with its base. */
  Optional<MemberRole> roleOf(UUID accountId, UUID studioId);
}
