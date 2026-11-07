package io.github.aaronfilson.studiomanager.apitoken;

import java.util.UUID;

/**
 * A personal API token was revoked. Its owner is emailed, so a revoke they didn't make doesn't go
 * unnoticed (plan 17, step 2).
 */
public record ApiTokenRevoked(
    UUID tokenId, UUID studioId, UUID ownerAccountId, String tokenName, UUID revokedByAccountId) {}
