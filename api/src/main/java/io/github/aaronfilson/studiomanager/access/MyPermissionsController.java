package io.github.aaronfilson.studiomanager.access;

import io.github.aaronfilson.studiomanager.contract.DutyDto;
import io.github.aaronfilson.studiomanager.contract.MyPermissionsDto;
import io.github.aaronfilson.studiomanager.contract.PermissionsApi;
import io.github.aaronfilson.studiomanager.contract.StudioRoleDto;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the caller may do in a studio (plan 21 §5), so the web and mobile apps can ask {@code
 * can('pieces.move')} instead of comparing role names. Hiding is convenience; the API still checks.
 */
@RestController
class MyPermissionsController implements PermissionsApi {

  private final AccessService access;

  MyPermissionsController(AccessService access) {
    this.access = access;
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_READ')")
  public ResponseEntity<MyPermissionsDto> getMyPermissions(UUID studioId) {
    TenantContext context = TenantContext.require();
    String roleKey = Objects.requireNonNull(context.role(), "role").name();
    return ResponseEntity.ok(
        new MyPermissionsDto()
            .role(StudioRoleDto.valueOf(roleKey))
            .roleName(displayName(roleKey))
            .permissions(context.permissions().stream().map(Permission::key).sorted().toList())
            .duties(
                access.dutiesOf(context.accountId()).stream()
                    .map(duty -> DutyDto.valueOf(duty.name()))
                    .toList()));
  }

  /** {@code STAFF} as {@code Staff}, until R6 lets owners name roles. */
  private static String displayName(String roleKey) {
    return roleKey.charAt(0) + roleKey.substring(1).toLowerCase(Locale.ROOT);
  }
}
