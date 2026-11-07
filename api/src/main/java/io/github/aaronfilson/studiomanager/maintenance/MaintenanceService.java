package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.access.AccessService;
import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.kiln.KilnService;
import io.github.aaronfilson.studiomanager.kiln.KilnView;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Asset;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.AssetKind;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Task;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What's maintained in the bound studio, and what's due: the reads other modules and the controller
 * use. Changes are in {@link AssetChanges}, {@link TaskChanges} and {@link MaintenanceRecords};
 * reminders in {@link MaintenanceReminders}.
 *
 * <p>Each task keeps its own clock, the time and the kiln's firing count when it was last done, so
 * completing one task never restarts another. Kilns are assets like any other, except that their
 * name, down state and firing count come from the kiln module.
 *
 * <p>Every list here is a fixed number of queries: the assets, the tasks with their assets, the
 * kilns, and the names of the people assigned.
 */
@Service
public class MaintenanceService {

  /**
   * Most urgent first; among equals, the one whose days run out first, then the fewest firings
   * left.
   */
  static final Comparator<Task> URGENCY =
      Comparator.comparing(Task::status)
          .reversed()
          .thenComparing(MaintenanceService::dueAtOrLatest)
          .thenComparingInt(MaintenanceService::firingsLeftOrMost);

  private final MaintainedAssetRepository assets;
  private final MaintenanceTaskRepository tasks;
  private final KilnService kilns;
  private final AccessService access;
  private final AccountDirectory accounts;
  private final Clock clock;

  MaintenanceService(
      MaintainedAssetRepository assets,
      MaintenanceTaskRepository tasks,
      KilnService kilns,
      AccessService access,
      AccountDirectory accounts,
      Clock clock) {
    this.assets = assets;
    this.tasks = tasks;
    this.kilns = kilns;
    this.access = access;
    this.accounts = accounts;
    this.clock = clock;
  }

