package io.github.aaronfilson.studiomanager.demo;

import io.github.aaronfilson.studiomanager.firing.FeeRule;
import io.github.aaronfilson.studiomanager.firing.FeeSchedule;
import io.github.aaronfilson.studiomanager.firing.FeeService;
import io.github.aaronfilson.studiomanager.firing.PieceSize;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.Draft;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.Line;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.LineKind;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.OwnerKind;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.Unit;
import io.github.aaronfilson.studiomanager.glaze.GlazeEdits;
import io.github.aaronfilson.studiomanager.kiln.FiringKind;
import io.github.aaronfilson.studiomanager.kiln.FiringLoads;
import io.github.aaronfilson.studiomanager.kiln.FiringTransitions;
import io.github.aaronfilson.studiomanager.kiln.FiringView;
import io.github.aaronfilson.studiomanager.kiln.KilnService;
import io.github.aaronfilson.studiomanager.kiln.KilnService.KilnSettings;
import io.github.aaronfilson.studiomanager.kiln.KilnView;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.PaymentChannel;
import io.github.aaronfilson.studiomanager.ledger.LedgerService;
import io.github.aaronfilson.studiomanager.maintenance.AssetChanges;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.AssetKind;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Task;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.TaskSettings;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceRecords;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceService;
import io.github.aaronfilson.studiomanager.maintenance.ProblemReportService;
import io.github.aaronfilson.studiomanager.maintenance.TaskChanges;
import io.github.aaronfilson.studiomanager.piece.PieceIntake;
import io.github.aaronfilson.studiomanager.piece.PieceService;
import io.github.aaronfilson.studiomanager.piece.PieceStage;
import io.github.aaronfilson.studiomanager.platform.measure.Length;
import io.github.aaronfilson.studiomanager.platform.measure.LengthUnit;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.supplies.PurchaseRequests;
import io.github.aaronfilson.studiomanager.supplies.SupplyItems;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Category;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.ItemSettings;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.MovementKind;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.PurchaseRequest;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.RequestStatus;
import io.github.aaronfilson.studiomanager.supplies.SupplySales;
import io.github.aaronfilson.studiomanager.supplies.SupplyService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Fills a new, empty Demo Clay Co. with a few weeks of a small studio's life, through the same
 * services the app calls, as the people who would do each thing. The same steps every time, so
 * every reset looks the same apart from the dates.
 *
 * <p>Each call is its own transaction, as each would be in the app, because row-level security and
 * Hibernate's tenant are read when a transaction begins.
 */
@Component
class DemoSeed {

  /** What the seed made, for the reset to report and the tests to check. */
  record Seeded(int pieces, int kilns, int firings, int glazes, int supplies) {}

  private final FeeService fees;
  private final KilnService kilns;
  private final FiringLoads loads;
  private final FiringTransitions firings;
  private final PieceService pieces;
  private final GlazeEdits glazes;
  private final MaintenanceService maintenance;
  private final AssetChanges assets;
  private final TaskChanges tasks;
  private final MaintenanceRecords records;
  private final ProblemReportService problems;
  private final SupplyItems supplyItems;
  private final SupplyService supplies;
  private final SupplySales sales;
  private final PurchaseRequests purchases;
  private final LedgerService ledger;

  DemoSeed(
      FeeService fees,
      KilnService kilns,
      FiringLoads loads,
      FiringTransitions firings,
      PieceService pieces,
      GlazeEdits glazes,
      MaintenanceService maintenance,
      AssetChanges assets,
      TaskChanges tasks,
      MaintenanceRecords records,
      ProblemReportService problems,
      SupplyItems supplyItems,
      SupplyService supplies,
      SupplySales sales,
      PurchaseRequests purchases,
      LedgerService ledger) {
    this.fees = fees;
    this.kilns = kilns;
    this.loads = loads;
    this.firings = firings;
    this.pieces = pieces;
    this.glazes = glazes;
    this.maintenance = maintenance;
    this.assets = assets;
    this.tasks = tasks;
    this.records = records;
    this.problems = problems;
    this.supplyItems = supplyItems;
    this.supplies = supplies;
    this.sales = sales;
    this.purchases = purchases;
    this.ledger = ledger;
  }

