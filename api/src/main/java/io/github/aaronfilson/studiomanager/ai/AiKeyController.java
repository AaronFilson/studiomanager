package io.github.aaronfilson.studiomanager.ai;

import io.github.aaronfilson.studiomanager.contract.AiApi;
import io.github.aaronfilson.studiomanager.contract.AiKeyStatusDto;
import io.github.aaronfilson.studiomanager.contract.AiProviderDto;
import io.github.aaronfilson.studiomanager.contract.SetAiKeyRequestDto;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import java.time.ZoneOffset;
import java.util.UUID;
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
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_AI_KEY_MANAGE')")
  public ResponseEntity<AiKeyStatusDto> getAiKey(UUID studioId) {
    return ResponseEntity.ok(
        dto(keys.status().orElseThrow(() -> new NotFoundException("The studio has no AI key."))));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_AI_KEY_MANAGE')")
  public ResponseEntity<AiKeyStatusDto> setAiKey(UUID studioId, SetAiKeyRequestDto request) {
    return ResponseEntity.ok(
        dto(
            keys.set(
                AiKeys.Provider.valueOf(request.getProvider().getValue()),
                request.getApiKey(),
                Boolean.TRUE.equals(request.getAllowPlatformReaders()))));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_AI_KEY_MANAGE')")
  public ResponseEntity<Void> deleteAiKey(UUID studioId) {
    keys.remove();
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_AI_KEY_MANAGE')")
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
        .allowPlatformReaders(status.allowPlatformReaders())
        .setAt(status.setAt().atOffset(ZoneOffset.UTC));
  }
}
