package io.github.aaronfilson.studiomanager.maintenance;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.aaronfilson.studiomanager.access.AccessService;
import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.kiln.KilnCreated;
import io.github.aaronfilson.studiomanager.kiln.KilnService;
import io.github.aaronfilson.studiomanager.kiln.KilnView;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Asset;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.AssetKind;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.PartUse;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.PartUsed;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Record;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Task;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.TaskSettings;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.Csv;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MemberView;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Item;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.MovementKind;
import io.github.aaronfilson.studiomanager.supplies.SupplyService;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Upkeep of everything in the bound studio: kilns, other equipment and the building.
 *
 * <p>Each task keeps its own clock, the time and the kiln's firing count when it was last done,
 * so completing one task never restarts another. Kilns are assets like any other, except that
 * their name, down state and firing count come from the kiln module.
 */
@Service
public class MaintenanceService {

    private static final String SELECT_TASK = """
            select t.id, t.asset_id, t.title, t.checklist, t.every_days, t.every_firings, t.assigned_duty,
                   t.assigned_to, t.last_done_at, t.last_done_firings, t.version
            from maintenance_task t join asset a on a.id = t.asset_id
            where not t.archived and not a.archived
            """;

    private final JdbcClient jdbc;
    private final KilnService kilns;
    private final AccessService access;
    private final StudioMembers members;
    private final AccountDirectory accounts;
    private final ApplicationEventPublisher events;
    private final SupplyService supplies;

    MaintenanceService(JdbcClient jdbc, KilnService kilns, AccessService access, StudioMembers members,
            AccountDirectory accounts, ApplicationEventPublisher events, SupplyService supplies) {
        this.jdbc = jdbc;
        this.kilns = kilns;
        this.access = access;
        this.members = members;
        this.accounts = accounts;
        this.events = events;
        this.supplies = supplies;
    }

