package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.access.AccessService;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceService;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.UniqueViolations;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Resource;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.ResourceKind;
import java.time.Clock;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adding things to the calendar. */
@Service
class ResourceChanges {

  private final BookableResourceRepository resources;
  private final ScheduleService reads;
  private final AccessService access;
  private final MaintenanceService maintenance;
  private final Clock clock;
  private final StudioPermissions permissions;

  ResourceChanges(
      BookableResourceRepository resources,
      ScheduleService reads,
      AccessService access,
      MaintenanceService maintenance,
      Clock clock,
      StudioPermissions permissions) {
    this.resources = resources;
    this.reads = reads;
    this.access = access;
    this.maintenance = maintenance;
    this.clock = clock;
    this.permissions = permissions;
  }

  /**
   * Adds something bookable. Linking it to a maintained asset means it can't be booked while the
   * asset is out of service, except for its repair or a closure.
   */
  @Transactional
  public Resource createResource(
      String name,
      ResourceKind kind,
      int capacity,
      @Nullable UUID requiredCertification,
      @Nullable UUID assetId) {
    permissions.require(Permission.SCHEDULE_MANAGE, ScheduleService.ONLY_STAFF);
    String cleaned = validName(name);
    if (capacity < 1 || capacity > 200) {
      throw new InvalidInputException("Capacity is 1 to 200.");
    }
    if (requiredCertification != null) {
      access.type(requiredCertification);
    }
    if (assetId != null) {
      // Not found for an asset of another studio or one no longer tracked
      maintenance.asset(assetId, clock.instant());
    }
    BookableResource resource =
        new BookableResource(cleaned, kind, capacity, requiredCertification, assetId);
    try {
      resources.saveAndFlush(resource);
    } catch (DataIntegrityViolationException e) {
      if (UniqueViolations.isUniqueViolation(e)) {
        throw new InvalidInputException(
            "There's already something called '%s' on the calendar.".formatted(cleaned));
      }
      throw e;
    }
    UUID id = resource.id();
    return reads.resources().stream()
        .filter(listed -> listed.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("No such resource."));
  }

  private static String validName(@Nullable String name) {
    String cleaned = name == null ? "" : name.strip();
    if (cleaned.isEmpty() || cleaned.length() > 60) {
      throw new InvalidInputException("Give it a name of 1 to 60 characters.");
    }
    return cleaned;
  }
}
