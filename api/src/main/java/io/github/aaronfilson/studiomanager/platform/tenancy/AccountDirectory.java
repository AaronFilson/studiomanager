package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Names and email addresses of accounts, for showing who made a piece or who is on a roster, and
 * for emailing them. Implemented by the account module; declared here so other modules don't
 * depend on it. Accounts aren't tenant data, so this works with or without a tenant bound.
 */
public interface AccountDirectory {

    /** The names of those accounts that have one. Accounts without a name are left out. */
    Map<UUID, String> displayNames(Collection<UUID> accountIds);

    /**
     * The email addresses of those accounts that have one, as the identity provider last gave
     * it. Accounts without one are left out.
     */
    Map<UUID, String> emails(Collection<UUID> accountIds);

}
