package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A role's stored permission set in the bound studio (plan 21, lanes R4 and R7). Implemented by the
 * studio module, which owns the tables; declared here so tenancy doesn't depend on it.
 */
public interface StoredRolePermissions {

  /**
   * The permissions stored for the system role {@code role} in the bound studio, or empty when the
   * studio has no stored role of that rank. Keys the code doesn't know are left out.
   */
  Optional<Set<Permission>> of(StudioRole role);

  /**
   * The permissions stored for the role row {@code roleId} in the bound studio, system or custom,
   * or empty when there is no such row. Keys the code doesn't know are left out.
   */
  Optional<Set<Permission>> ofRoleId(UUID roleId);
}