  /** Everything maintained, kilns first and then by name, each with its tasks soonest due first. */
  @Transactional(readOnly = true)
  public List<Asset> assets(Instant now) {
    List<MaintainedAsset> tracked = assets.findTracked();
    Map<UUID, KilnView> kilnsById = kilnsById();
    Map<UUID, List<Task>> tasksByAsset =
        taskViews(tasks.findTracked(), kilnsById, now).stream()
            .collect(Collectors.groupingBy(Task::assetId));
    return tracked.stream()
        .map(asset ->
            assetView(
                asset, kilnOf(asset, kilnsById), tasksByAsset.getOrDefault(asset.id(), List.of())))
        .sorted(
            Comparator.comparing((Asset asset) -> asset.kind() != AssetKind.KILN)
                .thenComparing(Asset::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @Transactional(readOnly = true)
  public Asset asset(UUID assetId, Instant now) {
    return assets(now).stream()
        .filter(asset -> asset.id().equals(assetId))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("No such asset."));
  }

  /**
   * Why each out-of-service asset is down, by asset id; kilns included, from the kiln. Assets in
   * service aren't in the map. For the calendar, which won't book a down asset.
   */
  @Transactional(readOnly = true)
  public Map<UUID, String> downReasons() {
    Map<UUID, KilnView> kilnsById = kilnsById();
    Map<UUID, String> down = new HashMap<>();
    for (MaintainedAsset asset : assets.findTracked()) {
      KilnView kiln = kilnOf(asset, kilnsById);
      String reason = kiln == null ? asset.downReason() : kiln.downReason();
      if (reason != null) {
        down.put(asset.id(), reason);
      }
    }
    return down;
  }

  /** The asset that is this kiln. Every kiln has one, from when it was added. */
  @Transactional(readOnly = true)
  public Optional<UUID> assetOfKiln(UUID kilnId) {
    return assets.findIdOfKiln(kilnId);
  }

  /** Tasks due or due soon on any asset, most urgent first: what Today shows staff. */
  @Transactional(readOnly = true)
  public List<Task> dueTasks(Instant now) {
    return taskViews(tasks.findTracked(), kilnsById(), now).stream()
        .filter(task -> task.status() != DueStatus.OK)
        .sorted(URGENCY)
        .toList();
  }

  /** Tasks assigned to the caller by name or through a duty they hold, most urgent first. */
  @Transactional(readOnly = true)
  public List<Task> myTasks(Instant now) {
    UUID me = TenantContext.require().accountId();
    Set<Duty> duties = access.dutiesOf(me);
    return taskViews(tasks.findTracked(), kilnsById(), now).stream()
        .filter(task -> me.equals(task.assignedTo()) || holdsDuty(duties, task.assignedDuty()))
        .sorted(URGENCY)
        .toList();
  }

  /** Published tasks for this kind of asset that it doesn't have yet (matched by title). */
  @Transactional(readOnly = true)
  public List<StarterTasks.Suggestion> suggestions(UUID assetId) {
    MaintainedAsset asset = findAsset(assetId);
    Set<String> existing = Set.copyOf(tasks.findTitlesInLowerCase(assetId));
    UUID kilnId = asset.kilnId();
    KilnView.KilnType kilnType = kilnId == null ? null : kilns.get(kilnId).kind();
    return StarterTasks.forKind(asset.kind(), kilnType).stream()
        .filter(
            suggestion -> !existing.contains(suggestion.task().title().toLowerCase(Locale.ROOT)))
        .toList();
  }

  // --- For the other services in this package ---------------------------------------------------

  /** A tracked asset, or not found. */
  MaintainedAsset findAsset(UUID assetId) {
    return assets.findTracked(assetId).orElseThrow(() -> new NotFoundException("No such asset."));
  }

  /** A tracked task on a tracked asset, or not found. */
  MaintenanceTask findTask(UUID taskId) {
    return tasks.findTracked(taskId).orElseThrow(() -> new NotFoundException("No such task."));
  }

  /** One task as the API shows it. */
  Task view(MaintenanceTask task) {
    MaintainedAsset asset = task.asset();
    UUID kilnId = asset.kilnId();
    Map<UUID, KilnView> kiln = kilnId == null ? Map.of() : Map.of(kilnId, kilns.get(kilnId));
    return taskViews(List.of(task), kiln, clock.instant()).getFirst();
  }

  /** The kiln's total firings now, for an asset that is a kiln; null for anything else. */
  @Nullable Integer firingsNow(MaintainedAsset asset) {
    UUID kilnId = asset.kilnId();
    return kilnId == null ? null : kilns.get(kilnId).totalFirings();
  }

  /** The asset's name: its own, or for a kiln the kiln's. */
  String nameOf(MaintainedAsset asset) {
    UUID kilnId = asset.kilnId();
    return nameOf(asset, kilnId == null ? null : kilns.get(kilnId));
  }

  /** The name of each tracked asset, by id: two queries, with no tasks loaded. */
  Map<UUID, String> trackedNames() {
    Map<UUID, KilnView> kilnsById = kilnsById();
    return assets.findTracked().stream()
        .collect(
            Collectors.toMap(
                MaintainedAsset::id, asset -> nameOf(asset, kilnOf(asset, kilnsById))));
  }

  // --- Views -----------------------------------------------------------------------------------

  private List<Task> taskViews(
      List<MaintenanceTask> found, Map<UUID, KilnView> kilnsById, Instant now) {
    Map<UUID, String> names =
        accounts.displayNames(
            found.stream().flatMap(task -> Stream.ofNullable(task.assignedTo())).toList());
    List<Task> views = new ArrayList<>(found.size());
    for (MaintenanceTask task : found) {
      MaintainedAsset asset = task.asset();
      KilnView kiln = kilnOf(asset, kilnsById);
      DueRule.Due due =
          DueRule.evaluate(
              task.everyDays(),
              task.everyFirings(),
              task.lastDoneAt(),
              task.lastDoneFirings(),
              kiln == null ? null : kiln.totalFirings(),
              now);
      UUID assignedTo = task.assignedTo();
      views.add(
          new Task(
              task.id(),
              asset.id(),
              nameOf(asset, kiln),
              task.title(),
              task.checklist(),
              task.everyDays(),
              task.everyFirings(),
              task.assignedDuty(),
              assignedTo,
              assignedTo == null ? null : names.get(assignedTo),
              task.lastDoneAt(),
              due.dueAt(),
              due.firingsLeft(),
              due.status(),
              task.version()));
    }
    views.sort(URGENCY);
    return views;
  }

  private static Asset assetView(MaintainedAsset asset, @Nullable KilnView kiln, List<Task> tasks) {
    return new Asset(
        asset.id(),
        asset.kind(),
        nameOf(asset, kiln),
        asset.kilnId(),
        asset.location(),
        asset.notes(),
        kiln == null ? asset.downReason() : kiln.downReason(),
        kiln == null ? null : kiln.totalFirings(),
        asset.version(),
        tasks);
  }

  private static String nameOf(MaintainedAsset asset, @Nullable KilnView kiln) {
    String ownName = asset.name();
    if (ownName != null) {
      return ownName;
    }
    return kiln == null ? "Kiln" : kiln.name();
  }

  private static @Nullable KilnView kilnOf(MaintainedAsset asset, Map<UUID, KilnView> kilnsById) {
    UUID kilnId = asset.kilnId();
    return kilnId == null ? null : kilnsById.get(kilnId);
  }

  private Map<UUID, KilnView> kilnsById() {
    return kilns.list().stream().collect(Collectors.toMap(KilnView::id, Function.identity()));
  }

  private static boolean holdsDuty(Set<Duty> duties, @Nullable Duty duty) {
    return duty != null && duties.contains(duty);
  }

  private static Instant dueAtOrLatest(Task task) {
    return Objects.requireNonNullElse(task.dueAt(), Instant.MAX);
  }

  private static int firingsLeftOrMost(Task task) {
    return Objects.requireNonNullElse(task.firingsLeft(), Integer.MAX_VALUE);
  }
}
