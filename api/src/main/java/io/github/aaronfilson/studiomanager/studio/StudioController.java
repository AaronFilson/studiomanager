package io.github.aaronfilson.studiomanager.studio;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.CreateStudioRequestDto;
import io.github.aaronfilson.studiomanager.contract.StudioDto;
import io.github.aaronfilson.studiomanager.contract.StudioRoleDto;
import io.github.aaronfilson.studiomanager.contract.StudiosApi;
import io.github.aaronfilson.studiomanager.contract.UnitSystemDto;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Implements the generated {@link StudiosApi}; routes and request validation come from the spec. */
@RestController
class StudioController implements StudiosApi {

    private final StudioService studios;

    StudioController(StudioService studios) {
        this.studios = studios;
    }

    @Override
    public ResponseEntity<StudioDto> createStudio(CreateStudioRequestDto request) {
        StudioView studio = studios.create(request.getName(), UnitSystem.valueOf(request.getUnitSystem().name()),
                request.getTimeZone(), request.getCountry(), request.getCurrency());
        return ResponseEntity.created(URI.create("/api/v1/studios/" + studio.id())).body(toDto(studio));
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<StudioDto> getStudio(UUID studioId) {
        return ResponseEntity.ok(toDto(studios.get(studioId)));
    }

    private static StudioDto toDto(StudioView studio) {
        return new StudioDto(studio.id(), studio.name(), UnitSystemDto.valueOf(studio.unitSystem().name()),
                studio.timeZone().getId(), studio.country(), studio.currency().getCurrencyCode(),
                StudioRoleDto.valueOf(studio.myRole().name()), studio.memberCount(),
                studio.createdAt().atOffset(ZoneOffset.UTC));
    }

}
