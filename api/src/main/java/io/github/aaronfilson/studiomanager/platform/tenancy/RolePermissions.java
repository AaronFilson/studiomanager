package io.github.aaronfilson.studiomanager.platform.tenancy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * What each role holds in a studio, from its stored permission set (plan 21, lane R4).
 *
 * <ul>
 *   <li>An OWNER holds every permission, whatever is stored.
 *   <li>Any other role holds its stored set plus the self-service floor, never an owner-only
 *       permission. A studio with no stored role of that rank falls back to the code defaults.
 *   <li>A person also holds what their duties grant ({@link #resolve}).
 *   <li>Jobs don't come here: they keep their ceiling's code defaults ({@link
 *       TenantContext#system}).
 * </ul>
 *
 * <p>Stored sets are cached per studio and role for {@link #CACHE_FOR}, and dropped as soon as a
 * change on this instance commits. With more than one instance, another one's change applies within
 * that minute.
 *
 * <p>In {@linkplain PermissionSettings.Mode#SHADOW shadow mode} the code defaults decide, and a
 * stored set that differs from them is logged and counted, by role and by how many permissions
 * differ.
 */
@Component
@EnableConfigurationProperties(PermissionSettings.class)
public class RolePermissions {

  static final Duration CACHE_FOR = Duration.ofMinutes(1);

  private static final Logger log = LoggerFactory.getLogger(RolePermissions.class);

  private final StoredRolePermissions stored;
  private final DutyGrantLookup duties;
  private final PermissionSettings.Mode mode;
  private final MeterRegistry meters;
  private final Cache<StudioRoleKey, Optional<Set<Permission>>> cache;

  /**
   * Duties come from the access module. A module test that starts without it (Spring Modulith's
   * {@code @ApplicationModuleTest}) has no duties, so grants none.
   */
  @Autowired
  RolePermissions(
      StoredRolePermissions stored,
      ObjectProvider<DutyGrantLookup> duties,
      PermissionSettings settings,
      MeterRegistry meters,
      Clock clock) {
    this(stored, duties.getIfAvailable(() -> accountId -> Set.of()), settings, meters, clock);
  }

  RolePermissions(
      StoredRolePermissions stored,
      DutyGrantLookup duties,
      PermissionSettings settings,
      MeterRegistry meters,
      Clock clock) {
    this.stored = stored;
    this.duties = duties;
    this.mode = settings.mode();
    this.meters = meters;
    this.cache =
        Caffeine.newBuilder()
            .expireAfterWrite(CACHE_FOR)
            .ticker(() -> clock.millis() * 1_000_000L)
            .build();
  }

  /**
   * What {@code role} holds in the bound studio, without anyone's duties.
   *
   * @throws IllegalStateException outside any one studio
   */
  public Set<Permission> ofRole(StudioRole role) {
    if (role == StudioRole.OWNER) {
      return Collections.unmodifiableSet(EnumSet.allOf(Permission.class));
    }
    UUID studioId = TenantContext.require().requireStudioId();
    Set<Permission> defaults = Permission.defaultsFor(role);
    Set<Permission> fromStore =
        cache
            .get(new StudioRoleKey(studioId, role), key -> stored.of(key.role()))
            .map(RolePermissions::effective)
            .orElse(defaults);
    return switch (mode) {
      case ENFORCING -> fromStore;
      case SHADOW -> {
        compare(role, defaults, fromStore);
        yield defaults;
      }
    };
  }

  /** Whether {@code role} holds {@code permission} in the bound studio: for choosing people. */
  public boolean roleHolds(StudioRole role, Permission permission) {
    return ofRole(role).contains(permission);
  }

  /**
   * The person {@code caller}, bound to a studio with their role, with what they hold there: their
   * role's set and what their duties grant.
   */
  public TenantContext resolve(TenantContext caller) {
    StudioRole role = caller.role();
    if (role == null || caller.isSystem()) {
      throw new IllegalArgumentException("Resolve a person bound to a studio");
    }
    EnumSet<Permission> held = EnumSet.noneOf(Permission.class);
    held.addAll(ofRole(role));
    if (role != StudioRole.OWNER) {
      duties.grantedTo(caller.accountId()).stream()
          .filter(permission -> !permission.isOwnerOnly())
          .forEach(held::add);
    }
    return caller.withPermissions(held);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onChange(RolePermissionsChanged change) {
    forget(change.studioId());
  }

  /** Drops the studio's cached sets, so the next request reads them again. */
  public void forget(UUID studioId) {
    Arrays.stream(StudioRole.values())
        .forEach(role -> cache.invalidate(new StudioRoleKey(studioId, role)));
  }

  /**
   * What a new studio stores for each role: the code defaults without the floor, and nothing for
   * OWNER, which holds everything in code. {@code V39__studio_roles.sql} seeds the same.
   */
  public static Map<StudioRole, Set<Permission>> storedDefaults() {
    Map<StudioRole, Set<Permission>> seeded = new EnumMap<>(StudioRole.class);
    for (StudioRole role : StudioRole.values()) {
      seeded.put(
          role, role == StudioRole.OWNER ? Set.of() : withoutFloor(Permission.defaultsFor(role)));
    }
    return Collections.unmodifiableMap(seeded);
  }

  /**
   * What to store for {@code role} so that it holds {@code wanted}: without the floor, which every
   * role holds anyway.
   *
   * @throws InvalidInputException for the OWNER role, which always holds everything, or for an
   *     owner-only permission, which no other role may hold
   */
  public static Set<Permission> storableFor(StudioRole role, Set<Permission> wanted) {
    if (role == StudioRole.OWNER) {
      throw new InvalidInputException("Owners always hold every permission.");
    }
    if (wanted.stream().anyMatch(Permission::isOwnerOnly)) {
      throw new InvalidInputException("Some of those permissions are for owners only.");
    }
    return withoutFloor(wanted);
  }

  /** The roles in the order the owners' roles page lists them: OWNER first. */
  public static List<StudioRole> listed() {
    return List.of(StudioRole.OWNER, StudioRole.STAFF, StudioRole.MEMBER);
  }

  /** Whether owners may edit what {@code role} holds: every role but OWNER. */
  public static boolean editable(StudioRole role) {
    return role != StudioRole.OWNER;
  }

  /**
   * What {@code role} holds, without anyone's duties, given its stored set: everything for an
   * OWNER, the stored set as held for another role, or the code defaults when nothing is stored.
   * Unlike {@link #ofRole} this ignores shadow mode and the cache, for showing owners what they
   * set.
   */
  public static Set<Permission> held(StudioRole role, Optional<Set<Permission>> storedSet) {
    if (!editable(role)) {
      return Collections.unmodifiableSet(EnumSet.allOf(Permission.class));
    }
    return storedSet.map(RolePermissions::effective).orElse(Permission.defaultsFor(role));
  }

  /** {@code stored} as held: with the floor, which every role holds. */
  public static Set<Permission> withFloor(Set<Permission> stored) {
    EnumSet<Permission> held = EnumSet.noneOf(Permission.class);
    held.addAll(stored);
    Arrays.stream(Permission.values()).filter(Permission::isFloor).forEach(held::add);
    return Collections.unmodifiableSet(held);
  }

  private static Set<Permission> withoutFloor(Set<Permission> permissions) {
    EnumSet<Permission> stored = EnumSet.noneOf(Permission.class);
    permissions.stream().filter(permission -> !permission.isFloor()).forEach(stored::add);
    return Collections.unmodifiableSet(stored);
  }

  /** A stored set as held: the floor added, anything owner-only left out. */
  private static Set<Permission> effective(Set<Permission> storedSet) {
    EnumSet<Permission> held = EnumSet.noneOf(Permission.class);
    held.addAll(storedSet);
    held.removeIf(Permission::isOwnerOnly);
    return withFloor(held);
  }

  private void compare(StudioRole role, Set<Permission> defaults, Set<Permission> fromStore) {
    if (defaults.equals(fromStore)) {
      return;
    }
    long added = fromStore.stream().filter(permission -> !defaults.contains(permission)).count();
    long removed = defaults.stream().filter(permission -> !fromStore.contains(permission)).count();
    meters.counter("studiomanager.permissions.shadow_mismatch", "role", role.name()).increment();
    log.warn(
        "Shadow permissions: the stored {} set differs from the code defaults ({} more, {} fewer)",
        role.name(),
        added,
        removed);
  }

  private record StudioRoleKey(UUID studioId, StudioRole role) {}
}
