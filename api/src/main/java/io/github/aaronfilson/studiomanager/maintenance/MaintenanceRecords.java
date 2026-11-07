package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.access.AccessService;
import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.PartUse;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.PartUsed;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Task;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.WorkRecord;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.Csv;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Item;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.MovementKind;
import io.github.aaronfilson.studiomanager.supplies.SupplyService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The record history: work done on an asset, written when a task is completed or a repair is
 * logged, and read as an asset's history or as the export. Records are insert-only.
 *
 * <p>Every record is written by {@link #insertRecord}, and an asset's history is read by {@link
 * #records}; the export reads through {@link MaintenanceQueries#recordsDoneBetween}.
 */
@Service
class MaintenanceRecords {

  /** The most records {@link #records} returns. */
  static final int RECORDS_LIMIT = 500;

  private final MaintenanceRecordRepository records;
  private final MaintenanceTaskRepository tasks;
  private final MaintenanceService reads;
  private final MaintenanceQueries queries;
  private final AccessService access;
  private final AccountDirectory accounts;
  private final SupplyService supplies;
  private final Clock clock;

  MaintenanceRecords(
      MaintenanceRecordRepository records,
      MaintenanceTaskRepository tasks,
      MaintenanceService reads,
      MaintenanceQueries queries,
      AccessService access,
      AccountDirectory accounts,
      SupplyService supplies,
      Clock clock) {
    this.records = records;
    this.tasks = tasks;
    this.reads = reads;
    this.queries = queries;
    this.access = access;
    this.accounts = accounts;
    this.supplies = supplies;
    this.clock = clock;
  }

  /**
   * Records a task as done and restarts its clock, and only its clock. Staff can complete any task;
   * anyone else only one assigned to them, by name or through a duty they hold. Parts used are
   * drawn from supplies, which only staff can do.
   */
  @Transactional
  public Task complete(
      UUID taskId,
      @Nullable String note,
      @Nullable BigDecimal cost,
      List<String> checked,
      List<PartUse> parts) {
    Instant now = clock.instant();
    MaintenanceTask task = reads.findTask(taskId);
    requireMayComplete(task);
    Set<String> lines = new HashSet<>(task.checklist());
    List<String> ticked = checked.stream().map(String::strip).distinct().toList();
    if (!lines.containsAll(ticked)) {
      throw new InvalidInputException("Only lines from the task's checklist can be ticked.");
    }
    requireValidParts(parts);
    MaintainedAsset asset = task.asset();
    Integer firings = reads.firingsNow(asset);
    insertRecord(
        asset,
        task,
        note,
        cost,
        ticked,
        firings,
        parts,
        task.title() + ": " + reads.nameOf(asset),
        now);
    task.markDone(now, Objects.requireNonNullElse(firings, 0));
    tasks.flush();
    return reads.view(task);
  }

  /**
   * Logs work done outside any task, such as a repair. No task's clock restarts. Each part used is
   * taken out of supplies' stock, so the history says which spare went in and the shelf count stays
   * right.
   */
  @Transactional
  public WorkRecord recordRepair(
      UUID assetId, String note, @Nullable BigDecimal cost, List<PartUse> parts) {
    if (note.isBlank()) {
      throw new InvalidInputException("Describe the work.");
    }
    requireValidParts(parts);
    MaintainedAsset asset = reads.findAsset(assetId);
    MaintenanceRecord record =
        insertRecord(
            asset,
            null,
            note,
            cost,
            List.of(),
            reads.firingsNow(asset),
            parts,
            "Repair: " + reads.nameOf(asset),
            clock.instant());
    UUID doneBy = record.doneBy();
    return record.toView(
        accounts.displayNames(List.of(doneBy)).get(doneBy), partsUsed(record.parts()));
  }

  /** The asset's history, newest first, at most {@value #RECORDS_LIMIT}. */
  @Transactional(readOnly = true)
  public Capped<WorkRecord> records(UUID assetId) {
    reads.findAsset(assetId);
    List<MaintenanceRecord> found =
        records.findNewestForAsset(assetId, Limit.of(Capped.fetchSize(RECORDS_LIMIT)));
    Map<UUID, String> names =
        accounts.displayNames(found.stream().map(MaintenanceRecord::doneBy).distinct().toList());
    Map<UUID, List<PartUsed>> partsByRecord = partsByRecord(records.findPartsForAsset(assetId));
    return Capped.fromFetched(found, RECORDS_LIMIT)
        .map(record ->
            record.toView(
                names.get(record.doneBy()), partsByRecord.getOrDefault(record.id(), List.of())));
  }

  /**
   * Every record done on or between two dates in the studio's time zone, oldest first, as CSV: the
   * history an insurer or landlord asks for.
   */
  @Transactional(readOnly = true)
  public String recordsCsv(LocalDate from, LocalDate to, ZoneId zone) {
    if (to.isBefore(from) || from.plusYears(10).isBefore(to)) {
      throw new InvalidInputException(
          "Choose a range of up to ten years, with 'from' on or before 'to'.");
    }
    Map<UUID, String> assetNames = reads.trackedNames();
    List<MaintenanceQueries.ExportedRecord> rows =
        queries.recordsDoneBetween(
            from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant());
    Map<UUID, String> names =
        accounts.displayNames(
            rows.stream().map(MaintenanceQueries.ExportedRecord::doneBy).distinct().toList());
    Csv csv =
        new Csv()
            .row(
                List.of(
                    "Date", "Asset", "Task", "Note", "Cost", "Checked", "At firing", "Done by"));
    for (MaintenanceQueries.ExportedRecord row : rows) {
      // Archived assets keep their records; a kiln's name lives on the kiln
      String ownName = row.ownName();
      String asset = assetNames.getOrDefault(row.assetId(), ownName == null ? "Kiln" : ownName);
      String taskTitle = row.taskTitle();
      csv.row(
          Arrays.asList(
              row.doneAt().atZone(zone).toLocalDate().toString(),
              asset,
              taskTitle == null ? "Repair or other work" : taskTitle,
              row.note(),
              row.cost(),
              String.join("; ", row.checked()),
              row.atFiring(),
              names.get(row.doneBy())));
    }
    return csv.toString();
  }

  /**
   * Writes one record, with the parts drawn from supplies for it. Each part is taken out of stock
   * first, noted with what it was for, so supplies reports an unknown or archived item before the
   * record's part rows point at it.
   */
  private MaintenanceRecord insertRecord(
      MaintainedAsset asset,
      @Nullable MaintenanceTask task,
      @Nullable String note,
      @Nullable BigDecimal cost,
      List<String> checked,
      @Nullable Integer atFiring,
      List<PartUse> parts,
      String partsWereFor,
      Instant now) {
    if (cost != null && cost.signum() < 0) {
      throw new InvalidInputException("A cost can't be negative.");
    }
    MaintenanceRecord record =
        new MaintenanceRecord(
            asset,
            task,
            MaintenanceText.optional(note, 1000, "note"),
            cost,
            checked,
            atFiring,
            TenantContext.require().accountId(),
            now);
    String movementNote =
        partsWereFor.length() > 200 ? partsWereFor.substring(0, 200) : partsWereFor;
    for (PartUse part : parts) {
      supplies.recordMovement(part.itemId(), MovementKind.USED, part.quantity(), movementNote);
      record.addPart(part.itemId(), part.quantity());
    }
    records.saveAndFlush(record);
    return record;
  }

  private static void requireValidParts(List<PartUse> parts) {
    if (parts.size() > 20
        || parts.stream().map(PartUse::itemId).distinct().count() != parts.size()) {
      throw new InvalidInputException("List each part once, up to 20 parts.");
    }
  }

  /** The parts of each record, named as supplies names them now. */
  private Map<UUID, List<PartUsed>> partsByRecord(List<RecordPart> parts) {
    if (parts.isEmpty()) {
      return Map.of();
    }
    Map<UUID, Item> items =
        supplies.items().stream().collect(Collectors.toMap(Item::id, Function.identity()));
    Map<UUID, List<PartUsed>> byRecord = new HashMap<>();
    for (RecordPart part : parts) {
      byRecord
          .computeIfAbsent(part.recordId(), recordId -> new ArrayList<>())
          .add(partUsed(part, items.get(part.itemId())));
    }
    return byRecord;
  }

  private List<PartUsed> partsUsed(List<RecordPart> parts) {
    return partsByRecord(parts).values().stream().flatMap(List::stream).toList();
  }

  /** An archived item drops out of supplies' list; its part still counts. */
  private static PartUsed partUsed(RecordPart part, @Nullable Item item) {
    return new PartUsed(
        part.itemId(),
        item == null ? "A retired supply" : item.name(),
        item == null ? "" : item.unit(),
        part.quantity().stripTrailingZeros());
  }

  private void requireMayComplete(MaintenanceTask task) {
    TenantContext caller = TenantContext.require();
    StudioRole role = caller.role();
    if (role != null && role.includes(StudioRole.STAFF)) {
      return;
    }
    Duty duty = task.assignedDuty();
    boolean mine =
        caller.accountId().equals(task.assignedTo())
            || (duty != null && access.dutiesOf(caller.accountId()).contains(duty));
    if (!mine) {
      throw new AccessDeniedException(
          "Only staff, or whoever this task is assigned to, can complete it.");
    }
  }
}
