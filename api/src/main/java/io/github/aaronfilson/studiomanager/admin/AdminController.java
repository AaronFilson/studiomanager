package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.contract.AdminApi;
import io.github.aaronfilson.studiomanager.contract.BoostStepDto;
import io.github.aaronfilson.studiomanager.contract.ChannelRateLimitsDto;
import io.github.aaronfilson.studiomanager.contract.RateLimitBoostDto;
import io.github.aaronfilson.studiomanager.contract.RateLimitOverviewDto;
import io.github.aaronfilson.studiomanager.contract.RateLimitValuesDto;
import io.github.aaronfilson.studiomanager.contract.ScheduleBoostRequestDto;
import io.github.aaronfilson.studiomanager.contract.SettingChangeDto;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Limits;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The admin API. The security configuration already requires the {@code Platform.Admin} app role on
 * these routes; the annotations say so again where the methods are.
 */
@RestController
class AdminController implements AdminApi {

  private static final int DEFAULT_HISTORY = 50;
  private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};

  private final RateLimitAdmin rateLimits;
  private final JsonMapper json;

  AdminController(RateLimitAdmin rateLimits, JsonMapper json) {
    this.rateLimits = rateLimits;
    this.json = json;
  }

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<RateLimitOverviewDto> getRateLimits() {
    return ResponseEntity.ok(dto(rateLimits.overview()));
  }

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<RateLimitOverviewDto> setNormalRateLimits(ChannelRateLimitsDto request) {
    return ResponseEntity.ok(
        dto(
            rateLimits.setNormal(
                new RateLimitAdmin.ChannelLimits(
                    limits(request.getMcp()), limits(request.getToken())))));
  }

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<RateLimitOverviewDto> resetNormalRateLimits() {
    return ResponseEntity.ok(dto(rateLimits.resetNormal()));
  }

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<RateLimitOverviewDto> scheduleRateLimitBoost(
      ScheduleBoostRequestDto request) {
    OffsetDateTime startsAt = request.getStartsAt();
    return ResponseEntity.ok(
        dto(
            rateLimits.scheduleBoost(
                startsAt == null ? null : startsAt.toInstant(),
                request.getStudioId(),
                request.getUnlimitedMinutes(),
                request.getEasingMinutes())));
  }

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<RateLimitOverviewDto> endRateLimitBoost() {
    return ResponseEntity.ok(dto(rateLimits.endBoost()));
  }

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<List<SettingChangeDto>> listSettingChanges(@Nullable Integer limit) {
    return ResponseEntity.ok(
        rateLimits.history(limit == null ? DEFAULT_HISTORY : limit).stream()
            .map(this::dto)
            .toList());
  }

  private static Limits limits(RateLimitValuesDto values) {
    return new Limits(
        values.getBurst(),
        values.getRefillTokens(),
        Duration.ofSeconds(values.getRefillPeriodSeconds()),
        values.getDailyCap());
  }

  private static RateLimitOverviewDto dto(RateLimitAdmin.Overview overview) {
    RateLimitAdmin.ChannelLimits override = overview.override();
    RateLimitBoost boost = overview.boost();
    return new RateLimitOverviewDto()
        .now(utc(overview.now()))
        .defaults(dto(overview.defaults()))
        .override(override == null ? null : dto(override))
        .normal(dto(overview.normal()))
        .boost(boost == null ? null : dto(boost));
  }

  private static ChannelRateLimitsDto dto(RateLimitAdmin.ChannelLimits limits) {
    return new ChannelRateLimitsDto().mcp(dto(limits.mcp())).token(dto(limits.token()));
  }

  private static RateLimitValuesDto dto(Limits limits) {
    return new RateLimitValuesDto()
        .burst(limits.burst())
        .refillTokens(limits.refillTokens())
        .refillPeriodSeconds(Math.toIntExact(limits.refillPeriod().toSeconds()))
        .dailyCap(limits.dailyCap());
  }

  private static RateLimitBoostDto dto(RateLimitBoost boost) {
    return new RateLimitBoostDto()
        .startsAt(utc(boost.startsAt()))
        .endsAt(utc(boost.endsAt()))
        .studioId(boost.studioId())
        .unlimitedMinutes(Math.toIntExact(boost.unlimited().toMinutes()))
        .easingMinutes(Math.toIntExact(boost.easing().toMinutes()))
        .steps(boost.steps().stream().map(AdminController::dto).toList());
  }

  private static BoostStepDto dto(RateLimitBoost.Step step) {
    return switch (step) {
      case RateLimitBoost.Step.Unlimited unlimited ->
          new BoostStepDto()
              .kind(BoostStepDto.KindEnum.UNLIMITED)
              .startsAt(utc(unlimited.startsAt()))
              .endsAt(utc(unlimited.endsAt()));
      case RateLimitBoost.Step.Easing easing ->
          new BoostStepDto()
              .kind(BoostStepDto.KindEnum.EASING)
              .startsAt(utc(easing.startsAt()))
              .endsAt(utc(easing.endsAt()))
              .multiplier(easing.multiplier());
    };
  }

  private SettingChangeDto dto(PlatformSettings.Change change) {
    return new SettingChangeDto()
        .key(change.key())
        .version(change.version())
        .oldValue(jsonObject(change.oldJson()))
        .newValue(jsonObject(change.newJson()))
        .changedById(change.changedById())
        .changedByName(change.changedByName())
        .changedAt(change.changedAt().withOffsetSameInstant(ZoneOffset.UTC));
  }

  private @Nullable Map<String, Object> jsonObject(@Nullable String stored) {
    return stored == null ? null : json.readValue(stored, JSON_OBJECT);
  }

  private static OffsetDateTime utc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