    /** Everything maintained, kilns first and then by name, each with its tasks soonest due first. */
    @Transactional(readOnly = true)
    public List<Asset> assets(Instant now) {
        List<AssetRow> rows = jdbc.sql("""
                select id, kind, kiln_id, name, location, notes, down_reason, version from asset where not archived
                """).query(MaintenanceService::assetRow).list();
        Map<UUID, KilnView> kilnsById = kilnsById();
        Map<UUID, List<Task>> tasksByAsset = tasks(SELECT_TASK, Map.of(), rows, kilnsById, now).stream()
                .collect(Collectors.groupingBy(Task::assetId));
        return rows.stream()
                .map(row -> toAsset(row, kilnsById, tasksByAsset.getOrDefault(row.id(), List.of())))
                .sorted(Comparator.comparing((Asset a) -> a.kind() != AssetKind.KILN)
                        .thenComparing(Asset::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public Asset asset(UUID assetId, Instant now) {
        return assets(now).stream().filter(a -> a.id().equals(assetId)).findFirst()
                .orElseThrow(() -> new NotFoundException("No such asset."));
    }

    /**
     * Why each out-of-service asset is down, by asset id; kilns included, from the kiln. Assets in
     * service aren't in the map. For the calendar, which won't book a down asset.
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> downReasons() {
        return assets(now()).stream().filter(a -> a.downReason() != null)
                .collect(Collectors.toMap(Asset::id, Asset::downReason));
    }

    /** Tasks due or due soon on any asset, most urgent first: what Today shows staff. */
    @Transactional(readOnly = true)
    public List<Task> dueTasks(Instant now) {
        return assets(now).stream().flatMap(a -> a.tasks().stream())
                .filter(t -> t.status() != DueStatus.OK)
                .sorted(URGENCY)
                .toList();
    }

    /** Tasks assigned to the caller by name or through a duty they hold, most urgent first. */
    @Transactional(readOnly = true)
    public List<Task> myTasks(Instant now) {
        UUID me = TenantContext.require().accountId();
        Set<Duty> duties = access.dutiesOf(me);
        return assets(now).stream().flatMap(a -> a.tasks().stream())
                .filter(t -> me.equals(t.assignedTo()) || (t.assignedDuty() != null && duties.contains(t.assignedDuty())))
                .sorted(URGENCY)
                .toList();
    }

    @Transactional
    public Asset createAsset(AssetKind kind, String name, @Nullable String location, @Nullable String notes) {
        if (kind == AssetKind.KILN) {
            throw new InvalidInputException("Add kilns on the kilns page; they appear here by themselves.");
        }
        UUID id = Uuids.v7();
        try {
            jdbc.sql("""
                    insert into asset (id, studio_id, kind, name, location, notes, created_at, version)
                    values (:id, :studioId, :kind, :name, :location, :notes, :now, 0)
                    """)
                    .param("id", id)
                    .param("studioId", TenantContext.require().studioId())
                    .param("kind", kind.name())
                    .param("name", requireName(name))
                    .param("location", optional(location, 80, "location"))
                    .param("notes", optional(notes, 1000, "notes"))
                    .param("now", utc(now()))
                    .update();
        } catch (DuplicateKeyException e) {
            throw new InvalidInputException("There's already something called '%s'.".formatted(name.strip()));
        }
        return asset(id, now());
    }

    /** Changes the location and notes, and the name of anything but a kiln (which takes the kiln's). */
    @Transactional
    public Asset updateAsset(UUID assetId, @Nullable String name, @Nullable String location, @Nullable String notes,
            long expectedVersion) {
        AssetRow row = assetRow(assetId);
        String newName = row.kilnId() != null ? null : name == null ? row.name() : requireName(name);
        try {
            requireUpdated(jdbc.sql("""
                    update asset set name = :name, location = :location, notes = :notes, version = version + 1
                    where id = :id and version = :version and not archived
                    """)
                    .param("name", newName)
                    .param("location", optional(location, 80, "location"))
                    .param("notes", optional(notes, 1000, "notes"))
                    .param("id", assetId)
                    .param("version", expectedVersion)
                    .update());
        } catch (DuplicateKeyException e) {
            throw new InvalidInputException("There's already something called '%s'.".formatted(newName));
        }
        return asset(assetId, now());
    }

    /** Takes an asset out of service with a reason or, with no reason, puts it back. Not for kilns. */
    @Transactional
    public Asset setDown(UUID assetId, @Nullable String reason, long expectedVersion) {
        if (assetRow(assetId).kilnId() != null) {
            throw new ConflictException("Mark a kiln down on the kiln itself, where it also stops loads.");
        }
        requireUpdated(jdbc.sql("""
                update asset set down_reason = :reason, version = version + 1
                where id = :id and version = :version and not archived
                """)
                .param("reason", optional(reason, 200, "reason"))
                .param("id", assetId)
                .param("version", expectedVersion)
                .update());
        return asset(assetId, now());
    }

    /** Stops tracking an asset and its tasks. Its records are kept. Not for kilns. */
    @Transactional
    public void archiveAsset(UUID assetId) {
        if (assetRow(assetId).kilnId() != null) {
            throw new ConflictException("A kiln is tracked for as long as it exists.");
        }
        jdbc.sql("update asset set archived = true, version = version + 1 where id = :id").param("id", assetId).update();
    }

    @Transactional
    public Task createTask(UUID assetId, TaskSettings settings) {
        AssetRow asset = assetRow(assetId);
        TaskSettings valid = validate(settings, asset);
        Instant now = now();
        UUID id = Uuids.v7();
        jdbc.sql("""
                insert into maintenance_task (id, studio_id, asset_id, title, checklist, every_days, every_firings,
                                              assigned_duty, assigned_to, last_done_at, last_done_firings,
                                              created_at, version)
                values (:id, :studioId, :assetId, :title, :checklist, :everyDays, :everyFirings, :duty, :assignedTo,
                        :now, :firings, :now, 0)
                """)
                .param("id", id)
                .param("studioId", TenantContext.require().studioId())
                .param("assetId", assetId)
                .param("title", valid.title())
                .param("checklist", valid.checklist().toArray(String[]::new))
                .param("everyDays", valid.everyDays())
                .param("everyFirings", valid.everyFirings())
                .param("duty", valid.assignedDuty() == null ? null : valid.assignedDuty().name())
                .param("assignedTo", valid.assignedTo())
                .param("now", utc(now))
                .param("firings", firingsOf(asset))
                .update();
        return task(id, now);
    }

    /** Changes a task. Its clock carries on from when it was last done. */
    @Transactional
    public Task updateTask(UUID taskId, TaskSettings settings, long expectedVersion) {
        TaskSettings valid = validate(settings, assetRow(assetIdOf(taskId)));
        requireUpdated(jdbc.sql("""
                update maintenance_task set title = :title, checklist = :checklist, every_days = :everyDays,
                       every_firings = :everyFirings, assigned_duty = :duty, assigned_to = :assignedTo,
                       version = version + 1
                where id = :id and version = :version and not archived
                """)
                .param("title", valid.title())
                .param("checklist", valid.checklist().toArray(String[]::new))
                .param("everyDays", valid.everyDays())
                .param("everyFirings", valid.everyFirings())
                .param("duty", valid.assignedDuty() == null ? null : valid.assignedDuty().name())
                .param("assignedTo", valid.assignedTo())
                .param("id", taskId)
                .param("version", expectedVersion)
                .update());
        return task(taskId, now());
    }

    @Transactional
    public void archiveTask(UUID taskId) {
        assetIdOf(taskId);
        jdbc.sql("update maintenance_task set archived = true, version = version + 1 where id = :id")
                .param("id", taskId).update();
    }

    /**
     * Records a task as done and restarts its clock, and only its clock. Staff can complete any
     * task; anyone else only one assigned to them, by name or through a duty they hold.
     */
    @Transactional
    public Task complete(UUID taskId, @Nullable String note, @Nullable BigDecimal cost, List<String> checked) {
        Instant now = now();
        Task task = task(taskId, now);
        requireMayComplete(task);
        Set<String> lines = new HashSet<>(task.checklist());
        List<String> ticked = checked.stream().map(String::strip).distinct().toList();
        if (!lines.containsAll(ticked)) {
            throw new InvalidInputException("Only lines from the task's checklist can be ticked.");
        }
        AssetRow asset = assetRow(task.assetId());
        Integer firings = asset.kilnId() == null ? null : firingsOf(asset);
        insertRecord(asset.id(), taskId, note, cost, ticked, firings, now);
        jdbc.sql("""
                update maintenance_task set last_done_at = :now, last_done_firings = :firings, version = version + 1
                where id = :id
                """)
                .param("now", utc(now))
                .param("firings", firings == null ? 0 : firings)
                .param("id", taskId)
                .update();
        return task(taskId, now);
    }

    /**
     * Logs work done outside any task, such as a repair. No task's clock restarts. Each part used
     * is taken out of supplies' stock, so the history says which spare went in and the shelf count
     * stays right.
     */
    @Transactional
    public Record recordRepair(UUID assetId, String note, @Nullable BigDecimal cost, List<PartUse> parts) {
        if (note.isBlank()) {
            throw new InvalidInputException("Describe the work.");
        }
        if (parts.size() > 20 || parts.stream().map(PartUse::itemId).distinct().count() != parts.size()) {
            throw new InvalidInputException("List each part once, up to 20 parts.");
        }
        AssetRow asset = assetRow(assetId);
        UUID id = insertRecord(assetId, null, note, cost, List.of(),
                asset.kilnId() == null ? null : firingsOf(asset), now());
        String movementNote = "Repair: " + nameOf(asset, kilnsById());
        for (PartUse part : parts) {
            supplies.recordMovement(part.itemId(), MovementKind.USED, part.quantity(),
                    movementNote.length() > 200 ? movementNote.substring(0, 200) : movementNote);
            jdbc.sql("""
                    insert into maintenance_record_part (record_id, studio_id, item_id, quantity)
                    values (:recordId, :studioId, :itemId, :quantity)
                    """)
                    .param("recordId", id)
                    .param("studioId", TenantContext.require().studioId())
                    .param("itemId", part.itemId())
                    .param("quantity", part.quantity())
                    .update();
        }
        return records(assetId).stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    /** The asset's history, newest first. */
    @Transactional(readOnly = true)
    public List<Record> records(UUID assetId) {
        assetRow(assetId);
        List<Record> rows = jdbc.sql("""
                select r.id, r.asset_id, r.task_id, t.title, r.note, r.cost, r.checked, r.at_firing, r.done_by, r.done_at
                from maintenance_record r left join maintenance_task t on t.id = r.task_id
                where r.asset_id = :assetId order by r.done_at desc, r.id desc limit 500
                """)
                .param("assetId", assetId)
                .query((rs, n) -> new Record(rs.getObject("id", UUID.class), rs.getObject("asset_id", UUID.class),
                        rs.getObject("task_id", UUID.class), rs.getString("title"), rs.getString("note"),
                        rs.getBigDecimal("cost"), strings(rs.getArray("checked")),
                        rs.getObject("at_firing", Integer.class), rs.getObject("done_by", UUID.class), null,
                        rs.getObject("done_at", OffsetDateTime.class).toInstant(), List.of()))
                .list();
        Map<UUID, String> names = accounts.displayNames(rows.stream().map(Record::doneBy).toList());
        Map<UUID, List<PartUsed>> parts = partsOf(assetId);
        return rows.stream()
                .map(r -> new Record(r.id(), r.assetId(), r.taskId(), r.taskTitle(), r.note(), r.cost(), r.checked(),
                        r.atFiring(), r.doneBy(), names.get(r.doneBy()), r.doneAt(),
                        parts.getOrDefault(r.id(), List.of())))
                .toList();
    }

    /** Published tasks for this kind of asset that it doesn't have yet (matched by title). */
    @Transactional(readOnly = true)
    public List<StarterTasks.Suggestion> suggestions(UUID assetId) {
        AssetRow asset = assetRow(assetId);
        Set<String> existing = jdbc.sql("select lower(title) from maintenance_task where asset_id = :id and not archived")
                .param("id", assetId).query(String.class).set();
        KilnView kiln = asset.kilnId() == null ? null : kilns.get(asset.kilnId());
        return StarterTasks.forKind(asset.kind(), kiln == null ? null : kiln.kind()).stream()
                .filter(s -> !existing.contains(s.task().title().toLowerCase(Locale.ROOT)))
                .toList();
    }

    /**
     * Every record done on or between two dates in the studio's time zone, oldest first, as CSV:
     * the history an insurer or landlord asks for.
     */
    @Transactional(readOnly = true)
    public String recordsCsv(LocalDate from, LocalDate to, ZoneId zone) {
        if (to.isBefore(from) || from.plusYears(10).isBefore(to)) {
            throw new InvalidInputException("Choose a range of up to ten years, with 'from' on or before 'to'.");
        }
        Map<UUID, String> assetNames = assets(now()).stream().collect(Collectors.toMap(Asset::id, Asset::name));
        record Row(UUID assetId, @Nullable String archivedName, @Nullable String taskTitle, @Nullable String note,
                @Nullable BigDecimal cost, List<String> checked, @Nullable Integer atFiring, UUID doneBy,
                Instant doneAt) {
        }
        List<Row> rows = jdbc.sql("""
                select r.asset_id, a.name, t.title, r.note, r.cost, r.checked, r.at_firing, r.done_by, r.done_at
                from maintenance_record r
                join asset a on a.id = r.asset_id
                left join maintenance_task t on t.id = r.task_id
                where r.done_at >= :from and r.done_at < :to
                order by r.done_at, r.id
                """)
                .param("from", utc(from.atStartOfDay(zone).toInstant()))
                .param("to", utc(to.plusDays(1).atStartOfDay(zone).toInstant()))
                .query((rs, n) -> new Row(rs.getObject("asset_id", UUID.class), rs.getString("name"),
                        rs.getString("title"), rs.getString("note"), rs.getBigDecimal("cost"),
                        strings(rs.getArray("checked")), rs.getObject("at_firing", Integer.class),
                        rs.getObject("done_by", UUID.class), rs.getObject("done_at", OffsetDateTime.class).toInstant()))
                .list();
        Map<UUID, String> names = accounts.displayNames(rows.stream().map(Row::doneBy).toList());
        Csv csv = new Csv().row(List.of("Date", "Asset", "Task", "Note", "Cost", "Checked", "At firing", "Done by"));
        for (Row row : rows) {
            // Archived assets keep their records; a kiln's name lives on the kiln
            String asset = assetNames.getOrDefault(row.assetId(), row.archivedName() == null ? "Kiln" : row.archivedName());
            csv.row(Arrays.asList(row.doneAt().atZone(zone).toLocalDate().toString(), asset,
                    row.taskTitle() == null ? "Repair or other work" : row.taskTitle(), row.note(), row.cost(),
                    String.join("; ", row.checked()), row.atFiring(), names.get(row.doneBy())));
        }
        return csv.toString();
    }

    /**
     * Reminds whoever a task is assigned to when it comes due or nearly so, once per cycle: a
     * {@link MaintenanceDue} for each task in the bound studio not reminded about since it was
     * last done. Run by {@link MaintenanceReminderSweep}. Returns how many tasks were reminded.
     */
    @Transactional
    public int remindDue(Instant now) {
        Map<UUID, Instant> unreminded = jdbc.sql("""
                select id, last_done_at from maintenance_task
                where not archived and reminded_for is distinct from last_done_at
                """)
                .query((rs, n) -> Map.entry(rs.getObject("id", UUID.class),
                        rs.getObject("last_done_at", OffsetDateTime.class).toInstant()))
                .list().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        List<Task> due = unreminded.isEmpty() ? List.of()
                : dueTasks(now).stream().filter(t -> unreminded.containsKey(t.id())).toList();
        if (due.isEmpty()) {
            return 0;
        }
        UUID studioId = TenantContext.require().studioId();
        Map<UUID, Set<Duty>> duties = access.allDuties();
        List<UUID> staff = members.list().stream().filter(m -> m.role().includes(StudioRole.STAFF))
                .map(MemberView::accountId).toList();
        for (Task task : due) {
            events.publishEvent(new MaintenanceDue(studioId, task.id(), task.assetName(), task.title(), task.status(),
                    task.dueAt(), task.firingsLeft(), task.checklist(), recipients(task, duties, staff)));
            // Only this cycle: if the task was done meanwhile, its new cycle still gets its reminder
            jdbc.sql("update maintenance_task set reminded_for = last_done_at where id = :id and last_done_at = :cycle")
                    .param("id", task.id())
                    .param("cycle", utc(unreminded.get(task.id())))
                    .update();
        }
        return due.size();
    }

    /** The named assignee; else everyone holding the duty; else, with nobody to do it, the staff. */
    private static List<UUID> recipients(Task task, Map<UUID, Set<Duty>> duties, List<UUID> staff) {
        if (task.assignedTo() != null) {
            return List.of(task.assignedTo());
        }
        if (task.assignedDuty() != null) {
            List<UUID> holders = duties.entrySet().stream().filter(e -> e.getValue().contains(task.assignedDuty()))
                    .map(Map.Entry::getKey).sorted().toList();
            if (!holders.isEmpty()) {
                return holders;
            }
        }
        return staff;
    }

    /** Every kiln is maintained, so a new one gets its asset in the same transaction. */
    @EventListener
    void onKilnCreated(KilnCreated kiln) {
        jdbc.sql("""
                insert into asset (id, studio_id, kind, kiln_id, created_at, version)
                values (:id, :studioId, 'KILN', :kilnId, :now, 0)
                """)
                .param("id", Uuids.v7())
                .param("studioId", kiln.studioId())
                .param("kilnId", kiln.kilnId())
                .param("now", utc(now()))
                .update();
    }

    // --- Reading ---------------------------------------------------------------------------------

    /** The parts used in each of the asset's records, named as supplies names them. */
    private Map<UUID, List<PartUsed>> partsOf(UUID assetId) {
        record Row(UUID recordId, UUID itemId, BigDecimal quantity) {
        }
        List<Row> rows = jdbc.sql("""
                select p.record_id, p.item_id, p.quantity
                from maintenance_record_part p join maintenance_record r on r.id = p.record_id
                where r.asset_id = :assetId
                """)
                .param("assetId", assetId)
                .query((rs, n) -> new Row(rs.getObject("record_id", UUID.class), rs.getObject("item_id", UUID.class),
                        rs.getBigDecimal("quantity")))
                .list();
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Item> items = supplies.items().stream().collect(Collectors.toMap(Item::id, Function.identity()));
        return rows.stream().collect(Collectors.groupingBy(Row::recordId, Collectors.mapping(row -> {
            Item item = items.get(row.itemId());
            // An archived item drops out of the list; its part still counts
            return new PartUsed(row.itemId(), item == null ? "A retired supply" : item.name(),
                    item == null ? "" : item.unit(), row.quantity().stripTrailingZeros());
        }, Collectors.toList())));
    }

    private Task task(UUID taskId, Instant now) {
        AssetRow asset = assetRow(assetIdOf(taskId));
        return tasks(SELECT_TASK + "and t.id = :taskId", Map.of("taskId", taskId), List.of(asset), kilnsById(), now)
                .getFirst();
    }

    private List<Task> tasks(String sql, Map<String, Object> params, Collection<AssetRow> assets,
            Map<UUID, KilnView> kilnsById, Instant now) {
        List<TaskRow> rows = jdbc.sql(sql).params(params).query(MaintenanceService::taskRow).list();
        Map<UUID, AssetRow> assetsById = assets.stream().collect(Collectors.toMap(AssetRow::id, Function.identity()));
        Map<UUID, String> names = accounts.displayNames(
                rows.stream().map(TaskRow::assignedTo).filter(id -> id != null).toList());
        return rows.stream()
                .filter(row -> assetsById.containsKey(row.assetId()))
                .map(row -> {
                    AssetRow asset = assetsById.get(row.assetId());
                    KilnView kiln = asset.kilnId() == null ? null : kilnsById.get(asset.kilnId());
                    DueRule.Due due = DueRule.evaluate(row.everyDays(), row.everyFirings(), row.lastDoneAt(),
                            row.lastDoneFirings(), kiln == null ? null : kiln.totalFirings(), now);
                    return new Task(row.id(), row.assetId(), nameOf(asset, kilnsById), row.title(), row.checklist(),
                            row.everyDays(), row.everyFirings(), row.assignedDuty(), row.assignedTo(),
                            row.assignedTo() == null ? null : names.get(row.assignedTo()), row.lastDoneAt(),
                            due.dueAt(), due.firingsLeft(), due.status(), row.version());
                })
                .sorted(URGENCY)
                .toList();
    }

    /** Most urgent first; among equals, the one whose days run out first, then the fewest firings left. */
    private static final Comparator<Task> URGENCY = Comparator.comparing(Task::status).reversed()
            .thenComparing(Task::dueAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Task::firingsLeft, Comparator.nullsLast(Comparator.naturalOrder()));

    private Asset toAsset(AssetRow row, Map<UUID, KilnView> kilnsById, List<Task> tasks) {
        KilnView kiln = row.kilnId() == null ? null : kilnsById.get(row.kilnId());
        return new Asset(row.id(), row.kind(), nameOf(row, kilnsById), row.kilnId(), row.location(), row.notes(),
                kiln == null ? row.downReason() : kiln.downReason(), kiln == null ? null : kiln.totalFirings(),
                row.version(), tasks);
    }

    private static String nameOf(AssetRow row, Map<UUID, KilnView> kilnsById) {
        if (row.kilnId() == null) {
            return row.name();
        }
        KilnView kiln = kilnsById.get(row.kilnId());
        return kiln == null ? "Kiln" : kiln.name();
    }

    private Map<UUID, KilnView> kilnsById() {
        return kilns.list().stream().collect(Collectors.toMap(KilnView::id, Function.identity()));
    }

    private int firingsOf(AssetRow asset) {
        return asset.kilnId() == null ? 0 : kilns.get(asset.kilnId()).totalFirings();
    }

    private AssetRow assetRow(UUID assetId) {
        return jdbc.sql("""
                select id, kind, kiln_id, name, location, notes, down_reason, version from asset
                where id = :id and not archived
                """)
                .param("id", assetId)
                .query(MaintenanceService::assetRow).optional()
                .orElseThrow(() -> new NotFoundException("No such asset."));
    }

    private UUID assetIdOf(UUID taskId) {
        return jdbc.sql("""
                select t.asset_id from maintenance_task t join asset a on a.id = t.asset_id
                where t.id = :id and not t.archived and not a.archived
                """)
                .param("id", taskId)
                .query(UUID.class).optional()
                .orElseThrow(() -> new NotFoundException("No such task."));
    }

    // --- Writing ---------------------------------------------------------------------------------

    private UUID insertRecord(UUID assetId, @Nullable UUID taskId, @Nullable String note, @Nullable BigDecimal cost,
            List<String> checked, @Nullable Integer atFiring, Instant now) {
        if (cost != null && cost.signum() < 0) {
            throw new InvalidInputException("A cost can't be negative.");
        }
        TenantContext caller = TenantContext.require();
        UUID id = Uuids.v7();
        jdbc.sql("""
                insert into maintenance_record (id, studio_id, asset_id, task_id, note, cost, checked, at_firing,
                                                done_by, done_at)
                values (:id, :studioId, :assetId, :taskId, :note, :cost, :checked, :atFiring, :by, :now)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("assetId", assetId)
                .param("taskId", taskId)
                .param("note", optional(note, 1000, "note"))
                .param("cost", cost)
                .param("checked", checked.toArray(String[]::new))
                .param("atFiring", atFiring)
                .param("by", caller.accountId())
                .param("now", utc(now))
                .update();
        return id;
    }

    private void requireMayComplete(Task task) {
        TenantContext caller = TenantContext.require();
        StudioRole role = caller.role();
        if (role != null && role.includes(StudioRole.STAFF)) {
            return;
        }
        boolean mine = caller.accountId().equals(task.assignedTo())
                || (task.assignedDuty() != null && access.dutiesOf(caller.accountId()).contains(task.assignedDuty()));
        if (!mine) {
            throw new AccessDeniedException("Only staff, or whoever this task is assigned to, can complete it.");
        }
    }

    private TaskSettings validate(TaskSettings settings, AssetRow asset) {
        String title = settings.title().strip();
        if (title.isEmpty() || title.length() > 120) {
            throw new InvalidInputException("A task needs a title of 1 to 120 characters.");
        }
        List<String> checklist = settings.checklist().stream().map(String::strip).filter(s -> !s.isEmpty())
                .distinct().toList();
        if (checklist.size() > 30 || checklist.stream().anyMatch(s -> s.length() > 200)) {
            throw new InvalidInputException("A checklist has at most 30 lines of up to 200 characters.");
        }
        if (settings.everyDays() == null && settings.everyFirings() == null) {
            throw new InvalidInputException("Say how often: every so many days, firings, or both.");
        }
        if (settings.everyFirings() != null && asset.kilnId() == null) {
            throw new InvalidInputException("Only kilns count firings. Use days for this one.");
        }
        if (settings.assignedDuty() != null && settings.assignedTo() != null) {
            throw new InvalidInputException("Assign the task to a duty or to a person, not both.");
        }
        if (settings.assignedTo() != null && members.roleOf(settings.assignedTo()).isEmpty()) {
            throw new InvalidInputException("The task can only be assigned to someone in the studio.");
        }
        return new TaskSettings(title, checklist, settings.everyDays(), settings.everyFirings(),
                settings.assignedDuty(), settings.assignedTo());
    }

    private static String requireName(String name) {
        String cleaned = name.strip();
        if (cleaned.isEmpty() || cleaned.length() > 80) {
            throw new InvalidInputException("A name of 1 to 80 characters is needed.");
        }
        return cleaned;
    }

    private static @Nullable String optional(@Nullable String value, int max, String what) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.strip();
        if (cleaned.length() > max) {
            throw new InvalidInputException("Keep the %s to %d characters.".formatted(what, max));
        }
        return cleaned;
    }

    private static void requireUpdated(int updated) {
        if (updated == 0) {
            throw new ConflictException("Someone else changed this at the same time. Reload and try again.");
        }
    }

    // --- Rows ------------------------------------------------------------------------------------

    private record AssetRow(UUID id, AssetKind kind, @Nullable UUID kilnId, @Nullable String name,
            @Nullable String location, @Nullable String notes, @Nullable String downReason, long version) {
    }

    private record TaskRow(UUID id, UUID assetId, String title, List<String> checklist, @Nullable Integer everyDays,
            @Nullable Integer everyFirings, @Nullable Duty assignedDuty, @Nullable UUID assignedTo, Instant lastDoneAt,
            int lastDoneFirings, long version) {
    }

    private static AssetRow assetRow(ResultSet rs, int row) throws SQLException {
        return new AssetRow(rs.getObject("id", UUID.class), AssetKind.valueOf(rs.getString("kind")),
                rs.getObject("kiln_id", UUID.class), rs.getString("name"), rs.getString("location"),
                rs.getString("notes"), rs.getString("down_reason"), rs.getLong("version"));
    }

    private static TaskRow taskRow(ResultSet rs, int row) throws SQLException {
        String duty = rs.getString("assigned_duty");
        return new TaskRow(rs.getObject("id", UUID.class), rs.getObject("asset_id", UUID.class), rs.getString("title"),
                strings(rs.getArray("checklist")), rs.getObject("every_days", Integer.class),
                rs.getObject("every_firings", Integer.class), duty == null ? null : Duty.valueOf(duty),
                rs.getObject("assigned_to", UUID.class), rs.getObject("last_done_at", OffsetDateTime.class).toInstant(),
                rs.getInt("last_done_firings"), rs.getLong("version"));
    }

    private static List<String> strings(@Nullable Array array) throws SQLException {
        return array == null ? List.of() : Arrays.asList((String[]) array.getArray());
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

}
