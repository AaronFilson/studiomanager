package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Method-security checks against the bound tenant, for use in annotations such as
 * {@code @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")}.
 *
 * <p>{@link TenantContextFilter} already refuses non-members; checking again at the method keeps
 * each endpoint's requirement visible where it is declared.
 */
@Component("studioAuthz")
public class StudioAuthorization {

  private final StudioPermissions permissions;

  public StudioAuthorization(StudioPermissions permissions) {
    this.permissions = permissions;
  }

  /**
   * Whether the caller holds {@code permission} in {@code studioId}, for
   * {@code @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_MOVE')")} (ADR 25).
   */
  public boolean can(UUID studioId, Permission permission) {
    return permissions.has(studioId, permission);
  }

  public boolean isMember(UUID studioId) {
    return hasRole(studioId, StudioRole.MEMBER);
  }

  public boolean hasRole(UUID studioId, StudioRole minimum) {
    TenantContext context = TenantContext.current().orElse(null);
    if (context == null || !studioId.equals(context.studioId())) {
      return false;
    }
    StudioRole role = context.role();
    return role != null && role.includes(minimum);
  }
}
