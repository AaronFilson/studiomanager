package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.UUID;

/**
 * A studio's stored permission sets changed. Published inside the changing transaction; {@link
 * RolePermissions} drops its cached sets for the studio once it commits.
 */
public record RolePermissionsChanged(UUID studioId) {}
