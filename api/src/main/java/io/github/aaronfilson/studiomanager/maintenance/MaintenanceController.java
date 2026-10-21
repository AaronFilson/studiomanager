package io.github.aaronfilson.studiomanager.maintenance;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.contract.AssetAvailabilityRequestDto;
import io.github.aaronfilson.studiomanager.contract.AssetDto;
import io.github.aaronfilson.studiomanager.contract.AssetKindDto;
import io.github.aaronfilson.studiomanager.contract.ProblemReportDto;
import io.github.aaronfilson.studiomanager.contract.ReportProblemRequestDto;
import io.github.aaronfilson.studiomanager.contract.ReportableAssetDto;
import io.github.aaronfilson.studiomanager.contract.ResolveProblemRequestDto;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.ProblemReport;
import io.github.aaronfilson.studiomanager.contract.CompleteMaintenanceTaskRequestDto;
import io.github.aaronfilson.studiomanager.contract.MaintenanceApi;
import io.github.aaronfilson.studiomanager.contract.MaintenanceRecordDto;
import io.github.aaronfilson.studiomanager.contract.MaintenanceTaskDto;
import io.github.aaronfilson.studiomanager.contract.RecordRepairRequestDto;
import io.github.aaronfilson.studiomanager.contract.SaveAssetRequestDto;
import io.github.aaronfilson.studiomanager.contract.SaveMaintenanceTaskRequestDto;
import io.github.aaronfilson.studiomanager.contract.SuggestedMaintenanceTaskDto;
import io.github.aaronfilson.studiomanager.contract.UpdateAssetRequestDto;
import io.github.aaronfilson.studiomanager.contract.UpdateMaintenanceTaskRequestDto;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.AssetKind;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.PartUse;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.TaskSettings;
import io.github.aaronfilson.studiomanager.studio.StudioService;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MaintenanceController implements MaintenanceApi {

    private final MaintenanceService maintenance;
    private final ProblemReportService problems;
    private final StudioService studios;

    MaintenanceController(MaintenanceService maintenance, ProblemReportService problems, StudioService studios) {
        this.maintenance = maintenance;
        this.problems = problems;
        this.studios = studios;
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<AssetDto>> listAssets(UUID studioId) {
        return ResponseEntity.ok(maintenance.assets(Instant.now()).stream().map(MaintenanceDtos::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<AssetDto> createAsset(UUID studioId, SaveAssetRequestDto request) {
        var asset = maintenance.createAsset(AssetKind.valueOf(request.getKind().name()), request.getName(),
                request.getLocation(), request.getNotes());
        return ResponseEntity.created(URI.create("/api/v1/studios/%s/maintenance/assets/%s".formatted(studioId, asset.id())))
                .body(MaintenanceDtos.toDto(asset));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<AssetDto> updateAsset(UUID studioId, UUID assetId, UpdateAssetRequestDto request) {
        return ResponseEntity.ok(MaintenanceDtos.toDto(maintenance.updateAsset(assetId, request.getName(),
                request.getLocation(), request.getNotes(), request.getExpectedVersion())));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<Void> archiveAsset(UUID studioId, UUID assetId) {
        maintenance.archiveAsset(assetId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<AssetDto> setAssetAvailability(UUID studioId, UUID assetId, AssetAvailabilityRequestDto request) {
        return ResponseEntity.ok(MaintenanceDtos.toDto(
                maintenance.setDown(assetId, request.getDownReason(), request.getExpectedVersion())));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<MaintenanceTaskDto> createMaintenanceTask(UUID studioId, UUID assetId,
            SaveMaintenanceTaskRequestDto request) {
        var task = maintenance.createTask(assetId, settings(request));
        return ResponseEntity.created(URI.create("/api/v1/studios/%s/maintenance/tasks/%s".formatted(studioId, task.id())))
                .body(MaintenanceDtos.toDto(task));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<MaintenanceTaskDto> updateMaintenanceTask(UUID studioId, UUID taskId,
            UpdateMaintenanceTaskRequestDto request) {
        return ResponseEntity.ok(MaintenanceDtos.toDto(
                maintenance.updateTask(taskId, settings(request.getTask()), request.getExpectedVersion())));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<Void> archiveMaintenanceTask(UUID studioId, UUID taskId) {
        maintenance.archiveTask(taskId);
        return ResponseEntity.noContent().build();
    }

    /** Members may call this; the service lets through only staff and the task's assignee. */
    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<MaintenanceTaskDto> completeMaintenanceTask(UUID studioId, UUID taskId,
            CompleteMaintenanceTaskRequestDto request) {
        return ResponseEntity.ok(MaintenanceDtos.toDto(maintenance.complete(taskId, request.getNote(),
                decimal(request.getCost()), request.getChecked() == null ? List.of() : request.getChecked())));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<MaintenanceRecordDto> recordRepair(UUID studioId, UUID assetId, RecordRepairRequestDto request) {
        var record = maintenance.recordRepair(assetId, request.getNote(), decimal(request.getCost()),
                request.getParts() == null ? List.of() : request.getParts().stream()
                        .map(p -> new PartUse(p.getItemId(), new BigDecimal(p.getQuantity()))).toList());
        return ResponseEntity.status(201).body(MaintenanceDtos.toDto(record));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<MaintenanceRecordDto>> listMaintenanceRecords(UUID studioId, UUID assetId) {
        return ResponseEntity.ok(maintenance.records(assetId).stream().map(MaintenanceDtos::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<SuggestedMaintenanceTaskDto>> listSuggestedMaintenanceTasks(UUID studioId, UUID assetId) {
        return ResponseEntity.ok(maintenance.suggestions(assetId).stream()
                .map(s -> new SuggestedMaintenanceTaskDto()
                        .task(new SaveMaintenanceTaskRequestDto()
                                .title(s.task().title())
                                .checklist(s.task().checklist())
                                .everyDays(s.task().everyDays())
                                .everyFirings(s.task().everyFirings()))
                        .source(s.source()))
                .toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<String> exportMaintenanceRecords(LocalDate from, LocalDate to, UUID studioId) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"maintenance-%s-to-%s.csv\"".formatted(from, to))
                .body(maintenance.recordsCsv(from, to, studios.current().timeZone()));
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<ReportableAssetDto>> listReportableAssets(UUID studioId) {
        return ResponseEntity.ok(problems.reportable().stream()
                .map(a -> new ReportableAssetDto().id(a.id()).kind(AssetKindDto.valueOf(a.kind().name())).name(a.name())
                        .available(a.available()))
                .toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<ProblemReportDto>> listProblemReports(UUID studioId, @Nullable Boolean open) {
        return ResponseEntity.ok(problems.reports(Boolean.TRUE.equals(open)).stream()
                .map(MaintenanceController::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<ProblemReportDto> reportProblem(UUID studioId, ReportProblemRequestDto request) {
        return ResponseEntity.status(201).body(toDto(problems.report(request.getAssetId(), request.getDescription())));
    }

    /** Members may call this; the service lets through only staff. */
    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<ProblemReportDto> resolveProblemReport(UUID studioId, UUID reportId,
            ResolveProblemRequestDto request) {
        return ResponseEntity.ok(toDto(problems.resolve(reportId, request.getResolution(), request.getExpectedVersion())));
    }

    private static ProblemReportDto toDto(ProblemReport report) {
        return new ProblemReportDto()
                .id(report.id())
                .assetId(report.assetId())
                .assetName(report.assetName())
                .description(report.description())
                .reportedBy(report.reportedBy())
                .reportedByName(report.reportedByName())
                .reportedAt(report.reportedAt().atOffset(ZoneOffset.UTC))
                .open(report.open())
                .resolvedByName(report.resolvedByName())
                .resolvedAt(report.resolvedAt() == null ? null : report.resolvedAt().atOffset(ZoneOffset.UTC))
                .resolution(report.resolution())
                .version(report.version());
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<MaintenanceTaskDto>> listMyMaintenanceTasks(UUID studioId) {
        return ResponseEntity.ok(maintenance.myTasks(Instant.now()).stream().map(MaintenanceDtos::toDto).toList());
    }

    private static TaskSettings settings(SaveMaintenanceTaskRequestDto request) {
        return new TaskSettings(request.getTitle(), request.getChecklist() == null ? List.of() : request.getChecklist(),
                request.getEveryDays(), request.getEveryFirings(),
                request.getAssignedDuty() == null ? null : Duty.valueOf(request.getAssignedDuty().name()),
                request.getAssignedTo());
    }

    private static @Nullable BigDecimal decimal(@Nullable String value) {
        return value == null ? null : new BigDecimal(value);
    }

}
