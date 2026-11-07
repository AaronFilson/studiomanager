package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * The bound studio's roles and what each holds (plan 21, lanes R4, R6 and R7). Seeded with the code
 * defaults when a studio is made; owners edit the STAFF and MEMBER sets, and make, rename, edit and
 * delete custom roles.
 *
 * <p>What is stored is only the editable part: OWNER holds everything in code, and the self-service
 * floor and owner-only permissions are never stored ({@code V39__studio_roles.sql}).
 */
@Service
public class StudioRoles implements StoredRolePermissions {

  /** The longest role name ({@code studio_role.name}). */
  static final int MAX_NAME_LENGTH = 60;

  private static final Logger log = LoggerFactory.getLogger(StudioRoles.class);

  /** The longest key made from a name, leaving room for a "_12" to tell two apart. */
  private static final int KEY_STEM_LENGTH = 30;

  private final StoredRoleRepository roles;
  private final StoredRolePermissionRepository rolePermissions;
  private final StudioMembershipRepository memberships;
  private final StudioInviteRepository invites;
  private final AccessChangeLog changeLog;
  private final StudioPermissions permissions;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /**
   * Reads the invite table directly, not through {@link StudioInvites}: that checks invites with
   * {@link RolePermissions}, which reads its sets from here.
   */
  StudioRoles(
      StoredRoleRepository roles,
      StoredRolePermissionRepository rolePermissions,
      StudioMembershipRepository memberships,
      StudioInviteRepository invites,
      AccessChangeLog changeLog,
      StudioPermissions permissions,
      ApplicationEventPublisher events,
      Clock clock) {
    this.roles = roles;
    this.rolePermissions = rolePermissions;
    this.memberships = memberships;
    this.invites = invites;
    this.changeLog = changeLog;
    this.permissions = permissions;
    this.events = events;
    this.clock = clock;
  }

  /**
   * The permissions stored for the system role {@code role} in the bound studio. A key this release
   * doesn't know is left out, never granted, and counted in the log.
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<Set<Permission>> of(StudioRole role) {
    return roles.findSystemRole(role).map(stored -> known(rolePermissions.findByRole(stored.id())));
  }

  /** The permissions stored for the role row {@code roleId}, system or custom. */
  @Override
  @Transactional(readOnly = true)
  public Optional<Set<Permission>> ofRoleId(UUID roleId) {
    return roles.findById(roleId).map(stored -> known(rolePermissions.findByRole(stored.id())));
  }

  /**
   * Makes the system role {@code role} hold exactly {@code wanted}, plus the floor everyone holds.
   * Only owners can ({@code studio.roles.manage}); the OWNER role and owner-only permissions can't
   * be edited. Records the change in the access log in the same transaction, and every instance's
   * cache drops the studio's sets once it commits here (others within a minute).
   *
   * @return what the role now holds
   */
  @Transactional
  public Set<Permission> setPermissions(StudioRole role, Set<Permission> wanted) {
    requireManager("Only an owner can change what a role may do.");
    Set<Permission> storable = RolePermissions.storableFor(role, wanted);
    StoredRole stored =
        roles
            .findSystemRole(role)
            .orElseThrow(() -> new NotFoundException("This studio has no such role."));
    replacePermissions(stored, storable);
    return RolePermissions.withFloor(storable);
  }

  /**
   * A role as the roles page shows it.
   *
   * @param id the role's row
   * @param key its key: {@code staff} for a system role, a slug of its name for a custom one
   * @param role its base: the system role itself, or the rank a custom role sits at
   * @param name the role's name in this studio
   * @param permissions what the role holds now, with the floor; everything for OWNER
   * @param editable false for OWNER, which always holds everything
   * @param custom true for a role the owners made, which they may rename and delete
   * @param memberCount how many members hold the role, for "3 people lose ..." before saving
   */
  public record RoleView(
      UUID id,
      String key,
      StudioRole role,
      String name,
      Set<Permission> permissions,
      boolean editable,
      boolean custom,
      int memberCount) {}

  /**
   * The bound studio's roles: the system roles, OWNER first, then the custom roles by name, each
   * with what it holds. Only owners can read them ({@code studio.roles.manage}).
   */
  @Transactional(readOnly = true)
  public List<RoleView> list(UUID studioId) {
    requireManagerIn(studioId, "Only an owner can see what each role may do.");
    List<RoleView> listed = new ArrayList<>();
    RolePermissions.listed().forEach(role -> listed.add(view(studioId, role)));
    roles.findCustomRoles().forEach(role -> listed.add(viewOf(studioId, role)));
    return listed;
  }

