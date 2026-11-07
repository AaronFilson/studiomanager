package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.contract.AccessChangeDto;
import io.github.aaronfilson.studiomanager.contract.AccessChangeKindDto;
import io.github.aaronfilson.studiomanager.contract.AccessChangeListDto;
import io.github.aaronfilson.studiomanager.contract.CreateCustomRoleRequestDto;
import io.github.aaronfilson.studiomanager.contract.PermissionAreaDto;
import io.github.aaronfilson.studiomanager.contract.PermissionInfoDto;
import io.github.aaronfilson.studiomanager.contract.PermissionLockDto;
import io.github.aaronfilson.studiomanager.contract.RoleCatalogueDto;
import io.github.aaronfilson.studiomanager.contract.RoleDto;
import io.github.aaronfilson.studiomanager.contract.RolesApi;
import io.github.aaronfilson.studiomanager.contract.SetRolePermissionsRequestDto;
import io.github.aaronfilson.studiomanager.contract.StudioRoleDto;
import io.github.aaronfilson.studiomanager.contract.UpdateCustomRoleRequestDto;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.PermissionCatalogue;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.net.URI;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** The owners' roles page: what each role may do, and the access log (plan 21, lane R6). */
@RestController
class RoleController implements RolesApi {

  private final StudioRoles roles;
  private final StudioMembers members;

  RoleController(StudioRoles roles, StudioMembers members) {
    this.roles = roles;
    this.members = members;
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_ROLES_MANAGE')")
  public ResponseEntity<RoleCatalogueDto> listRoles(UUID studioId) {
    List<RoleDto> listed = roles.list(studioId).stream().map(RoleController::toDto).toList();
    return ResponseEntity.ok(new RoleCatalogueDto().areas(catalogue()).roles(listed));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_ROLES_MANAGE')")
  public ResponseEntity<RoleDto> setRolePermissions(
      UUID studioId, StudioRoleDto role, SetRolePermissionsRequestDto request) {
    StudioRole chosen = StudioRole.valueOf(role.name());
    roles.setPermissions(chosen, known(request.getPermissions()));
    return ResponseEntity.ok(toDto(roles.view(studioId, chosen)));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_ROLES_MANAGE')")
  public ResponseEntity<RoleDto> resetRolePermissions(UUID studioId, StudioRoleDto role) {
    StudioRole chosen = StudioRole.valueOf(role.name());
    roles.resetPermissions(chosen);
    return ResponseEntity.ok(toDto(roles.view(studioId, chosen)));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'MEMBERS_INVITE')")
  public ResponseEntity<List<RoleDto>> listAssignableRoles(UUID studioId) {
    return ResponseEntity.ok(
        roles.assignable(studioId).stream().map(RoleController::toDto).toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_ROLES_MANAGE')")
  public ResponseEntity<RoleDto> createCustomRole(
      UUID studioId, CreateCustomRoleRequestDto request) {
    StudioRoles.RoleView made =
        roles.createCustom(
            studioId,
            request.getName(),
            StudioRole.valueOf(request.getBase().name()),
            known(request.getPermissions()));
    return ResponseEntity.created(
            URI.create("/api/v1/studios/" + studioId + "/roles/custom/" + made.id()))
        .body(toDto(made));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_ROLES_MANAGE')")
  public ResponseEntity<RoleDto> updateCustomRole(
      UUID studioId, UUID roleId, UpdateCustomRoleRequestDto request) {
    return ResponseEntity.ok(
        toDto(
            roles.updateCustom(
                studioId, roleId, request.getName(), known(request.getPermissions()))));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_ROLES_MANAGE')")
  public ResponseEntity<Void> deleteCustomRole(UUID studioId, UUID roleId) {
    roles.deleteCustom(studioId, roleId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_ROLES_MANAGE')")
  public ResponseEntity<AccessChangeListDto> listAccessChanges(UUID studioId) {
    Capped<AccessChangeLog.Entry> history = roles.history(studioId);
    Map<String, String> names = new HashMap<>();
    for (MemberView member : members.list()) {
      String displayName = member.displayName();
      if (displayName != null) {
        names.put(member.accountId().toString(), displayName);
      }
    }
    Capped<AccessChangeDto> listed = history.map(entry -> toDto(entry, names));
    return ResponseEntity.ok(
        new AccessChangeListDto()
            .items(listed.items())
            .truncated(listed.truncated())
            .limit(listed.limit()));
  }

  /** Every permission, grouped by area in the catalogue's order. */
  private static List<PermissionAreaDto> catalogue() {
    return Arrays.stream(PermissionCatalogue.Area.values())
        .map(area ->
            new PermissionAreaDto()
                .area(area.name())
                .label(area.label())
                .permissions(
                    Arrays.stream(Permission.values())
                        .filter(
                            permission -> PermissionCatalogue.describe(permission).area() == area)
                        .map(RoleController::toDto)
                        .toList()))
        .filter(area -> !area.getPermissions().isEmpty())
        .toList();
  }

  private static PermissionInfoDto toDto(Permission permission) {
    PermissionCatalogue.Description description = PermissionCatalogue.describe(permission);
    return new PermissionInfoDto()
        .key(permission.key())
        .label(description.label())
        .description(description.description())
        .lock(lockOf(permission));
  }

  private static @Nullable PermissionLockDto lockOf(Permission permission) {
    return switch (permission.defaultGrant()) {
      case OWNER_ONLY -> PermissionLockDto.OWNER_ONLY;
      case FLOOR -> PermissionLockDto.FLOOR;
      case MEMBER -> null;
      case STAFF -> null;
      case DUTY_ONLY -> null;
    };
  }

  private static RoleDto toDto(StudioRoles.RoleView role) {
    return new RoleDto()
        .id(role.id())
        .key(role.key())
        .role(StudioRoleDto.valueOf(role.role().name()))
        .name(role.name())
        .permissions(role.permissions().stream().map(Permission::key).sorted().toList())
        .editable(role.editable())
        .custom(role.custom())
        .memberCount(role.memberCount());
  }

  private static AccessChangeDto toDto(AccessChangeLog.Entry entry, Map<String, String> names) {
    boolean aboutAMember =
        switch (entry.kind()) {
          case MEMBER_DUTIES -> true;
          case MEMBER_ROLE -> true;
          case ROLE_PERMISSIONS -> false;
          case BILLING_CONTACT -> false;
          case ROLE_CREATED -> false;
          case ROLE_RENAMED -> false;
          case ROLE_DELETED -> false;
        };
    return new AccessChangeDto()
        .id(entry.id())
        .at(entry.at().atOffset(ZoneOffset.UTC))
        .actorAccountId(entry.actorAccountId())
        .actorName(names.get(entry.actorAccountId().toString()))
        .viaToken(entry.viaTokenId() != null)
        .kind(AccessChangeKindDto.valueOf(entry.kind().name()))
        .target(entry.target())
        .targetName(aboutAMember ? names.get(entry.target()) : null)
        .added(entry.added())
        .removed(entry.removed());
  }

  /** The permissions named by {@code keys}; a key this release doesn't know is refused. */
  private static Set<Permission> known(List<String> keys) {
    EnumSet<Permission> chosen = EnumSet.noneOf(Permission.class);
    for (String key : keys) {
      chosen.add(
          Permission.byKey(key)
              .orElseThrow(() -> new InvalidInputException("That isn't a known permission.")));
    }
    return chosen;
  }
}
