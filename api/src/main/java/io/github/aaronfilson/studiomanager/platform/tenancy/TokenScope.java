package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * What a personal API token may do. Reading and each area's writes are separate.
 *
 * <p>Each scope allows a fixed set of {@linkplain #permissions() permissions}, and a token holds
 * only what its person holds and its scopes allow (plan 21 §5, lane R5). {@link TokenPolicy} still
 * decides which areas and methods a token reaches at all; the permissions cap what it does there.
 */
public enum TokenScope {
  READ("read"),
  WRITE_PIECES("write:pieces"),
  WRITE_KILNS("write:kilns"),
  WRITE_MAINTENANCE("write:maintenance"),
  WRITE_SCHEDULE("write:schedule"),
  WRITE_SUPPLIES("write:supplies"),
  WRITE_GLAZES("write:glazes"),
  WRITE_NOTICES("write:notices"),
  /** Suggest changes over MCP, which wait for staff to approve them in the app. */
  PROPOSE("propose");

  private final String wireName;

  TokenScope(String wireName) {
    this.wireName = wireName;
  }

  public String wireName() {
    return wireName;
  }

  public static Optional<TokenScope> fromWireName(String name) {
    return Arrays.stream(values()).filter(s -> s.wireName.equals(name)).findFirst();
  }

  /**
   * The permissions this scope allows a token to use, the one place scopes meet permissions.
   *
   * <p>{@code read} allows what reading the reachable areas needs, including the permissions that
   * decide how much a reader sees (a staff reader sees everyone's pieces through {@code
   * pieces.move}); a write there is still refused by {@link TokenPolicy} without its write scope.
   * Each write scope allows its own area's permissions. No scope allows money (but a person's own
   * statement), access, tokens, webhooks, imports, invoices, the export, reviewing suggestions or
   * the AI key: {@link TokenPolicy}'s never-reachable areas.
   */
  public Set<Permission> permissions() {
    return switch (this) {
      case READ ->
          EnumSet.of(
              Permission.STUDIO_READ,
              Permission.MEMBERS_READ,
              Permission.MEMBERS_DUTIES_MANAGE,
              Permission.MEMBERS_CERTIFY,
              Permission.PIECES_READ,
              Permission.PIECES_MOVE,
              Permission.KILNS_READ,
              Permission.FIRINGS_MANAGE,
              Permission.FEES_READ,
              Permission.MAINTENANCE_READ,
              Permission.MAINTENANCE_MANAGE,
              Permission.MAINTENANCE_REPORTS_RESOLVE,
              Permission.SCHEDULE_READ,
              Permission.SCHEDULE_MANAGE,
              Permission.SCHEDULE_HOURS_MANAGE,
              Permission.SCHEDULE_BOOK_FOR_OTHERS,
              Permission.SUPPLIES_READ,
              Permission.SUPPLIES_MANAGE,
              Permission.SUPPLIES_PURCHASE_REQUEST,
              Permission.GLAZES_READ,
              Permission.GLAZES_STUDIO_EDIT,
              Permission.MONEY_OWN_READ,
              Permission.NOTICES_READ,
              Permission.TODAY_READ,
              Permission.ACTIVITY_READ,
              Permission.MCP_USE);
      case WRITE_PIECES ->
          EnumSet.of(
              Permission.PIECES_INTAKE_OWN,
              Permission.PIECES_INTAKE_FOR_OTHERS,
              Permission.PIECES_MOVE,
              Permission.PIECES_POLICY_EDIT,
              Permission.FEES_READ);
      case WRITE_KILNS -> EnumSet.of(Permission.KILNS_MANAGE, Permission.FIRINGS_MANAGE);
      case WRITE_MAINTENANCE ->
          EnumSet.of(
              Permission.MAINTENANCE_READ,
              Permission.MAINTENANCE_REPORT_PROBLEM,
              Permission.MAINTENANCE_MANAGE,
              Permission.MAINTENANCE_COMPLETE_ANY,
              Permission.MAINTENANCE_COMPLETE_ASSIGNED,
              Permission.MAINTENANCE_REPORTS_RESOLVE);
      case WRITE_SCHEDULE ->
          EnumSet.of(
              Permission.MEMBERS_CERTIFY,
              Permission.SCHEDULE_MANAGE,
              Permission.SCHEDULE_BOOK_OWN,
              Permission.SCHEDULE_BOOK_FOR_OTHERS,
              Permission.SCHEDULE_SHIFTS_TAKE_OPEN,
              Permission.SCHEDULE_SHIFTS_TAKE_DUTY,
              Permission.SCHEDULE_HOURS_MANAGE,
              Permission.SCHEDULE_HOURS_LOG_OWN);
      case WRITE_SUPPLIES ->
          EnumSet.of(
              Permission.SUPPLIES_MANAGE,
              Permission.SUPPLIES_SELL,
              Permission.SUPPLIES_PURCHASE_REQUEST,
              Permission.SUPPLIES_PURCHASE_APPROVE);
      case WRITE_GLAZES -> EnumSet.of(Permission.GLAZES_OWN_EDIT, Permission.GLAZES_STUDIO_EDIT);
      case WRITE_NOTICES -> EnumSet.of(Permission.NOTICES_READ, Permission.NOTICES_SEND);
      case PROPOSE -> EnumSet.of(Permission.PROPOSALS_CREATE);
    };
  }

  /** What a token with {@code scopes} may use: everything any of them allows. */
  public static Set<Permission> allowedBy(Collection<TokenScope> scopes) {
    EnumSet<Permission> allowed = EnumSet.noneOf(Permission.class);
    scopes.forEach(scope -> allowed.addAll(scope.permissions()));
    return Collections.unmodifiableSet(allowed);
  }
}
