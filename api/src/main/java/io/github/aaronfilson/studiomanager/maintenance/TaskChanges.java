package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Task;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.TaskSettings;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Setting up, changing and archiving the recurring tasks on an asset. Public: an approved assistant
 * suggestion (the proposal module) sets a task up through it.
 */
@Service
public class TaskChanges {

  private final MaintenanceTaskRepository tasks;
  private final MaintenanceService reads;
  private final StudioMembers members;
  private final Clock clock;

  TaskChanges(
      MaintenanceTaskRepository tasks,
      MaintenanceService reads,
      StudioMembers members,
      Clock clock) {
    this.tasks = tasks;
    this.reads = reads;
    this.members = members;
    this.clock = clock;
  }

  /** A new task, whose clock starts now. */
  @Transactional
  public Task createTask(UUID assetId, TaskSettings settings) {
    MaintainedAsset asset = reads.findAsset(assetId);
    TaskSettings valid = validate(settings, asset);
    MaintenanceTask task =
        new MaintenanceTask(
            asset, valid, clock.instant(), Objects.requireNonNullElse(reads.firingsNow(asset), 0));
    tasks.saveAndFlush(task);
    return reads.view(task);
  }

  /** Changes a task. Its clock carries on from when it was last done. */
  @Transactional
  public Task updateTask(UUID taskId, TaskSettings settings, long expectedVersion) {
    MaintenanceTask task = reads.findTask(taskId);
    TaskSettings valid = validate(settings, task.asset());
    if (task.version() != expectedVersion) {
      throw MaintenanceText.staleVersion();
    }
    task.change(valid);
    tasks.flush();
    return reads.view(task);
  }

  @Transactional
  public void archiveTask(UUID taskId) {
    reads.findTask(taskId).archive();
    tasks.flush();
  }

  private TaskSettings validate(TaskSettings settings, MaintainedAsset asset) {
    String title = settings.title().strip();
    if (title.isEmpty() || title.length() > 120) {
      throw new InvalidInputException("A task needs a title of 1 to 120 characters.");
    }
    List<String> checklist =
        settings.checklist().stream()
            .map(String::strip)
            .filter(line -> !line.isEmpty())
            .distinct()
            .toList();
    if (checklist.size() > 30 || checklist.stream().anyMatch(line -> line.length() > 200)) {
      throw new InvalidInputException("A checklist has at most 30 lines of up to 200 characters.");
    }
    if (settings.everyDays() == null && settings.everyFirings() == null) {
      throw new InvalidInputException("Say how often: every so many days, firings, or both.");
    }
    if (settings.everyFirings() != null && !asset.isKiln()) {
      throw new InvalidInputException("Only kilns count firings. Use days for this one.");
    }
    if (settings.assignedDuty() != null && settings.assignedTo() != null) {
      throw new InvalidInputException("Assign the task to a duty or to a person, not both.");
    }
    UUID assignedTo = settings.assignedTo();
    if (assignedTo != null && members.roleOf(assignedTo).isEmpty()) {
      throw new InvalidInputException("The task can only be assigned to someone in the studio.");
    }
    return new TaskSettings(
        title,
        checklist,
        settings.everyDays(),
        settings.everyFirings(),
        settings.assignedDuty(),
        assignedTo);
  }
}