  /**
   * Seeds the studio every context in {@code people} is bound to. Everyone in it is already a
   * member, in their {@link DemoPeople#role()}.
   */
  Seeded seed(Map<DemoPeople, TenantContext> people) {
    Map<DemoPeople, TenantContext> as = new EnumMap<>(people);
    TenantContext owner = context(as, DemoPeople.MARA);
    TenantContext staff = context(as, DemoPeople.THEO);

    // Firing fees: 4 cents a cubic inch, each side rounded up to the half inch, $4 at least
    TenantContext.callAs(
        owner,
        () ->
            fees.save(
                new FeeRule.PerVolume(
                    new BigDecimal("0.04"),
                    FeeRule.VolumeUnit.CUBIC_INCH,
                    FeeRule.DimensionRounding.HALF_UNIT),
                new BigDecimal("4.00"),
                FeeSchedule.DEFAULT_TAX_CATEGORY));

    // Kilns. The maintenance module adds each one's asset by itself.
    KilnView bigKiln =
        TenantContext.callAs(
            owner,
            () ->
                kilns.create(
                    new KilnSettings("Big Skutt", "Kiln room", KilnView.KilnType.ELECTRIC, 6)));
    KilnView easyFire =
        TenantContext.callAs(
            owner,
            () ->
                kilns.create(
                    new KilnSettings(
                        "L&L Easy-Fire", "Kiln room", KilnView.KilnType.ELECTRIC, null)));
    KilnView testKiln =
        TenantContext.callAs(
            owner,
            () ->
                kilns.create(
                    new KilnSettings("Test kiln", "Glaze lab", KilnView.KilnType.ELECTRIC, null)));
    TenantContext.callAs(
        owner,
        () ->
            kilns.setDown(
                testKiln.id(),
                "Element 2 burned out; a replacement is on order.",
                testKiln.version()));

    // Pieces, dropped off at the shelf and taken in by staff
    List<UUID> received = new ArrayList<>();
    for (DemoPiece piece : DemoPiece.ALL) {
      UUID makerId = context(as, piece.maker()).accountId();
      received.add(
          TenantContext.callAs(
                  staff,
                  () ->
                      pieces.intake(
                          new PieceIntake(
                              makerId, piece.description(), piece.clayBody(), null, piece.size())))
              .id());
    }

    // The first eight were bisque fired last week
    fire(staff, bigKiln.id(), FiringKind.BISQUE, "04", received.subList(0, 8), "Even, no cracks.");
    // Six were glazed; four of those came out of a glaze firing
    for (UUID pieceId : received.subList(0, 6)) {
      move(staff, pieceId, PieceStage.GLAZED);
    }
    fire(
        staff,
        bigKiln.id(),
        FiringKind.GLAZE,
        "6",
        received.subList(0, 4),
        "Cone 6 bent at 4 o'clock.");
    // Two have been picked up
    for (UUID pieceId : received.subList(0, 2)) {
      move(staff, pieceId, PieceStage.PICKED_UP);
    }
    // A bisque load is firing now
    FiringView firing =
        TenantContext.callAs(
            staff,
            () -> loads.load(easyFire.id(), FiringKind.BISQUE, "04", received.subList(8, 10)));
    TenantContext.callAs(staff, () -> firings.start(firing.id(), firing.version()));

    int glazeCount = seedGlazes(staff, context(as, DemoPeople.JUN));
    seedMaintenance(owner, staff, context(as, DemoPeople.SAM), bigKiln.id());
    int supplyCount = seedSupplies(owner, staff, as);

    // Members paying what they owe
    TenantContext.callAs(
        staff,
        () ->
            ledger.recordPayment(
                context(as, DemoPeople.PRIYA).accountId(),
                new BigDecimal("40.00"),
                PaymentChannel.CASH,
                "at the desk"));
    TenantContext.callAs(
        staff,
        () ->
            ledger.recordPayment(
                context(as, DemoPeople.SAM).accountId(),
                new BigDecimal("25.00"),
                PaymentChannel.SQUARE,
                "card reader"));

    return new Seeded(DemoPiece.ALL.size(), 3, 3, glazeCount, supplyCount);
  }

