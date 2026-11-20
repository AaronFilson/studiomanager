package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Studio glaze mix log and specific gravity readings. Only studio glazes have mixes, and only staff
 * holding {@link Permission#GLAZES_STUDIO_EDIT} log mixes or readings.
 */
@Service
public class GlazeMixService {

  static final BigDecimal SG_TOLERANCE = new BigDecimal("0.03");
  private static final BigDecimal MAX_WEIGHT_G = new BigDecimal("2000000");

  private final GlazeMixRepository mixes;
  private final GlazeBook book;
  private final AccountDirectory accounts;
  private final ApplicationEventPublisher events;
  private final EntityManager entityManager;
  private final Clock clock;

  GlazeMixService(
      GlazeMixRepository mixes,
      GlazeBook book,
      AccountDirectory accounts,
      ApplicationEventPublisher events,
      EntityManager entityManager,
      Clock clock) {
    this.mixes = mixes;
    this.book = book;
    this.accounts = accounts;
    this.events = events;
    this.entityManager = entityManager;
    this.clock = clock;
  }

  public record ReadingView(
      UUID id,
      UUID readByAccountId,
      @Nullable String readByName,
      Instant readAt,
      BigDecimal specificGravity,
      @Nullable String note,
      boolean offTarget,
      @Nullable String warning) {}

  public record MixView(
      UUID id,
      UUID recipeId,
      UUID mixedByAccountId,
      @Nullable String mixedByName,
      Instant mixedAt,
      BigDecimal dryWeightG,
      @Nullable BigDecimal waterG,
      @Nullable String notes,
      List<ReadingView> readings) {}

  @Transactional(readOnly = true)
  public List<MixView> list(UUID glazeId) {
    GlazeRecipe recipe = book.find(glazeId);
    if (recipe.ownerKind() != GlazeBook.OwnerKind.STUDIO) {
      return List.of();
    }
    List<GlazeMix> found = mixes.findByRecipeIdWithReadings(glazeId);
    if (found.isEmpty()) {
      return List.of();
    }
    Set<UUID> accountIds = new HashSet<>();
    for (GlazeMix mix : found) {
      accountIds.add(mix.mixedBy());
      for (GlazeMixReading reading : mix.readings()) {
        accountIds.add(reading.readBy());
      }
    }
    Map<UUID, String> names = accounts.displayNames(accountIds);
    return found.stream().map(mix -> toView(mix, recipe.targetSg(), names)).toList();
  }

  @Transactional
  public MixView logMix(
      UUID glazeId,
      BigDecimal dryWeightG,
      @Nullable BigDecimal waterG,
      @Nullable BigDecimal initialSpecificGravity,
      @Nullable String notes) {
    TenantContext caller = requireStudioEdit();
    GlazeRecipe recipe = requireStudioGlaze(glazeId);
    BigDecimal cleanDry = checkDryWeight(dryWeightG);
    BigDecimal cleanWater = checkWater(waterG);
    BigDecimal cleanSg = initialSpecificGravity == null ? null : checkSg(initialSpecificGravity);
    String cleanNotes = cleanText(notes, 2000, "notes");
    Instant now = clock.instant();

    GlazeMix mix =
        new GlazeMix(recipe.id(), caller.accountId(), now, cleanDry, cleanWater, cleanNotes);
    if (cleanSg != null) {
      mix.addReading(caller.accountId(), now, cleanSg, "Initial reading");
    }
    entityManager.persist(mix);
    entityManager.flush();

    events.publishEvent(
        new GlazeMixed(
            caller.requireStudioId(),
            mix.id(),
            recipe.id(),
            recipe.name(),
            caller.accountId(),
            now,
            cleanDry,
            cleanWater,
            cleanSg,
            recipe.targetSg(),
            cleanNotes));

    Map<UUID, String> names = accounts.displayNames(List.of(caller.accountId()));
    return toView(mix, recipe.targetSg(), names);
  }

  @Transactional
  public ReadingView addReading(
      UUID glazeId, UUID mixId, BigDecimal specificGravity, @Nullable String note) {
    TenantContext caller = requireStudioEdit();
    GlazeRecipe recipe = requireStudioGlaze(glazeId);
    BigDecimal cleanSg = checkSg(specificGravity);
    String cleanNote = cleanText(note, 500, "note");
    GlazeMix mix =
        mixes
            .findByIdAndRecipeIdWithReadings(mixId, glazeId)
            .orElseThrow(() -> new NotFoundException("No such glaze mix."));
    GlazeMixReading reading =
        mix.addReading(caller.accountId(), clock.instant(), cleanSg, cleanNote);
    mixes.flush();
    Map<UUID, String> names = accounts.displayNames(List.of(caller.accountId()));
    return toReadingView(reading, recipe.targetSg(), names);
  }

  static @Nullable String sgWarning(BigDecimal measured, @Nullable BigDecimal target) {
    if (target == null) {
      return null;
    }
    BigDecimal delta = measured.subtract(target);
    BigDecimal abs = delta.abs();
    if (abs.compareTo(SG_TOLERANCE) <= 0) {
      return null;
    }
    String direction = delta.signum() > 0 ? "above" : "below";
    return "Specific gravity %s is %s %s the target %s (±%s)."
        .formatted(
            measured.stripTrailingZeros().toPlainString(),
            abs.stripTrailingZeros().toPlainString(),
            direction,
            target.stripTrailingZeros().toPlainString(),
            SG_TOLERANCE.toPlainString());
  }

  private TenantContext requireStudioEdit() {
    TenantContext caller = TenantContext.require();
    if (!caller.holds(Permission.GLAZES_STUDIO_EDIT)) {
      throw new AccessDeniedException("Only staff can log studio glaze mixes and readings.");
    }
    return caller;
  }

  private GlazeRecipe requireStudioGlaze(UUID glazeId) {
    GlazeRecipe recipe = book.find(glazeId);
    if (recipe.ownerKind() != GlazeBook.OwnerKind.STUDIO) {
      throw new InvalidInputException("Only studio glazes have a mix log.");
    }
    return recipe;
  }

  private static BigDecimal checkDryWeight(BigDecimal value) {
    if (value.signum() <= 0 || value.compareTo(MAX_WEIGHT_G) > 0) {
      throw new InvalidInputException("Dry weight must be between 0 and 2,000,000 g.");
    }
    if (value.scale() > 2) {
      throw new InvalidInputException("Dry weight can have up to 2 decimal places.");
    }
    return value.stripTrailingZeros();
  }

  private static @Nullable BigDecimal checkWater(@Nullable BigDecimal value) {
    if (value == null) {
      return null;
    }
    if (value.signum() < 0 || value.compareTo(MAX_WEIGHT_G) > 0) {
      throw new InvalidInputException("Water weight must be between 0 and 2,000,000 g.");
    }
    if (value.scale() > 2) {
      throw new InvalidInputException("Water weight can have up to 2 decimal places.");
    }
    return value.stripTrailingZeros();
  }

  private static BigDecimal checkSg(BigDecimal value) {
    if (value.compareTo(GlazeEdits.MIN_SPECIFIC_GRAVITY) < 0
        || value.compareTo(GlazeEdits.MAX_SPECIFIC_GRAVITY) > 0) {
      throw new InvalidInputException("A specific gravity is between 1 and 2.5.");
    }
    if (value.scale() > 3) {
      throw new InvalidInputException("Specific gravity can have up to 3 decimal places.");
    }
    return value.stripTrailingZeros();
  }

  private static @Nullable String cleanText(@Nullable String text, int max, String field) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String trimmed = text.strip();
    if (trimmed.length() > max) {
      throw new InvalidInputException("Keep the %s to %d characters.".formatted(field, max));
    }
    return trimmed;
  }

  private static MixView toView(
      GlazeMix mix, @Nullable BigDecimal targetSg, Map<UUID, String> names) {
    List<ReadingView> readings =
        mix.readings().stream().map(r -> toReadingView(r, targetSg, names)).toList();
    return new MixView(
        mix.id(),
        mix.recipeId(),
        mix.mixedBy(),
        names.get(mix.mixedBy()),
        mix.mixedAt(),
        mix.dryWeightG().stripTrailingZeros(),
        mix.waterG() == null ? null : mix.waterG().stripTrailingZeros(),
        mix.notes(),
        readings);
  }

  private static ReadingView toReadingView(
      GlazeMixReading reading, @Nullable BigDecimal targetSg, Map<UUID, String> names) {
    BigDecimal sg = reading.specificGravity().stripTrailingZeros();
    String warning = sgWarning(sg, targetSg);
    return new ReadingView(
        reading.id(),
        reading.readBy(),
        names.get(reading.readBy()),
        reading.readAt(),
        sg,
        reading.note(),
        warning != null,
        warning);
  }
}
