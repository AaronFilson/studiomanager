package io.github.aaronfilson.studiomanager.ai;

import java.time.ZoneOffset;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.AiApi;
import io.github.aaronfilson.studiomanager.contract.AiKeyStatusDto;
import io.github.aaronfilson.studiomanager.contract.AiProviderDto;
import io.github.aaronfilson.studiomanager.contract.SetAiKeyRequestDto;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AiKeyController implements AiApi {

    private final AiKeys keys;

    AiKeyController(AiKeys keys) {
        this.keys = keys;
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'OWNER')")
    public ResponseEntity<AiKeyStatusDto> getAiKey(UUID studioId) {
        return ResponseEntity.ok(dto(keys.status().orElseThrow(() -> new NotFoundException("The studio has no AI key."))));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'OWNER')")
    public ResponseEntity<AiKeyStatusDto> setAiKey(UUID studioId, SetAiKeyRequestDto request) {
        return ResponseEntity.ok(dto(keys.set(AiKeys.Provider.valueOf(request.getProvider().getValue()),
                request.getApiKey(), Boolean.TRUE.equals(request.getAllowGoogleFallback()))));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'OWNER')")
    public ResponseEntity<Void> deleteAiKey(UUID studioId) {
        keys.remove();
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'OWNER')")
    public ResponseEntity<AiKeyStatusDto> testAiKey(UUID studioId) {
        return ResponseEntity.ok(dto(keys.test()));
    }

    private static AiKeyStatusDto dto(AiKeys.KeyStatus status) {
        return new AiKeyStatusDto()
                .provider(AiProviderDto.fromValue(status.provider().name()))
                .lastFour(status.lastFour())
                .status(AiKeyStatusDto.StatusEnum.valueOf(status.status().name()))
                .reason(status.reason())
                .checkedAt(status.checkedAt() == null ? null : status.checkedAt().atOffset(ZoneOffset.UTC))
                .allowGoogleFallback(status.allowGoogleFallback())
                .setAt(status.setAt().atOffset(ZoneOffset.UTC));
    }

}