  private void fire(
      TenantContext staff,
      UUID kilnId,
      FiringKind kind,
      String cone,
      List<UUID> pieceIds,
      String notes) {
    FiringView loaded =
        TenantContext.callAs(staff, () -> loads.load(kilnId, kind, cone, List.copyOf(pieceIds)));
    FiringView started =
        TenantContext.callAs(staff, () -> firings.start(loaded.id(), loaded.version()));
    TenantContext.callAs(staff, () -> firings.complete(started.id(), notes, started.version()));
  }

  private void move(TenantContext staff, UUID pieceId, PieceStage target) {
    TenantContext.callAs(
        staff, () -> pieces.move(pieceId, target, null, pieces.get(pieceId).version()));
  }

  private int seedGlazes(TenantContext staff, TenantContext member) {
    List<Draft> studioGlazes =
        List.of(
            new Draft(
                "Studio Clear",
                "The liner glaze. Dip for a count of three.",
                "6",
                "OXIDATION",
                true,
                new BigDecimal("1.42"),
                false,
                List.of(
                    base("Ferro Frit 3134", "20"),
                    base("Wollastonite", "15"),
                    base("EPK kaolin", "20"),
                    base("Silica 325", "25"),
                    base("Nepheline syenite", "20"))),
            new Draft(
                "Floating Blue",
                "Runs on vertical surfaces; keep it off the bottom inch.",
                "6",
                "OXIDATION",
                false,
                new BigDecimal("1.48"),
                false,
                List.of(
                    base("Nepheline syenite", "47.3"),
                    base("Gerstley borate", "27"),
                    base("Silica 325", "20.6"),
                    base("EPK kaolin", "5.1"),
                    addition("Red iron oxide", "2"),
                    addition("Cobalt carbonate", "1"),
                    addition("Rutile", "4"))),
            new Draft(
                "Tenmoku",
                "Breaks rust on edges where it's thin.",
                "6",
                "OXIDATION",
                false,
                null,
                false,
                List.of(
                    base("Custer feldspar", "40"),
                    base("Silica 325", "25"),
                    base("Whiting", "15"),
                    base("EPK kaolin", "10"),
                    base("Talc", "10"),
                    addition("Red iron oxide", "10"))));
    for (Draft draft : studioGlazes) {
      TenantContext.callAs(staff, () -> glazes.create(OwnerKind.STUDIO, draft));
    }
    // A member's own recipe, shared with the studio
    TenantContext.callAs(
        member,
        () ->
            glazes.create(
                OwnerKind.MEMBER,
                new Draft(
                    "Jun's Celadon",
                    "Pale green over porcelain. Thin coats.",
                    "6",
                    "OXIDATION",
                    true,
                    null,
                    true,
                    List.of(
                        base("Custer feldspar", "30"),
                        base("Silica 325", "30"),
                        base("Whiting", "20"),
                        base("EPK kaolin", "20"),
                        addition("Red iron oxide", "1")))));
    return studioGlazes.size() + 1;
  }

