package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * One thing a person may do in a studio: the permission catalogue of plan 21 §4 and ADR 25.
 *
 * <p>The catalogue is code, not data. A studio's owners choose which of these each role holds; they
 * never invent new ones. Each constant carries its stable {@linkplain #key() key}, the name stored
 * in the database and sent to the apps, and its {@linkplain #defaultGrant() default grant}, which
 * reproduces the role checks the API made before permissions existed.
 *
 * <p>An OWNER always holds every permission. {@link Grant#OWNER_ONLY} permissions can never be held
 * by any other role (plan 21, decision 5).
 */
public enum Permission {
  STUDIO_READ("studio.read", Grant.MEMBER),
  STUDIO_SETTINGS_EDIT("studio.settings.edit", Grant.STAFF),
  STUDIO_EXPORT("studio.export", Grant.OWNER_ONLY),
  STUDIO_AI_KEY_MANAGE("studio.ai_key.manage", Grant.OWNER_ONLY),
  STUDIO_ROLES_MANAGE("studio.roles.manage", Grant.OWNER_ONLY),

  MEMBERS_READ("members.read", Grant.STAFF),
  MEMBERS_INVITE("members.invite", Grant.STAFF),
  MEMBERS_ROLE_ASSIGN("members.role.assign", Grant.STAFF),
  MEMBERS_REMOVE("members.remove", Grant.STAFF),
  MEMBERS_DUTIES_MANAGE("members.duties.manage", Grant.STAFF),
  MEMBERS_CERTIFY("members.certify", Grant.STAFF),

  PIECES_INTAKE_OWN("pieces.intake_own", Grant.MEMBER),
  PIECES_READ("pieces.read", Grant.MEMBER),
  PIECES_INTAKE_FOR_OTHERS("pieces.intake_for_others", Grant.STAFF),
  PIECES_MOVE("pieces.move", Grant.STAFF),
  PIECES_POLICY_EDIT("pieces.policy.edit", Grant.STAFF),

  KILNS_READ("kilns.read", Grant.MEMBER),
  KILNS_MANAGE("kilns.manage", Grant.STAFF),
  FIRINGS_MANAGE("firings.manage", Grant.STAFF),

  FEES_READ("fees.read", Grant.MEMBER),
  FEES_EDIT("fees.edit", Grant.STAFF),

  MAINTENANCE_READ("maintenance.read", Grant.MEMBER),
  MAINTENANCE_REPORT_PROBLEM("maintenance.report_problem", Grant.MEMBER),
  MAINTENANCE_MANAGE("maintenance.manage", Grant.STAFF),
  MAINTENANCE_COMPLETE_ANY("maintenance.complete_any", Grant.STAFF),
  MAINTENANCE_COMPLETE_ASSIGNED("maintenance.complete_assigned", Grant.DUTY_ONLY),
  MAINTENANCE_REPORTS_RESOLVE("maintenance.reports.resolve", Grant.STAFF),

  SCHEDULE_READ("schedule.read", Grant.MEMBER),
  SCHEDULE_BOOK_OWN("schedule.book_own", Grant.MEMBER),
  SCHEDULE_SHIFTS_TAKE_OPEN("schedule.shifts.take_open", Grant.MEMBER),
  SCHEDULE_SHIFTS_TAKE_DUTY("schedule.shifts.take_duty", Grant.DUTY_ONLY),
  SCHEDULE_HOURS_LOG_OWN("schedule.hours.log_own", Grant.DUTY_ONLY),
  SCHEDULE_MANAGE("schedule.manage", Grant.STAFF),
  SCHEDULE_HOURS_MANAGE("schedule.hours.manage", Grant.STAFF),
  SCHEDULE_BOOK_FOR_OTHERS("schedule.book_for_others", Grant.STAFF),

  SUPPLIES_READ("supplies.read", Grant.MEMBER),
  SUPPLIES_MANAGE("supplies.manage", Grant.STAFF),
  SUPPLIES_SELL("supplies.sell", Grant.STAFF),
  SUPPLIES_PURCHASE_REQUEST("supplies.purchase.request", Grant.STAFF),
  SUPPLIES_PURCHASE_APPROVE("supplies.purchase.approve", Grant.OWNER_ONLY),
  INVOICES_MANAGE("invoices.manage", Grant.STAFF),

  GLAZES_READ("glazes.read", Grant.MEMBER),
  GLAZES_OWN_EDIT("glazes.own.edit", Grant.MEMBER),
  GLAZES_STUDIO_EDIT("glazes.studio.edit", Grant.STAFF),

  MONEY_OWN_READ("money.own.read", Grant.FLOOR),
  MONEY_READ("money.read", Grant.STAFF),
  MONEY_TAKE_PAYMENT("money.take_payment", Grant.STAFF),
  MONEY_STORED_VALUE_MANAGE("money.stored_value.manage", Grant.STAFF),
  MONEY_REFUND_AND_ADJUST("money.refund_and_adjust", Grant.STAFF),
  MONEY_PAYOUTS("money.payouts", Grant.STAFF),
  MONEY_PAYABLES_MANAGE("money.payables.manage", Grant.STAFF),
  MONEY_TAX_RATES_EDIT("money.tax_rates.edit", Grant.STAFF),

  NOTICES_READ("notices.read", Grant.MEMBER),
  NOTICES_SEND("notices.send", Grant.STAFF),

  TODAY_READ("today.read", Grant.STAFF),
  ACTIVITY_READ("activity.read", Grant.STAFF),

  IMPORTS_RUN("imports.run", Grant.STAFF),
  WEBHOOKS_MANAGE("webhooks.manage", Grant.STAFF),
  API_TOKENS_OWN("api_tokens.own", Grant.STAFF),
  API_TOKENS_MANAGE_ALL("api_tokens.manage_all", Grant.OWNER_ONLY),
  MCP_USE("mcp.use", Grant.STAFF),
  PROPOSALS_CREATE("proposals.create", Grant.STAFF),
  PROPOSALS_REVIEW("proposals.review", Grant.STAFF),

  OWNERS_MANAGE("owners.manage", Grant.OWNER_ONLY);

  /** Who holds a permission before a studio's owners edit anything. */
  public enum Grant {
    /**
     * Every role, always, and not editable: the self-service floor (plan 21, decision 3). The
     * action still checks the row is the caller's own.
     */
    FLOOR,
    /** MEMBER and above by default; owners may take it away from MEMBER or STAFF. */
    MEMBER,
    /** STAFF by default; owners may give it to MEMBER or take it from STAFF. */
    STAFF,
    /**
     * No role but OWNER by default; a duty grants it (plan 21, decision 6). Owners may still add it
     * to a role.
     */
    DUTY_ONLY,
    /** OWNER alone, never grantable to another role (plan 21, decision 5). */
    OWNER_ONLY
  }

  private static final Map<String, Permission> BY_KEY =
      Arrays.stream(values())
          .collect(Collectors.toUnmodifiableMap(Permission::key, Function.identity()));

  private final String key;
  private final Grant defaultGrant;

  Permission(String key, Grant defaultGrant) {
    this.key = key;
    this.defaultGrant = defaultGrant;
  }

  /** The stable name, such as {@code pieces.move}, stored in the database and sent to the apps. */
  public String key() {
    return key;
  }

  public Grant defaultGrant() {
    return defaultGrant;
  }

  /** Whether only an OWNER may ever hold this. */
  public boolean isOwnerOnly() {
    return defaultGrant == Grant.OWNER_ONLY;
  }

  /** Whether every role always holds this, whatever the owners edit. */
  public boolean isFloor() {
    return defaultGrant == Grant.FLOOR;
  }

  /**
   * The permission with this key. A key the code doesn't know, from an older or newer release, is
   * empty, so the caller can ignore it rather than grant it (plan 21 §7).
   */
  public static Optional<Permission> byKey(String key) {
    return Optional.ofNullable(BY_KEY.get(key));
  }

  /** The owner-only permissions, which no role but OWNER may hold. */
  public static Set<Permission> ownerOnly() {
    return Collections.unmodifiableSet(
        EnumSet.copyOf(Arrays.stream(values()).filter(Permission::isOwnerOnly).toList()));
  }

  /**
   * What {@code role} holds before a studio's owners edit anything. These reproduce the role checks
   * one for one, and they are the fixed ceiling for background jobs (plan 21 §7).
   */
  public static Set<Permission> defaultsFor(StudioRole role) {
    EnumSet<Permission> granted = EnumSet.noneOf(Permission.class);
    for (Permission permission : values()) {
      if (grantedByDefault(permission.defaultGrant, role)) {
        granted.add(permission);
      }
    }
    return Collections.unmodifiableSet(granted);
  }

  private static boolean grantedByDefault(Grant grant, StudioRole role) {
    return switch (grant) {
      case FLOOR -> true;
      case MEMBER -> role.includes(StudioRole.MEMBER);
      case STAFF -> role.includes(StudioRole.STAFF);
      case DUTY_ONLY -> role.includes(StudioRole.OWNER);
      case OWNER_ONLY -> role.includes(StudioRole.OWNER);
    };
  }
}
