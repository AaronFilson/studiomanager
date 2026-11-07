package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.access.AccessService;
import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Task;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.RolePermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MemberView;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reminds whoever a task is assigned to when it comes due or nearly so, once per cycle. Run in each
 * studio by {@link MaintenanceReminderSweep}.
 */
@Service
class MaintenanceReminders {

  private final MaintenanceTaskRepository tasks;
  private final MaintenanceService reads;
  private final MaintenanceQueries queries;
  private final AccessService access;
  private final StudioMembers members;
  private final ApplicationEventPublisher events;
  private final RolePermissions rolePermissions;

  MaintenanceReminders(
      MaintenanceTaskRepository tasks,
      MaintenanceService reads,
      MaintenanceQueries queries,
      AccessService access,
      StudioMembers members,
      ApplicationEventPublisher events,
      RolePermissions rolePermissions) {
    this.tasks = tasks;
    this.reads = reads;
    this.queries = queries;
    this.access = access;
    this.members = members;
    this.events = events;
    this.rolePermissions = rolePermissions;
  }

  /**
   * Publishes a {@link MaintenanceDue} for each task in the bound studio that is due or due soon at
   * {@code now} and hasn't been reminded about since it was last done. Returns how many.
   */
  @Transactional
  public int remindDue(Instant now) {
    Map<UUID, Instant> unreminded =
        tasks.findTracked().stream()
            .filter(task -> !task.remindedThisCycle())
            .collect(Collectors.toMap(MaintenanceTask::id, MaintenanceTask::lastDoneAt));
    List<Task> due =
        unreminded.isEmpty()
            ? List.of()
            : reads.dueTasks(now).stream()
                .filter(task -> unreminded.containsKey(task.id()))
                .toList();
    if (due.isEmpty()) {
      return 0;
    }
    UUID studioId = TenantContext.require().requireStudioId();
    Map<UUID, Set<Duty>> duties = access.allDuties();
    List<UUID> staff =
        members.list().stream()
            .filter(
                member -> rolePermissions.roleHolds(member.role(), Permission.MAINTENANCE_MANAGE))
            .map(MemberView::accountId)
            .toList();
    // Nothing here is changed through Hibernate, but the claim is SQL: flush by the rule (ADR 21)
    tasks.flush();
    int reminded = 0;
    for (Task task : due) {
      Instant cycle = unreminded.get(task.id());
      if (cycle != null && queries.claimReminder(task.id(), cycle)) {
        events.publishEvent(
            new MaintenanceDue(
                studioId,
                task.id(),
                task.assetName(),
                task.title(),
                task.status(),
                task.dueAt(),
                task.firingsLeft(),
                task.checklist(),
                recipients(task, duties, staff)));
        reminded++;
      }
    }
    return reminded;
  }

  /** The named assignee; else everyone holding the duty; else, with nobody to do it, the staff. */
  private static List<UUID> recipients(Task task, Map<UUID, Set<Duty>> duties, List<UUID> staff) {
    UUID assignedTo = task.assignedTo();
    if (assignedTo != null) {
      return List.of(assignedTo);
    }
    Duty duty = task.assignedDuty();
    if (duty != null) {
      List<UUID> holders =
          duties.entrySet().stream()
              .filter(entry -> entry.getValue().contains(duty))
              .map(Map.Entry::getKey)
              .sorted()
              .toList();
      if (!holders.isEmpty()) {
        return holders;
      }
    }
    return staff;
  }
}
