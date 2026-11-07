package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.contract.AssetAvailabilityRequestDto;
import io.github.aaronfilson.studiomanager.contract.AssetDto;
import io.github.aaronfilson.studiomanager.contract.AssetKindDto;
import io.github.aaronfilson.studiomanager.contract.CompleteMaintenanceTaskRequestDto;
import io.github.aaronfilson.studiomanager.contract.MaintenanceApi;
import io.github.aaronfilson.studiomanager.contract.MaintenanceRecordDto;
import io.github.aaronfilson.studiomanager.contract.MaintenanceRecordListDto;
import io.github.aaronfilson.studiomanager.contract.MaintenanceTaskDto;
import io.github.aaronfilson.studiomanager.contract.PartUseDto;
import io.github.aaronfilson.studiomanager.contract.PhotoUploadDto;
import io.github.aaronfilson.studiomanager.contract.ProblemReportDto;
import io.github.aaronfilson.studiomanager.contract.ProblemReportListDto;
import io.github.aaronfilson.studiomanager.contract.RecordRepairRequestDto;
import io.github.aaronfilson.studiomanager.contract.ReportProblemRequestDto;
import io.github.aaronfilson.studiomanager.contract.ReportableAssetDto;
import io.github.aaronfilson.studiomanager.contract.ResolveProblemRequestDto;
import io.github.aaronfilson.studiomanager.contract.SaveAssetRequestDto;
import io.github.aaronfilson.studiomanager.contract.SaveMaintenanceTaskRequestDto;
import io.github.aaronfilson.studiomanager.contract.SuggestedMaintenanceTaskDto;
import io.github.aaronfilson.studiomanager.contract.UpdateAssetRequestDto;
import io.github.aaronfilson.studiomanager.contract.UpdateMaintenanceTaskRequestDto;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.AssetKind;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.PartUse;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.TaskSettings;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MaintenanceController implements MaintenanceApi {

  private final MaintenanceService maintenance;
  private final AssetChanges assetChanges;
  private final TaskChanges taskChanges;
  private final MaintenanceRecords records;
  private final ProblemReportService problems;
  private final RecordPhotoService recordPhotos;
  private final StudioService studios;
  private final Clock clock;

  MaintenanceController(
      MaintenanceService maintenance,
      AssetChanges assetChanges,
      TaskChanges taskChanges,
      MaintenanceRecords records,
      ProblemReportService problems,
      RecordPhotoService recordPhotos,
      StudioService studios,
      Clock clock) {
    this.maintenance = maintenance;
    this.assetChanges = assetChanges;
    this.taskChanges = taskChanges;
    this.records = records;
    this.problems = problems;
    this.recordPhotos = recordPhotos;
    this.studios = studios;
    this.clock = clock;
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<List<AssetDto>> listAssets(UUID studioId) {
    return ResponseEntity.ok(
        maintenance.assets(clock.instant()).stream().map(MaintenanceDtos::toDto).toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<AssetDto> createAsset(UUID studioId, SaveAssetRequestDto request) {
    var asset =
        assetChanges.createAsset(
            AssetKind.valueOf(request.getKind().name()),
            request.getName(),
            request.getLocation(),
            request.getNotes());
    return ResponseEntity.created(
            URI.create("/api/v1/studios/%s/maintenance/assets/%s".formatted(studioId, asset.id())))
        .body(MaintenanceDtos.toDto(asset));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<AssetDto> updateAsset(
      UUID studioId, UUID assetId, UpdateAssetRequestDto request) {
    return ResponseEntity.ok(
        MaintenanceDtos.toDto(
            assetChanges.updateAsset(
                assetId,
                request.getName(),
                request.getLocation(),
                request.getNotes(),
                request.getExpectedVersion())));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<Void> archiveAsset(UUID studioId, UUID assetId) {
    assetChanges.archiveAsset(assetId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<AssetDto> setAssetAvailability(
      UUID studioId, UUID assetId, AssetAvailabilityRequestDto request) {
    return ResponseEntity.ok(
        MaintenanceDtos.toDto(
            assetChanges.setDown(assetId, request.getDownReason(), request.getExpectedVersion())));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<MaintenanceTaskDto> createMaintenanceTask(
      UUID studioId, UUID assetId, SaveMaintenanceTaskRequestDto request) {
    var task = taskChanges.createTask(assetId, settings(request));
    return ResponseEntity.created(
            URI.create("/api/v1/studios/%s/maintenance/tasks/%s".formatted(studioId, task.id())))
        .body(MaintenanceDtos.toDto(task));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<MaintenanceTaskDto> updateMaintenanceTask(
      UUID studioId, UUID taskId, UpdateMaintenanceTaskRequestDto request) {
    return ResponseEntity.ok(
        MaintenanceDtos.toDto(
            taskChanges.updateTask(
                taskId, settings(request.getTask()), request.getExpectedVersion())));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<Void> archiveMaintenanceTask(UUID studioId, UUID taskId) {
    taskChanges.archiveTask(taskId);
    return ResponseEntity.noContent().build();
  }

  /** Members may call this; the service lets through only staff and the task's assignee. */
  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<MaintenanceTaskDto> completeMaintenanceTask(
      UUID studioId, UUID taskId, CompleteMaintenanceTaskRequestDto request) {
    return ResponseEntity.ok(
        MaintenanceDtos.toDto(
            records.complete(
                taskId,
                request.getNote(),
                decimal(request.getCost()),
                request.getChecked() == null ? List.of() : request.getChecked(),
                parts(request.getParts()))));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<MaintenanceRecordDto> recordRepair(
      UUID studioId, UUID assetId, RecordRepairRequestDto request) {
    var record =
        records.recordRepair(
            assetId, request.getNote(), decimal(request.getCost()), parts(request.getParts()));
    return ResponseEntity.status(201).body(MaintenanceDtos.toDto(record, List.of()));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<MaintenanceRecordListDto> listMaintenanceRecords(
      UUID studioId, UUID assetId) {
    Map<UUID, List<RecordPhotoService.Photo>> photos = recordPhotos.photosOf(assetId);
    Capped<MaintenanceRecordDto> history =
        records
            .records(assetId)
            .map(r -> MaintenanceDtos.toDto(r, photos.getOrDefault(r.id(), List.of())));
    return ResponseEntity.ok(
        new MaintenanceRecordListDto(history.items(), history.truncated(), history.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<PhotoUploadDto> startRecordPhotoUpload(UUID studioId, UUID recordId) {
    RecordPhotoService.Upload upload = recordPhotos.startUpload(recordId);
    return ResponseEntity.status(201)
        .body(
            new PhotoUploadDto()
                .photoId(upload.photoId())
                .photoUploadUrl(upload.photoUploadUrl())
                .thumbnailUploadUrl(upload.thumbnailUploadUrl())
                .expiresAt(upload.expiresAt().atOffset(ZoneOffset.UTC))
                .maxPhotoBytes(upload.maxPhotoBytes())
                .maxThumbnailBytes(upload.maxThumbnailBytes()));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<Void> completeRecordPhotoUpload(
      UUID studioId, UUID recordId, UUID photoId) {
    recordPhotos.complete(recordId, photoId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<List<SuggestedMaintenanceTaskDto>> listSuggestedMaintenanceTasks(
      UUID studioId, UUID assetId) {
    return ResponseEntity.ok(
        maintenance.suggestions(assetId).stream()
            .map(s ->
                new SuggestedMaintenanceTaskDto()
                    .task(
                        new SaveMaintenanceTaskRequestDto()
                            .title(s.task().title())
                            .checklist(s.task().checklist())
                            .everyDays(s.task().everyDays())
                            .everyFirings(s.task().everyFirings()))
                    .source(s.source()))
            .toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<String> exportMaintenanceRecords(
      LocalDate from, LocalDate to, UUID studioId) {
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"maintenance-%s-to-%s.csv\"".formatted(from, to))
        .body(records.recordsCsv(from, to, studios.current().timeZone()));
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<List<ReportableAssetDto>> listReportableAssets(UUID studioId) {
    return ResponseEntity.ok(
        problems.reportable().stream()
            .map(a ->
                new ReportableAssetDto()
                    .id(a.id())
                    .kind(AssetKindDto.valueOf(a.kind().name()))
                    .name(a.name())
                    .available(a.available()))
            .toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<ProblemReportListDto> listProblemReports(
      UUID studioId, @Nullable Boolean open) {
    Capped<ProblemReportDto> reports =
        problems.reports(Boolean.TRUE.equals(open)).map(MaintenanceDtos::toDto);
    return ResponseEntity.ok(
        new ProblemReportListDto(reports.items(), reports.truncated(), reports.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<ProblemReportDto> reportProblem(
      UUID studioId, ReportProblemRequestDto request) {
    return ResponseEntity.status(201)
        .body(
            MaintenanceDtos.toDto(problems.report(request.getAssetId(), request.getDescription())));
  }

  /** Members may call this; the service lets through only staff. */
  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<ProblemReportDto> resolveProblemReport(
      UUID studioId, UUID reportId, ResolveProblemRequestDto request) {
    return ResponseEntity.ok(
        MaintenanceDtos.toDto(
            problems.resolve(reportId, request.getResolution(), request.getExpectedVersion())));
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<List<MaintenanceTaskDto>> listMyMaintenanceTasks(UUID studioId) {
    return ResponseEntity.ok(
        maintenance.myTasks(clock.instant()).stream().map(MaintenanceDtos::toDto).toList());
  }

  private static TaskSettings settings(SaveMaintenanceTaskRequestDto request) {
    return new TaskSettings(
        request.getTitle(),
        request.getChecklist() == null ? List.of() : request.getChecklist(),
        request.getEveryDays(),
        request.getEveryFirings(),
        request.getAssignedDuty() == null ? null : Duty.valueOf(request.getAssignedDuty().name()),
        request.getAssignedTo());
  }

  private static List<PartUse> parts(@Nullable List<PartUseDto> parts) {
    return parts == null
        ? List.of()
        : parts.stream()
            .map(p -> new PartUse(p.getItemId(), new BigDecimal(p.getQuantity())))
            .toList();
  }

  private static @Nullable BigDecimal decimal(@Nullable String value) {
    return value == null ? null : new BigDecimal(value);
  }
}