  private void seedMaintenance(
      TenantContext owner, TenantContext staff, TenantContext member, UUID bigKilnId) {
    UUID kilnAsset =
        TenantContext.callAs(staff, () -> maintenance.assetOfKiln(bigKilnId))
            .orElseThrow(() -> new IllegalStateException("Every kiln has an asset"));
    Task vacuum =
        TenantContext.callAs(
            staff,
            () ->
                tasks.createTask(
                    kilnAsset,
                    new TaskSettings(
                        "Vacuum the elements and check the thermocouple",
                        List.of(
                            "Vacuum the element grooves",
                            "Check the thermocouple for pitting",
                            "Look over the lid and brick for cracks"),
                        null,
                        25,
                        null,
                        null)));
    TenantContext.callAs(
        staff,
        () ->
            tasks.createTask(
                kilnAsset,
                new TaskSettings(
                    "Re-coat the shelves with kiln wash",
                    List.of("Scrape off drips", "Two thin coats of wash"),
                    90,
                    null,
                    null,
                    null)));
    TenantContext.callAs(
        staff,
        () ->
            records.complete(
                vacuum.id(),
                "Elements look fine. Thermocouple has some pitting; watch it.",
                null,
                List.of("Vacuum the element grooves", "Check the thermocouple for pitting"),
                List.of()));
    TenantContext.callAs(
        staff,
        () ->
            records.recordRepair(
                kilnAsset, "Replaced the lid hinge pin.", new BigDecimal("18.50"), List.of()));

    UUID wheel =
        TenantContext.callAs(
                owner,
                () -> assets.createAsset(AssetKind.WHEEL, "Wheel 3", "Wheel room", "Brent CXC"))
            .id();
    TenantContext.callAs(
        staff,
        () ->
            tasks.createTask(
                wheel,
                new TaskSettings(
                    "Clean the splash pan and check the belt",
                    List.of("Empty and scrub the splash pan", "Check the belt tension"),
                    14,
                    null,
                    null,
                    null)));
    UUID pugMill =
        TenantContext.callAs(
                owner,
                () ->
                    assets.createAsset(
                        AssetKind.PUG_MILL, "Peter Pugger VPM-20", "Reclaim corner", null))
            .id();
    TenantContext.callAs(
        staff,
        () ->
            tasks.createTask(
                pugMill,
                new TaskSettings(
                    "Clean the auger and seals",
                    List.of("Run it empty", "Clean the auger", "Check the seals"),
                    30,
                    null,
                    null,
                    null)));
    UUID vent =
        TenantContext.callAs(
                owner,
                () ->
                    assets.createAsset(
                        AssetKind.VENTILATION, "Kiln room vent hood", "Kiln room", null))
            .id();
    TenantContext.callAs(
        staff,
        () ->
            tasks.createTask(
                vent,
                new TaskSettings(
                    "Check the vent fan and damper",
                    List.of("Fan runs at full speed", "Damper opens fully"),
                    30,
                    null,
                    null,
                    null)));

    // Something a member noticed
    TenantContext.callAs(
        member, () -> problems.report(wheel, "Wheel 3 grinds when it speeds up past halfway."));
  }

  private int seedSupplies(
      TenantContext owner, TenantContext staff, Map<DemoPeople, TenantContext> as) {
    UUID bMix =
        supply(
            staff,
            new ItemSettings(
                "B-Mix 5 with grog",
                Category.CLAY,
                "25 lb bag",
                "Laguna Clay",
                7,
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("32.00"),
                null,
                "Speckled Buff"),
            "24");
    UUID buff =
        supply(
            staff,
            new ItemSettings(
                "Speckled Buff",
                Category.CLAY,
                "25 lb bag",
                "Laguna Clay",
                7,
                new BigDecimal("6"),
                new BigDecimal("12"),
                new BigDecimal("30.00"),
                null,
                null),
            "5");
    UUID wax =
        supply(
            staff,
            new ItemSettings(
                "Wax resist",
                Category.GLAZE,
                "pint",
                "Aftosa",
                5,
                new BigDecimal("3"),
                new BigDecimal("6"),
                new BigDecimal("9.50"),
                null,
                null),
            "8");
    supply(
        staff,
        new ItemSettings(
            "Kiln wash",
            Category.KILN_SUPPLY,
            "lb",
            "Bailey Pottery",
            10,
            new BigDecimal("5"),
            new BigDecimal("10"),
            null,
            null,
            null),
        "12");
    UUID element =
        supply(
            staff,
            new ItemSettings(
                "Skutt 1027 element",
                Category.KILN_SUPPLY,
                "each",
                "Skutt",
                14,
                new BigDecimal("1"),
                new BigDecimal("2"),
                null,
                null,
                null),
            null);
    UUID sponges =
        supply(
            staff,
            new ItemSettings(
                "Elephant ear sponge",
                Category.TOOL,
                "each",
                null,
                null,
                new BigDecimal("10"),
                new BigDecimal("24"),
                new BigDecimal("2.00"),
                null,
                null),
            "30");
    UUID gerstley =
        supply(
            staff,
            new ItemSettings(
                "Gerstley borate",
                Category.GLAZE_MATERIAL,
                "lb",
                "Bailey Pottery",
                10,
                new BigDecimal("10"),
                new BigDecimal("25"),
                null,
                null,
                null),
            "8");

    // Sales at the desk, charged to members' accounts
    sell(staff, bMix, context(as, DemoPeople.PRIYA).accountId(), "2");
    sell(staff, wax, context(as, DemoPeople.SAM).accountId(), "1");
    sell(staff, sponges, context(as, DemoPeople.JUN).accountId(), "3");

    // Purchase requests: two waiting for the owner, one approved and ordered
    TenantContext.callAs(
        staff, () -> purchases.request(buff, null, "Running low after the beginners' class."));
    TenantContext.callAs(staff, () -> purchases.request(gerstley, null, null));
    PurchaseRequest elements =
        TenantContext.callAs(
            staff, () -> purchases.request(element, null, "For the test kiln, and a spare."));
    PurchaseRequest approved =
        TenantContext.callAs(
            owner,
            () -> purchases.decide(elements.id(), RequestStatus.APPROVED, elements.version()));
    TenantContext.callAs(
        staff, () -> purchases.decide(approved.id(), RequestStatus.ORDERED, approved.version()));
    return 7;
  }

