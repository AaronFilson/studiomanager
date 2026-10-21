package io.github.aaronfilson.studiomanager.maintenance;

import java.time.ZoneOffset;
import java.util.List;

import io.github.aaronfilson.studiomanager.contract.AssetDto;
import io.github.aaronfilson.studiomanager.contract.AssetKindDto;
import io.github.aaronfilson.studiomanager.contract.DueStatusDto;
import io.github.aaronfilson.studiomanager.contract.DutyDto;
import io.github.aaronfilson.studiomanager.contract.MaintenanceRecordDto;
import io.github.aaronfilson.studiomanager.contract.MaintenanceTaskDto;
import io.github.aaronfilson.studiomanager.contract.PartUsedDto;
import io.github.aaronfilson.studiomanager.contract.PiecePhotoDto;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Asset;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Record;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Task;

/** Converts maintenance to the HTTP contract. Public for Today, which lists due tasks. */
public final class MaintenanceDtos {

    private MaintenanceDtos() {
    }

    public static MaintenanceTaskDto toDto(Task task) {
        return new MaintenanceTaskDto()
                .id(task.id())
                .assetId(task.assetId())
                .assetName(task.assetName())
                .title(task.title())
                .checklist(task.checklist())
                .everyDays(task.everyDays())
                .everyFirings(task.everyFirings())
                .assignedDuty(task.assignedDuty() == null ? null : DutyDto.valueOf(task.assignedDuty().name()))
                .assignedTo(task.assignedTo())
                .assignedToName(task.assignedToName())
                .lastDoneAt(task.lastDoneAt().atOffset(ZoneOffset.UTC))
                .dueAt(task.dueAt() == null ? null : task.dueAt().atOffset(ZoneOffset.UTC))
                .firingsLeft(task.firingsLeft())
                .status(DueStatusDto.valueOf(task.status().name()))
                .version(task.version());
    }

    static AssetDto toDto(Asset asset) {
        return new AssetDto()
                .id(asset.id())
                .kind(AssetKindDto.valueOf(asset.kind().name()))
                .name(asset.name())
                .kilnId(asset.kilnId())
                .location(asset.location())
                .notes(asset.notes())
                .available(asset.available())
                .downReason(asset.downReason())
                .totalFirings(asset.totalFirings())
                .version(asset.version())
                .tasks(asset.tasks().stream().map(MaintenanceDtos::toDto).toList());
    }

    static MaintenanceRecordDto toDto(Record record, List<RecordPhotoService.Photo> photos) {
        return new MaintenanceRecordDto()
                .id(record.id())
                .assetId(record.assetId())
                .taskId(record.taskId())
                .taskTitle(record.taskTitle())
                .note(record.note())
                .cost(record.cost() == null ? null : record.cost().toPlainString())
                .checked(record.checked())
                .atFiring(record.atFiring())
                .doneBy(record.doneBy())
                .doneByName(record.doneByName())
                .doneAt(record.doneAt().atOffset(ZoneOffset.UTC))
                .parts(record.parts().stream()
                        .map(p -> new PartUsedDto().itemId(p.itemId()).itemName(p.itemName()).unit(p.unit())
                                .quantity(p.quantity().toPlainString()))
                        .toList())
                .photos(photos.stream()
                        .map(p -> new PiecePhotoDto().id(p.id()).url(p.url()).thumbnailUrl(p.thumbnailUrl())
                                .uploadedByAccountId(p.uploadedBy()).uploadedAt(p.uploadedAt().atOffset(ZoneOffset.UTC)))
                        .toList());
    }

}
