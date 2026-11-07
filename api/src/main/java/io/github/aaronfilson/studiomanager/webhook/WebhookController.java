package io.github.aaronfilson.studiomanager.webhook;

import io.github.aaronfilson.studiomanager.contract.CreateWebhookEndpointDto;
import io.github.aaronfilson.studiomanager.contract.CreatedWebhookEndpointDto;
import io.github.aaronfilson.studiomanager.contract.UpdateWebhookEndpointDto;
import io.github.aaronfilson.studiomanager.contract.WebhookAttemptDto;
import io.github.aaronfilson.studiomanager.contract.WebhookDeliveryDto;
import io.github.aaronfilson.studiomanager.contract.WebhookDeliveryListDto;
import io.github.aaronfilson.studiomanager.contract.WebhookEndpointDto;
import io.github.aaronfilson.studiomanager.contract.WebhookEventTypeDto;
import io.github.aaronfilson.studiomanager.contract.WebhookEventTypeInfoDto;
import io.github.aaronfilson.studiomanager.contract.WebhookSecretDto;
import io.github.aaronfilson.studiomanager.contract.WebhooksApi;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class WebhookController implements WebhooksApi {

  private final WebhookEndpoints endpoints;
  private final StudioService studios;

  WebhookController(WebhookEndpoints endpoints, StudioService studios) {
    this.endpoints = endpoints;
    this.studios = studios;
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'WEBHOOKS_MANAGE')")
  public ResponseEntity<List<WebhookEventTypeInfoDto>> listWebhookEventTypes(UUID studioId) {
    String name = studios.current().name();
    return ResponseEntity.ok(
        Arrays.stream(WebhookEventType.values())
            .map(type ->
                new WebhookEventTypeInfoDto()
                    .type(WebhookEventTypeDto.fromValue(type.wireName()))
                    .description(type.description())
                    .example(WebhookExamples.of(type, studioId.toString(), name)))
            .toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'WEBHOOKS_MANAGE')")
  public ResponseEntity<List<WebhookEndpointDto>> listWebhookEndpoints(UUID studioId) {
    return ResponseEntity.ok(endpoints.list().stream().map(WebhookController::dto).toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'WEBHOOKS_MANAGE')")
  public ResponseEntity<CreatedWebhookEndpointDto> createWebhookEndpoint(
      UUID studioId, CreateWebhookEndpointDto request) {
    var created =
        endpoints.create(
            request.getUrl(), request.getDescription(), names(request.getEventTypes()));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CreatedWebhookEndpointDto()
                .endpoint(dto(created.endpoint()))
                .secret(created.secret()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'WEBHOOKS_MANAGE')")
  public ResponseEntity<WebhookEndpointDto> updateWebhookEndpoint(
      UUID studioId, UUID endpointId, UpdateWebhookEndpointDto request) {
    return ResponseEntity.ok(
        dto(
            endpoints.update(
                endpointId,
                request.getUrl(),
                request.getDescription(),
                // The generated DTO starts lists empty, so empty means "not given" (the spec needs
                // at least one)
                request.getEventTypes() == null || request.getEventTypes().isEmpty()
                    ? null
                    : names(request.getEventTypes()),
                request.getEnabled())));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'WEBHOOKS_MANAGE')")
  public ResponseEntity<Void> deleteWebhookEndpoint(UUID studioId, UUID endpointId) {
    endpoints.delete(endpointId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'WEBHOOKS_MANAGE')")
  public ResponseEntity<WebhookSecretDto> rotateWebhookSecret(UUID studioId, UUID endpointId) {
    var rotated = endpoints.rotateSecret(endpointId);
    return ResponseEntity.ok(
        new WebhookSecretDto()
            .secret(rotated.secret())
            .previousValidUntil(utc(rotated.previousValidUntil())));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'WEBHOOKS_MANAGE')")
  public ResponseEntity<WebhookDeliveryDto> testWebhookEndpoint(UUID studioId, UUID endpointId) {
    return ResponseEntity.ok(dto(endpoints.test(endpointId)));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'WEBHOOKS_MANAGE')")
  public ResponseEntity<WebhookDeliveryListDto> listWebhookDeliveries(
      UUID studioId, UUID endpointId) {
    Capped<WebhookDeliveryDto> deliveries =
        endpoints.deliveries(endpointId).map(WebhookController::dto);
    return ResponseEntity.ok(
        new WebhookDeliveryListDto(deliveries.items(), deliveries.truncated(), deliveries.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'WEBHOOKS_MANAGE')")
  public ResponseEntity<WebhookDeliveryDto> resendWebhookDelivery(UUID studioId, UUID deliveryId) {
    return ResponseEntity.ok(dto(endpoints.resend(deliveryId)));
  }

  private static List<String> names(List<WebhookEventTypeDto> types) {
    return types.stream().map(WebhookEventTypeDto::getValue).toList();
  }

  private static WebhookEndpointDto dto(WebhookEndpoints.EndpointView view) {
    return new WebhookEndpointDto()
        .id(view.id())
        .url(view.url())
        .description(view.description())
        .eventTypes(
            view.eventTypes().stream()
                .map(type -> WebhookEventTypeDto.fromValue(type.wireName()))
                .toList())
        .enabled(view.enabled())
        .disabledReason(view.disabledReason())
        .consecutiveFailures(view.consecutiveFailures())
        .previousSecretValidUntil(utcOrNull(view.previousSecretValidUntil()))
        .createdAt(utc(view.createdAt()));
  }

  private static WebhookDeliveryDto dto(WebhookEndpoints.DeliveryView view) {
    return new WebhookDeliveryDto()
        .id(view.id())
        .eventId(view.eventId())
        .eventType(WebhookEventTypeDto.fromValue(view.eventType().wireName()))
        .status(WebhookDeliveryDto.StatusEnum.valueOf(view.status()))
        .attempts(view.attempts())
        .createdAt(utc(view.createdAt()))
        .nextAttemptAt(utcOrNull(view.nextAttemptAt()))
        .deliveredAt(utcOrNull(view.deliveredAt()))
        .attemptLog(
            view.attemptLog().stream()
                .map(attempt ->
                    new WebhookAttemptDto()
                        .attemptedAt(utc(attempt.attemptedAt()))
                        .statusCode(attempt.statusCode())
                        .error(attempt.error())
                        .durationMs(attempt.durationMs())
                        .responseExcerpt(attempt.responseExcerpt()))
                .toList());
  }

  private static OffsetDateTime utc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }

  private static @Nullable OffsetDateTime utcOrNull(@Nullable Instant instant) {
    return instant == null ? null : utc(instant);
  }
}