  /**
   * The roles the caller may put someone in: invite them as, or give them (plan 21 §7: the role's
   * permissions must all be the caller's). Any member with {@code members.invite} or {@code
   * members.role.assign} may ask; the answer is only names.
   */
  @Transactional(readOnly = true)
  public List<RoleView> assignable(UUID studioId) {
    TenantContext caller = TenantContext.require();
    requireBoundTo(caller, studioId);
    List<StoredRole> every = new ArrayList<>();
    RolePermissions.listed().forEach(role -> roles.findSystemRole(role).ifPresent(every::add));
    every.addAll(roles.findCustomRoles());
    return every.stream()
        .map(role -> viewOf(studioId, role))
        .filter(role -> caller.permissions().containsAll(role.permissions()))
        .toList();
  }

  /** The changes to the bound studio's access, newest first. Only owners can read them. */
  @Transactional(readOnly = true)
  public Capped<AccessChangeLog.Entry> history(UUID studioId) {
    requireManagerIn(studioId, "Only an owner can read the access log.");
    return changeLog.recent();
  }

  /** Makes {@code role} hold its code defaults again, as {@link #setPermissions} would. */
  @Transactional
  public Set<Permission> resetPermissions(StudioRole role) {
    return setPermissions(role, Permission.defaultsFor(role));
  }

  /**
   * Makes a custom role (lane R7): a name, a base of STAFF or MEMBER, and what it holds. Only
   * owners can; owner-only permissions are refused. Logged as made, and with its permissions.
   *
   * @throws InvalidInputException for an OWNER base, an owner-only permission, or a bad name
   * @throws ConflictException when the studio has a role of that name
   */
  @Transactional
  public RoleView createCustom(
      UUID studioId, String name, StudioRole base, Set<Permission> wanted) {
    requireManagerIn(studioId, "Only an owner can make a role.");
    if (!RolePermissions.mayBaseACustomRole(base)) {
      throw new InvalidInputException(
          "A role sits at staff or member. Make someone an owner with the owner controls.");
    }
    Set<Permission> storable = RolePermissions.storableFor(base, wanted);
    String cleanName = validName(name);
    if (roles.existsByNameIgnoringCase(cleanName)) {
      throw ConflictException.duplicate("The studio already has a role called that.");
    }
    StoredRole role =
        roles.saveAndFlush(StoredRole.custom(freeKey(cleanName), cleanName, base, clock.instant()));
    changeLog.record(AccessChangeLog.Kind.ROLE_CREATED, role.key(), List.of(), List.of(cleanName));
    replacePermissions(role, storable);
    return viewOf(studioId, role);
  }

  /**
   * Renames a custom role and sets what it holds, as {@link #setPermissions} does for a system
   * role. Only owners can. Its base can't change: make another role instead.
   */
  @Transactional
  public RoleView updateCustom(UUID studioId, UUID roleId, String name, Set<Permission> wanted) {
    requireManagerIn(studioId, "Only an owner can change a role.");
    StoredRole role = customRole(roleId);
    Set<Permission> storable = RolePermissions.storableFor(role.base(), wanted);
    String cleanName = validName(name);
    String before = role.name();
    if (!before.equals(cleanName)) {
      if (!before.equalsIgnoreCase(cleanName) && roles.existsByNameIgnoringCase(cleanName)) {
        throw ConflictException.duplicate("The studio already has a role called that.");
      }
      role.rename(cleanName, clock.instant());
      roles.flush();
      changeLog.record(
          AccessChangeLog.Kind.ROLE_RENAMED, role.key(), List.of(before), List.of(cleanName));
    }
    replacePermissions(role, storable);
    return viewOf(studioId, role);
  }

  /**
   * Deletes a custom role nobody holds, withdrawing any open invites into it. Only owners can. A
   * role someone holds is refused: give them another role first (plan 21 §7), so nobody's access
   * changes as a side effect.
   *
   * @throws ConflictException while a member holds it
   */
  @Transactional
  public void deleteCustom(UUID studioId, UUID roleId) {
    requireManagerIn(studioId, "Only an owner can delete a role.");
    StoredRole role = customRole(roleId);
    int holders = memberships.countByStudioIdAndRoleId(studioId, roleId);
    if (holders > 0) {
      throw new ConflictException(
          "%s with this role. Give %s another role first."
              .formatted(
                  holders == 1 ? "1 person has" : holders + " people have",
                  holders == 1 ? "them" : "each of them"));
    }
    invites.deleteAll(invites.findOpenTo(roleId));
    changeLog.record(
        AccessChangeLog.Kind.ROLE_DELETED, role.key(), List.of(role.name()), List.of());
    roles.delete(role);
    roles.flush();
    events.publishEvent(new RolePermissionsChanged(studioId));
  }

