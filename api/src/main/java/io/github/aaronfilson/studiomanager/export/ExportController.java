package io.github.aaronfilson.studiomanager.export;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.ExportApi;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ExportController implements ExportApi {

    private final StudioExport export;

    ExportController(StudioExport export) {
        this.export = export;
    }

    /** Owner only: it holds every member's email address and the studio's books. */
    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'OWNER')")
    public ResponseEntity<Resource> exportStudio(UUID studioId) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String filename = "studio-export-%s.zip".formatted(now.atOffset(ZoneOffset.UTC).toLocalDate());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new ByteArrayResource(export.zip(now)));
    }

}
