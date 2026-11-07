package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Optional;
import java.util.Set;

/**
 * A role's stored permission set in the bound studio (plan 21, lane R4). Implemented by the studio
 * module, which owns the tables; declared here so tenancy doesn't depend on it.
 */
@FunctionalInterface
public interface StoredRolePermissions {

  /**
   * The permissions stored for {@code role} in the bound studio, or empty when the studio has no
   * stored role of that rank. Keys the code doesn't know are left out.
   */
  Optional<Set<Permission>> of(StudioRole role);
}