  /** Every role's name in the bound studio, by row, for showing invites and members. */
  @Transactional(readOnly = true)
  public Map<UUID, String> namesById() {
    return roles.findAll().stream().collect(Collectors.toMap(StoredRole::id, StoredRole::name));
  }

  /**
   * The system role {@code role} in the bound studio {@code studioId}, as the roles page shows it.
   */
  @Transactional(readOnly = true)
  RoleView view(UUID studioId, StudioRole role) {
    StoredRole stored =
        roles
            .findSystemRole(role)
            .orElseThrow(() -> new NotFoundException("This studio has no such role."));
    return viewOf(studioId, stored);
  }

  /**
   * The role row {@code roleId} in the bound studio {@code studioId}, as the roles page shows it.
   */
  @Transactional(readOnly = true)
  RoleView view(UUID studioId, UUID roleId) {
    return viewOf(
        studioId, roles.findById(roleId).orElseThrow(() -> new NotFoundException("No such role.")));
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
    roles.flush();
  }

  private RoleView viewOf(UUID studioId, StoredRole stored) {
    Set<Permission> storedSet = known(rolePermissions.findByRole(stored.id()));
    // A custom role is never OWNER-based, so this is its stored set with the floor
    Set<Permission> held = RolePermissions.held(stored.base(), Optional.of(storedSet));
    return new RoleView(
        stored.id(),
        stored.key(),
        stored.base(),
        stored.name(),
        held,
        !stored.isSystem() || RolePermissions.editable(stored.base()),
        !stored.isSystem(),
        memberships.countByStudioIdAndRoleId(studioId, stored.id()));
  }

  /** Stores exactly {@code storable} for {@code stored}, logs the change and drops the cache. */
  private void replacePermissions(StoredRole stored, Set<Permission> storable) {
    List<StoredRolePermission> current = rolePermissions.findByRole(stored.id());
    Set<String> before = keys(current);
    Set<String> after = keys(storable);
    for (StoredRolePermission permission : current) {
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
  }

  private StoredRole customRole(UUID roleId) {
    StoredRole role =
        roles.findById(roleId).orElseThrow(() -> new NotFoundException("No such role."));
    if (role.isSystem()) {
      throw new InvalidInputException(
          "The built-in roles can't be renamed or deleted; change what they may do instead.");
    }
    return role;
  }

  private TenantContext requireManager(String message) {
    return permissions.require(Permission.STUDIO_ROLES_MANAGE, message);
  }

  private void requireManagerIn(UUID studioId, String message) {
    requireBoundTo(requireManager(message), studioId);
  }

  private static void requireBoundTo(TenantContext caller, UUID studioId) {
    if (!studioId.equals(caller.studioId())) {
      throw new IllegalStateException("The request is not bound to studio " + studioId);
    }
  }

  /** A role's name, trimmed: 1 to 60 characters, no commas (the access log lists names). */
  private static String validName(String name) {
    String trimmed = name.strip();
    if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
      throw new InvalidInputException(
          "A role's name is 1 to %d characters.".formatted(MAX_NAME_LENGTH));
    }
    if (trimmed.contains(",")) {
      throw new InvalidInputException("A role's name can't contain a comma.");
    }
    return trimmed;
  }

  /**
   * A key for a new role made from its name, such as {@code kiln_tech}, not used in the studio yet:
   * never one of the system keys, with a number added when needed.
   */
  private String freeKey(String name) {
    String stem =
        name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    if (stem.isEmpty() || !Character.isLetter(stem.charAt(0))) {
      stem = "role_" + stem;
    }
    if (stem.length() > KEY_STEM_LENGTH) {
      stem = stem.substring(0, KEY_STEM_LENGTH).replaceAll("_+$", "");
    }
    String key = stem;
    for (int suffix = 2; roles.existsByKey(key) || isSystemKey(key); suffix++) {
      key = stem + "_" + suffix;
    }
    return key;
  }

  private static boolean isSystemKey(String key) {
    return RolePermissions.listed().stream()
        .anyMatch(role -> role.name().toLowerCase(Locale.ROOT).equals(key));
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
