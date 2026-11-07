package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Set;
import java.util.UUID;

/**
 * What a member's duties grant in the bound studio (plan 21 §3, decision 6). Implemented by the
 * access module, which owns duties; declared here so tenancy doesn't depend on it.
 */
@FunctionalInterface
public interface DutyGrantLookup {

  Set<Permission> grantedTo(UUID accountId);
}
