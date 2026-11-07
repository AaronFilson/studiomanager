package io.github.aaronfilson.studiomanager.platform.tenancy;

/**
 * What each {@link Permission} is called and what it lets a person do, in words an owner reads on
 * the roles page (plan 21, lane R6). The API serves these, so the web and mobile apps share one
 * wording. The switch has a case per permission and no default, so a new permission doesn't compile
 * until it is described here.
 */
public final class PermissionCatalogue {

  /** The groups the roles page shows permissions in, in this order. */
  public enum Area {
    STUDIO("Studio"),
    MEMBERS("Members"),
    PIECES("Pieces"),
    KILNS("Kilns and firings"),
    FEES("Fees"),
    MAINTENANCE("Maintenance"),
    SCHEDULE("Schedule"),
    SUPPLIES("Supplies"),
    GLAZES("Glazes"),
    MONEY("Money"),
    NOTICES("Notices"),
    OVERVIEW("Overview"),
    INTEGRATIONS("Integrations"),
    OWNERS("Owners");

    private final String label;

    Area(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  /**
   * How a permission is shown.
   *
   * @param area the group it is shown in
   * @param label a short name, such as "Move pieces"
   * @param description one sentence on what it allows
   */
  public record Description(Area area, String label, String description) {}

  private PermissionCatalogue() {}

  public static Description describe(Permission permission) {
    return switch (permission) {
      case STUDIO_READ ->
          new Description(
              Area.STUDIO, "See the studio", "Open the studio and see its name and units.");
      case STUDIO_SETTINGS_EDIT ->
          new Description(
              Area.STUDIO,
              "Edit studio settings",
              "Change the studio's settings, such as its units.");
      case STUDIO_EXPORT ->
          new Description(
              Area.STUDIO, "Download all data", "Download everything the studio holds.");
      case STUDIO_AI_KEY_MANAGE ->
          new Description(
              Area.STUDIO, "Manage the AI key", "Set, replace or remove the studio's AI key.");
      case STUDIO_ROLES_MANAGE ->
          new Description(
              Area.STUDIO,
              "Manage roles",
              "Choose what each role may do, and read the access log.");
      case MEMBERS_READ ->
          new Description(
              Area.MEMBERS, "See members", "See everyone in the studio and their roles.");
      case MEMBERS_INVITE ->
          new Description(
              Area.MEMBERS, "Invite people", "Invite people, up to the roles they could hold.");
      case MEMBERS_ROLE_ASSIGN ->
          new Description(Area.MEMBERS, "Change roles", "Change a member's role.");
      case MEMBERS_REMOVE ->
          new Description(Area.MEMBERS, "Remove members", "Remove someone from the studio.");
      case MEMBERS_DUTIES_MANAGE ->
          new Description(
              Area.MEMBERS, "Assign duties", "Give members duties, such as monitor or technician.");
      case MEMBERS_CERTIFY ->
          new Description(
              Area.MEMBERS,
              "Certify members",
              "Manage certification types and certify members for equipment.");
      case PIECES_INTAKE_OWN ->
          new Description(
              Area.PIECES, "Take in own pieces", "Bring in their own pieces for firing.");
      case PIECES_READ ->
          new Description(Area.PIECES, "See pieces", "See their pieces and where each one is.");
      case PIECES_INTAKE_FOR_OTHERS ->
          new Description(
              Area.PIECES,
              "Take in pieces for others",
              "Take in pieces on another member's behalf.");
      case PIECES_MOVE ->
          new Description(
              Area.PIECES, "Move pieces", "See everyone's pieces and move them between stages.");
      case PIECES_POLICY_EDIT ->
          new Description(
              Area.PIECES, "Edit the pickup policy", "Change how long ready pieces are held.");
      case KILNS_READ -> new Description(Area.KILNS, "See kilns", "See the studio's kilns.");
      case KILNS_MANAGE ->
          new Description(Area.KILNS, "Manage kilns", "Add, edit and retire kilns.");
      case FIRINGS_MANAGE ->
          new Description(
              Area.KILNS, "Run firings", "Plan firings, load kilns and work the kiln queue.");
      case FEES_READ ->
          new Description(Area.FEES, "See fees", "See the fee schedule and get a quote.");
      case FEES_EDIT -> new Description(Area.FEES, "Edit fees", "Change the firing fee schedule.");
      case MAINTENANCE_READ ->
          new Description(
              Area.MAINTENANCE, "See maintenance", "See maintenance tasks and reports.");
      case MAINTENANCE_REPORT_PROBLEM ->
          new Description(Area.MAINTENANCE, "Report problems", "Report a problem with equipment.");
      case MAINTENANCE_MANAGE ->
          new Description(
              Area.MAINTENANCE, "Manage maintenance", "Plan maintenance tasks, records and parts.");
      case MAINTENANCE_COMPLETE_ANY ->
          new Description(Area.MAINTENANCE, "Complete any task", "Mark any maintenance task done.");
      case MAINTENANCE_COMPLETE_ASSIGNED ->
          new Description(
              Area.MAINTENANCE,
              "Complete duty tasks",
              "Mark done the tasks assigned to their duty. The technician duty grants this.");
      case MAINTENANCE_REPORTS_RESOLVE ->
          new Description(
              Area.MAINTENANCE, "Resolve problem reports", "See every report and resolve them.");
      case SCHEDULE_READ ->
          new Description(Area.SCHEDULE, "See the schedule", "See the calendar and the roster.");
      case SCHEDULE_BOOK_OWN ->
          new Description(
              Area.SCHEDULE, "Book for themselves", "Book studio resources for themselves.");
      case SCHEDULE_SHIFTS_TAKE_OPEN ->
          new Description(
              Area.SCHEDULE,
              "Take open shifts",
              "Take an open shift, or offer their own for cover.");
      case SCHEDULE_SHIFTS_TAKE_DUTY ->
          new Description(
              Area.SCHEDULE,
              "Take duty shifts",
              "Take a shift that needs a duty they hold. Duties grant this.");
      case SCHEDULE_HOURS_LOG_OWN ->
          new Description(
              Area.SCHEDULE,
              "Log own hours",
              "Log their own credited hours. The work-exchange duty grants this.");
      case SCHEDULE_MANAGE ->
          new Description(
              Area.SCHEDULE,
              "Manage the schedule",
              "Set up resources and shifts, and see all bookings.");
      case SCHEDULE_HOURS_MANAGE ->
          new Description(
              Area.SCHEDULE, "Manage hours", "See and adjust everyone's credited hours.");
      case SCHEDULE_BOOK_FOR_OTHERS ->
          new Description(Area.SCHEDULE, "Book for others", "Book resources for other members.");
      case SUPPLIES_READ ->
          new Description(Area.SUPPLIES, "See supplies", "See the supplies catalogue.");
      case SUPPLIES_MANAGE ->
          new Description(Area.SUPPLIES, "Manage supplies", "Edit the catalogue and count stock.");
      case SUPPLIES_SELL -> new Description(Area.SUPPLIES, "Sell supplies", "Record supply sales.");
      case SUPPLIES_PURCHASE_REQUEST ->
          new Description(Area.SUPPLIES, "Request purchases", "Ask for supplies to be bought.");
      case SUPPLIES_PURCHASE_APPROVE ->
          new Description(Area.SUPPLIES, "Approve purchases", "Approve purchase requests.");
      case INVOICES_MANAGE ->
          new Description(
              Area.SUPPLIES, "Manage supplier invoices", "Read and confirm supplier invoices.");
      case GLAZES_READ -> new Description(Area.GLAZES, "See glazes", "See the glaze book.");
      case GLAZES_OWN_EDIT ->
          new Description(Area.GLAZES, "Keep own recipes", "Add and edit their own glaze recipes.");
      case GLAZES_STUDIO_EDIT ->
          new Description(Area.GLAZES, "Edit studio glazes", "Edit the studio's own glazes.");
      case MONEY_OWN_READ ->
          new Description(
              Area.MONEY, "See own balance", "See their own balance, statement and consignment.");
      case MONEY_READ ->
          new Description(Area.MONEY, "See the books", "See the ledger, balances and reports.");
      case MONEY_TAKE_PAYMENT ->
          new Description(Area.MONEY, "Take payments", "Record payments from members.");
      case MONEY_STORED_VALUE_MANAGE ->
          new Description(
              Area.MONEY, "Manage gift cards and credit", "Issue gift cards and stored credit.");
      case MONEY_REFUND_AND_ADJUST ->
          new Description(Area.MONEY, "Refund and adjust", "Refund payments and adjust balances.");
      case MONEY_PAYOUTS ->
          new Description(Area.MONEY, "Make payouts", "Pay out to artists and instructors.");
      case MONEY_PAYABLES_MANAGE ->
          new Description(
              Area.MONEY,
              "Manage artists and instructors",
              "Manage payees, consignment and payment runs.");
      case MONEY_TAX_RATES_EDIT ->
          new Description(Area.MONEY, "Edit tax rates", "Change the studio's sales tax rates.");
      case NOTICES_READ ->
          new Description(Area.NOTICES, "Read notices", "Read their notifications and notices.");
      case NOTICES_SEND ->
          new Description(Area.NOTICES, "Send notices", "Send a notice to the studio.");
      case TODAY_READ ->
          new Description(Area.OVERVIEW, "See today", "See the studio's overview for today.");
      case ACTIVITY_READ ->
          new Description(Area.OVERVIEW, "See activity", "See the studio's activity stream.");
      case IMPORTS_RUN ->
          new Description(Area.INTEGRATIONS, "Import data", "Import members and studio data.");
      case WEBHOOKS_MANAGE ->
          new Description(Area.INTEGRATIONS, "Manage webhooks", "Add and edit webhooks.");
      case API_TOKENS_OWN ->
          new Description(
              Area.INTEGRATIONS, "Use API tokens", "Make and revoke their own API tokens.");
      case API_TOKENS_MANAGE_ALL ->
          new Description(
              Area.INTEGRATIONS, "Manage everyone's tokens", "See and revoke anyone's API tokens.");
      case MCP_USE ->
          new Description(
              Area.INTEGRATIONS, "Use the assistant", "Connect an AI assistant through MCP.");
      case PROPOSALS_CREATE ->
          new Description(
              Area.INTEGRATIONS, "Suggest changes", "Let an assistant suggest changes for review.");
      case PROPOSALS_REVIEW ->
          new Description(
              Area.INTEGRATIONS,
              "Review suggestions",
              "Approve or turn down assistant suggestions.");
      case OWNERS_MANAGE ->
          new Description(
              Area.OWNERS,
              "Manage owners",
              "Add or remove co-owners and choose the billing contact.");
    };
  }
}
