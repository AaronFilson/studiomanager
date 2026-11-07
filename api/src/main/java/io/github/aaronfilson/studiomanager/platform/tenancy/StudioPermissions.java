package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * The one place a permission is checked (ADR 25). It reads the bound {@link TenantContext}, whose
 * permissions were resolved once for the request.
 *
 * <p>Controllers check through {@code @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_MOVE')")}.
 * Services call {@link #require} only where the answer depends on the row, such as someone else's
 * piece rather than your own.
 */
@Component
public class StudioPermissions {

  /** Whether the caller holds {@code permission} in the studio the call is bound to. */
  public boolean has(Permission permission) {
    return TenantContext.current().map(context -> context.holds(permission)).orElse(false);
  }

  /**
   * Whether the caller holds {@code permission} in {@code studioId}, and the call is bound there.
   */
  public boolean has(UUID studioId, Permission permission) {
    return TenantContext.current()
        .filter(context -> studioId.equals(context.studioId()))
        .map(context -> context.holds(permission))
        .orElse(false);
  }

  /**
   * Refuses the call unless the caller holds {@code permission}.
   *
   * @throws AccessDeniedException when they don't, which the API answers with a 403
   */
  public void require(Permission permission) {
    if (!has(permission)) {
      throw new AccessDeniedException("You don't have permission to do that in this studio.");
    }
  }
}
