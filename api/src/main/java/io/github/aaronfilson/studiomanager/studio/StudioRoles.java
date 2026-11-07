package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.RolePermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.RolePermissionsChanged;
import io.github.aaronfilson.studiomanager.platform.tenancy.StoredRolePermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The bound studio's roles and what each holds (plan 21, lane R4). Seeded with the code defaults
 * when a studio is made; R6 gives owners the page that calls {@link #setPermissions}.
 *
 * <p>What is stored is only the editable part: OWNER holds everything in code, and the self-service
 * floor and owner-only permissions are never stored ({@code V39__studio_roles.sql}).
 */
@Service
public class StudioRoles implements StoredRolePermissions {

  private static final Logger log = LoggerFactory.getLogger(StudioRoles.class);

  private final StoredRoleRepository roles;
  private final StoredRolePermissionRepository rolePermissions;
  private final StudioMembershipRepository memberships;
  private final AccessChangeLog changeLog;
  private final StudioPermissions permissions;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  StudioRoles(
      StoredRoleRepository roles,
      StoredRolePermissionRepository rolePermissions,
      StudioMembershipRepository memberships,
      AccessChangeLog changeLog,
      StudioPermissions permissions,
      ApplicationEventPublisher events,
      Clock clock) {
    this.roles = roles;
    this.rolePermissions = rolePermissions;
    this.memberships = memberships;
    this.changeLog = changeLog;
    this.permissions = permissions;
    this.events = events;
    this.clock = clock;
  }

  /**
   * The permissions stored for {@code role} in the bound studio. A key this release doesn't know is
   * left out, never granted, and counted in the log.
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<Set<Permission>> of(StudioRole role) {
    return roles.findSystemRole(role).map(stored -> known(rolePermissions.findByRole(stored.id())));
  }

  /**
   * Makes {@code role} hold exactly {@code wanted}, plus the floor everyone holds. Only owners can
   * ({@code studio.roles.manage}); the OWNER role and owner-only permissions can't be edited.
   * Records the change in the access log in the same transaction, and every instance's cache drops
   * the studio's sets once it commits here (others within a minute).
   *
   * @return what the role now holds
   */
  @Transactional
  public Set<Permission> setPermissions(StudioRole role, Set<Permission> wanted) {
    permissions.require(
        Permission.STUDIO_ROLES_MANAGE, "Only an owner can change what a role may do.");
    Set<Permission> storable = RolePermissions.storableFor(role, wanted);
    StoredRole stored =
        roles
            .findSystemRole(role)
            .orElseThrow(() -> new NotFoundException("This studio has no such role."));
    List<StoredRolePermission> held = rolePermissions.findByRole(stored.id());
    Set<String> before = keys(held);
    Set<String> after = keys(storable);
    for (StoredRolePermission permission : held) {
      if (!after.contains(permission.permission())) {
        rolePermissions.delete(permission);
      }
    }
    for (String key : after) {
      if (!before.contains(key)) {
        rolePermissions.save(new StoredRolePermission(stored.id(), key));
      }
    }
    stored.touch(clock.instant());
    rolePermissions.flush();
    changeLog.record(AccessChangeLog.Kind.ROLE_PERMISSIONS, stored.key(), before, after);
    events.publishEvent(new RolePermissionsChanged(TenantContext.require().requireStudioId()));
    return RolePermissions.withFloor(storable);
  }

  /**
   * A role as the roles page shows it.
   *
   * @param name the role's name in this studio
   * @param permissions what the role holds now, with the floor; everything for OWNER
   * @param editable false for OWNER, which always holds everything
   * @param memberCount how many members hold the role, for "3 people lose ..." before saving
   */
  public record RoleView(
      StudioRole role,
      String name,
      Set<Permission> permissions,
      boolean editable,
      int memberCount) {}

  /**
   * The bound studio's roles, OWNER first, each with what it holds. Only owners can read them
   * ({@code studio.roles.manage}).
   */
  @Transactional(readOnly = true)
  public List<RoleView> list(UUID studioId) {
    TenantContext caller =
        permissions.require(
            Permission.STUDIO_ROLES_MANAGE, "Only an owner can see what each role may do.");
    if (!studioId.equals(caller.studioId())) {
      throw new IllegalStateException("The request is not bound to studio " + studioId);
    }
    return RolePermissions.listed().stream().map(role -> view(studioId, role)).toList();
  }

  /** The changes to the bound studio's access, newest first. Only owners can read them. */
  @Transactional(readOnly = true)
  public Capped<AccessChangeLog.Entry> history(UUID studioId) {
    TenantContext caller =
        permissions.require(
            Permission.STUDIO_ROLES_MANAGE, "Only an owner can read the access log.");
    if (!studioId.equals(caller.studioId())) {
      throw new IllegalStateException("The request is not bound to studio " + studioId);
    }
    return changeLog.recent();
  }

  /** Makes {@code role} hold its code defaults again, as {@link #setPermissions} would. */
  @Transactional
  public Set<Permission> resetPermissions(StudioRole role) {
    return setPermissions(role, Permission.defaultsFor(role));
  }

  /** {@code role} in the bound studio {@code studioId}, as the roles page shows it. */
  @Transactional(readOnly = true)
  RoleView view(UUID studioId, StudioRole role) {
    Optional<StoredRole> stored = roles.findSystemRole(role);
    String name = stored.map(StoredRole::name).orElse(role.name());
    Set<Permission> held =
        RolePermissions.held(
            role, stored.map(found -> known(rolePermissions.findByRole(found.id()))));
    return new RoleView(
        role,
        name,
        held,
        RolePermissions.editable(role),
        memberships.countByStudioIdAndRole(studioId, role));
  }

  /**
   * Gives the bound studio, just made, its three system roles holding the code defaults. Runs in
   * the transaction that makes the studio.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  void seed(Instant now) {
    RolePermissions.storedDefaults()
        .forEach((base, defaults) -> {
          StoredRole role = roles.save(StoredRole.system(base, now));
          defaults.forEach(permission ->
              rolePermissions.save(new StoredRolePermission(role.id(), permission.key())));
        });
  }

  private static Set<String> keys(Set<Permission> permissions) {
    return permissions.stream().map(Permission::key).collect(Collectors.toSet());
  }

  private static Set<String> keys(List<StoredRolePermission> stored) {
    return stored.stream().map(StoredRolePermission::permission).collect(Collectors.toSet());
  }

  private static Set<Permission> known(List<StoredRolePermission> stored) {
    EnumSet<Permission> held = EnumSet.noneOf(Permission.class);
    int unknown = 0;
    for (StoredRolePermission permission : stored) {
      Optional<Permission> found = Permission.byKey(permission.permission());
      if (found.isPresent()) {
        held.add(found.get());
      } else {
        unknown++;
      }
    }
    if (unknown > 0) {
      log.warn("Ignored {} stored permissions this release doesn't know", unknown);
    }
    return held;
  }
}