  private UUID supply(TenantContext staff, ItemSettings settings, @Nullable String received) {
    UUID itemId = TenantContext.callAs(staff, () -> supplyItems.create(settings)).id();
    if (received != null) {
      TenantContext.callAs(
          staff,
          () ->
              supplies.recordMovement(
                  itemId, MovementKind.RECEIVED, new BigDecimal(received), "Opening stock"));
    }
    return itemId;
  }

  private void sell(TenantContext staff, UUID itemId, UUID memberId, String quantity) {
    TenantContext.callAs(staff, () -> sales.sell(itemId, memberId, new BigDecimal(quantity)));
  }

  private static Line base(String material, String parts) {
    return new Line(LineKind.BASE, material, new BigDecimal(parts), Unit.PARTS);
  }

  private static Line addition(String material, String percent) {
    return new Line(LineKind.ADDITION, material, new BigDecimal(percent), Unit.PERCENT);
  }

  private static TenantContext context(Map<DemoPeople, TenantContext> as, DemoPeople person) {
    TenantContext context = as.get(person);
    if (context == null) {
      throw new IllegalStateException("The seed needs everyone in the studio");
    }
    return context;
  }

  /** A piece someone dropped off, measured in inches. */
  private record DemoPiece(
      DemoPeople maker,
      String description,
      String clayBody,
      String length,
      String width,
      String height) {

    static final List<DemoPiece> ALL =
        List.of(
            new DemoPiece(DemoPeople.PRIYA, "Lidded jar", "B-Mix 5", "5", "5", "7"),
            new DemoPiece(DemoPeople.PRIYA, "Mug", "B-Mix 5", "4", "3.5", "4.5"),
            new DemoPiece(DemoPeople.SAM, "Serving bowl", "Speckled Buff", "10", "10", "4"),
            new DemoPiece(DemoPeople.SAM, "Bud vase", "Speckled Buff", "3", "3", "6"),
            new DemoPiece(DemoPeople.JUN, "Teapot", "Porcelain", "7", "5", "6"),
            new DemoPiece(DemoPeople.JUN, "Yunomi", "Porcelain", "3.5", "3.5", "4"),
            new DemoPiece(DemoPeople.SAM, "Pasta bowl", "Speckled Buff", "9", "9", "2.5"),
            new DemoPiece(DemoPeople.THEO, "Large planter", "B-Mix 5", "12", "12", "10"),
            new DemoPiece(DemoPeople.PRIYA, "Serving platter", "B-Mix 5", "14", "10", "2"),
            new DemoPiece(DemoPeople.JUN, "Dinner plate", "Porcelain", "11", "11", "1.5"),
            new DemoPiece(DemoPeople.SAM, "Pitcher", "Speckled Buff", "6", "5", "9"),
            new DemoPiece(DemoPeople.PRIYA, "Spoon rest", "B-Mix 5", "5", "3", "1"));

    PieceSize size() {
      return new PieceSize(
          Length.of(length, LengthUnit.INCH),
          Length.of(width, LengthUnit.INCH),
          Length.of(height, LengthUnit.INCH),
          null);
    }
  }
}
