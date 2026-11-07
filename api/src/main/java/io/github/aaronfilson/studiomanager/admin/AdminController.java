package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.contract.AdminApi;
import io.github.aaronfilson.studiomanager.contract.AdminStudioDto;
import io.github.aaronfilson.studiomanager.contract.BoostStepDto;
import io.github.aaronfilson.studiomanager.contract.ChannelRateLimitsDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceReaderDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceReaderOverviewDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceReaderSettingsDto;
import io.github.aaronfilson.studiomanager.contract.PlatformReaderRuleDto;
import io.github.aaronfilson.studiomanager.contract.PlatformReaderScopeDto;
import io.github.aaronfilson.studiomanager.contract.PlatformReaderUseDto;
import io.github.aaronfilson.studiomanager.contract.RateLimitBoostDto;
import io.github.aaronfilson.studiomanager.contract.RateLimitOverviewDto;
import io.github.aaronfilson.studiomanager.contract.RateLimitValuesDto;
import io.github.aaronfilson.studiomanager.contract.ScheduleBoostRequestDto;
import io.github.aaronfilson.studiomanager.contract.SettingChangeDto;
import io.github.aaronfilson.studiomanager.contract.StudioReaderOrderDto;
import io.github.aaronfilson.studiomanager.contract.StudioReaderUseDto;
import io.github.aaronfilson.studiomanager.invoice.InvoiceReaderKind;
import io.github.aaronfilson.studiomanager.invoice.PlatformReaderRules;
import io.github.aaronfilson.studiomanager.invoice.PlatformReaderUsage;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Limits;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  private final InvoiceReaderAdmin invoiceReaders;
  private final JsonMapper json;

  AdminController(RateLimitAdmin rateLimits, InvoiceReaderAdmin invoiceReaders, JsonMapper json) {
    this.rateLimits = rateLimits;
    this.invoiceReaders = invoiceReaders;
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

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<List<AdminStudioDto>> listAdminStudios() {
    return ResponseEntity.ok(
        rateLimits.studios().stream()
            .map(studio -> new AdminStudioDto(studio.studioId(), studio.studioName()))
            .toList());
  }

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<InvoiceReaderOverviewDto> getInvoiceReaders() {
    return ResponseEntity.ok(dto(invoiceReaders.overview()));
  }

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<InvoiceReaderOverviewDto> setInvoiceReaders(
      InvoiceReaderSettingsDto request) {
    Map<InvoiceReaderKind, PlatformReaderRules.ReaderRule> readers =
        new EnumMap<>(InvoiceReaderKind.class);
    for (PlatformReaderRuleDto rule : request.getReaders()) {
      InvoiceReaderKind reader = InvoiceReaderKind.valueOf(rule.getReader().getValue());
      if (!reader.paidByPlatform()) {
        throw new InvalidInputException(
            "Only the platform's own readers have rules; Claude runs on the studio's key.");
      }
      if (readers.containsKey(reader)) {
        throw new InvalidInputException("Each reader has one rule.");
      }
      readers.put(
          reader,
          new PlatformReaderRules.ReaderRule(
              rule.getEnabled(),
              PlatformReaderRules.Scope.valueOf(rule.getScope().getValue()),
              new HashSet<>(rule.getStudioIds()),
              rule.getStudioMonthlyPages(),
              rule.getPlatformMonthlyPages()));
    }
    Map<UUID, List<InvoiceReaderKind>> orders = new LinkedHashMap<>();
    for (StudioReaderOrderDto order : request.getStudioOrders()) {
      if (orders.containsKey(order.getStudioId())) {
        throw new InvalidInputException("Each studio has one order.");
      }
      orders.put(
          order.getStudioId(),
          order.getReaders().stream()
              .map(reader -> InvoiceReaderKind.valueOf(reader.getValue()))
              .toList());
    }
    return ResponseEntity.ok(
        dto(
            invoiceReaders.save(
                new PlatformReaderRules.Rules(InvoiceReaderAdmin.complete(readers), orders))));
  }

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<InvoiceReaderOverviewDto> resetInvoiceReaders() {
    return ResponseEntity.ok(dto(invoiceReaders.reset()));
  }

  private static InvoiceReaderOverviewDto dto(InvoiceReaderAdmin.Overview overview) {
    PlatformReaderRules.Rules rules = overview.rules();
    Map<InvoiceReaderKind, PlatformReaderRules.ReaderRule> complete =
        InvoiceReaderAdmin.complete(rules.readers());
    List<PlatformReaderRuleDto> readers =
        complete.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry ->
                new PlatformReaderRuleDto()
                    .reader(InvoiceReaderDto.fromValue(entry.getKey().name()))
                    .enabled(entry.getValue().enabled())
                    .scope(PlatformReaderScopeDto.fromValue(entry.getValue().scope().name()))
                    .studioIds(entry.getValue().studios().stream().sorted().toList())
                    .studioMonthlyPages(entry.getValue().studioMonthlyPages())
                    .platformMonthlyPages(entry.getValue().platformMonthlyPages()))
            .toList();
    List<StudioReaderOrderDto> orders =
        rules.orders().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry ->
                new StudioReaderOrderDto()
                    .studioId(entry.getKey())
                    .readers(
                        entry.getValue().stream()
                            .map(reader -> InvoiceReaderDto.fromValue(reader.name()))
                            .toList()))
            .toList();
    List<PlatformReaderUseDto> usage =
        complete.keySet().stream().sorted().map(reader -> use(reader, overview)).toList();
    return new InvoiceReaderOverviewDto()
        .settings(new InvoiceReaderSettingsDto().readers(readers).studioOrders(orders))
        .saved(overview.saved())
        .monthStart(utc(overview.monthStart()))
        .usage(usage);
  }

  private static PlatformReaderUseDto use(
      InvoiceReaderKind reader, InvoiceReaderAdmin.Overview overview) {
    List<PlatformReaderUsage.StudioUse> uses =
        overview.usage().stream().filter(use -> use.reader() == reader).toList();
    long reads = uses.stream().mapToLong(PlatformReaderUsage.StudioUse::reads).sum();
    long pages = uses.stream().mapToLong(PlatformReaderUsage.StudioUse::pages).sum();
    BigDecimal cost =
        uses.stream()
            .map(PlatformReaderUsage.StudioUse::costUsd)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new PlatformReaderUseDto()
        .reader(InvoiceReaderDto.fromValue(reader.name()))
        .reads(reads)
        .pages(pages)
        .costUsd(money(cost))
        .studios(
            uses.stream()
                .map(use ->
                    new StudioReaderUseDto()
                        .studioId(use.studioId())
                        .studioName(overview.studioNames().get(use.studioId()))
                        .reads(use.reads())
                        .pages(use.pages())
                        .costUsd(money(use.costUsd())))
                .toList());
  }

  private static String money(BigDecimal amount) {
    return amount.setScale(4, RoundingMode.HALF_UP).toPlainString();
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
        .boost(boost == null ? null : dto(boost).studioName(overview.boostStudioName()));
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
