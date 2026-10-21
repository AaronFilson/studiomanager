package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Display names of accounts, for showing who made a piece or who is on a roster. Implemented
 * by the account module; declared here so other modules don't depend on it.
 */
@FunctionalInterface
public interface AccountDirectory {

    /** The names of those accounts that have one. Accounts without a name are left out. */
    Map<UUID, String> displayNames(Collection<UUID> accountIds);

}
