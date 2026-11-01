package io.github.aaronfilson.studiomanager.importer;

import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.ImportConsignmentRequestDto;
import io.github.aaronfilson.studiomanager.contract.ImportMembersRequestDto;
import io.github.aaronfilson.studiomanager.contract.ImportResultDto;
import io.github.aaronfilson.studiomanager.contract.ImportResultRowsInnerDto;
import io.github.aaronfilson.studiomanager.contract.ImportsApi;
import io.github.aaronfilson.studiomanager.importer.ImportModel.ConsignmentRow;
import io.github.aaronfilson.studiomanager.importer.ImportModel.ImportResult;
import io.github.aaronfilson.studiomanager.importer.ImportModel.MemberRow;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ImportController implements ImportsApi {

    private final ImportService imports;

    ImportController(ImportService imports) {
        this.imports = imports;
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<ImportResultDto> importMembers(UUID studioId, ImportMembersRequestDto request) {
        var rows = request.getRows().stream()
                .map(row -> new MemberRow(row.getLine(), row.getEmail(), row.getBalance(), row.getBalanceAsOf()))
                .toList();
        return ResponseEntity.ok(toDto(imports.importMembers(rows, Boolean.TRUE.equals(request.getDryRun()))));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<ImportResultDto> importConsignment(UUID studioId, ImportConsignmentRequestDto request) {
        var rows = request.getRows().stream()
                .map(row -> new ConsignmentRow(row.getLine(), row.getArtist(), row.getArtistEmail(), row.getDescription(),
                        row.getRetailPrice(), row.getQuantity(), row.getArtistShare(), row.getReceivedOn()))
                .toList();
        return ResponseEntity.ok(toDto(imports.importConsignment(rows, Boolean.TRUE.equals(request.getDryRun()))));
    }

    private static ImportResultDto toDto(ImportResult result) {
        return new ImportResultDto()
                .committed(result.committed())
                .rows(result.rows().stream()
                        .map(row -> new ImportResultRowsInnerDto()
                                .line(row.line())
                                .outcome(ImportResultRowsInnerDto.OutcomeEnum.valueOf(row.outcome().name()))
                                .message(row.message()))
                        .toList());
    }

}
