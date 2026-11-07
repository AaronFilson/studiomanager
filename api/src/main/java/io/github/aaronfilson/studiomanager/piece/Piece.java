package io.github.aaronfilson.studiomanager.piece;

import io.github.aaronfilson.studiomanager.firing.FeeQuote;
import io.github.aaronfilson.studiomanager.firing.PieceSize;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.Money;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.measure.Length;
import io.github.aaronfilson.studiomanager.platform.measure.LengthUnit;
import io.github.aaronfilson.studiomanager.platform.measure.Weight;
import io.github.aaronfilson.studiomanager.platform.measure.WeightUnit;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A member's piece in the bound studio. Hibernate fills {@code studio_id} from the bound studio,
 * and auditing fills {@code created_by} (ADR 21).
 *
 * <p>The measurements keep the value and unit they were entered in. The fee is a snapshot taken at
 * intake, so it is stored as its columns, not looked up again.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "piece")
@EntityListeners(AuditingEntityListener.class)
class Piece {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private int number;

  private UUID makerAccountId;

  private @Nullable String description;

  private @Nullable String clayBody;

  private @Nullable String makerMark;

  private @Nullable BigDecimal lengthValue;

  private @Nullable BigDecimal widthValue;

  private @Nullable BigDecimal heightValue;

  @Enumerated(EnumType.STRING)
  private @Nullable LengthUnit lengthUnit;

  private @Nullable BigDecimal weightValue;

  @Enumerated(EnumType.STRING)
  private @Nullable WeightUnit weightUnit;

  @Enumerated(EnumType.STRING)
  private PieceStage stage;

  private BigDecimal feeAmount;

  private String feeCurrency;

  private @Nullable Integer feeScheduleVersion;

  private @Nullable String feeTaxCategory;

  private String feeBreakdown;

  private Instant receivedAt;

  private @Nullable Instant readyAt;

  private @Nullable Instant holdUntil;

  @CreatedBy private @Nullable UUID createdBy;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected Piece() {}

  /** A piece just dropped off: numbered, priced, and RECEIVED. */
  Piece(
      int number,
      UUID makerAccountId,
      Description described,
      PieceSize size,
      FeeQuote fee,
      Instant receivedAt) {
    this.id = Uuids.v7();
    this.number = number;
    this.makerAccountId = makerAccountId;
    this.description = described.description();
    this.clayBody = described.clayBody();
    this.makerMark = described.makerMark();
    Length length = size.length();
    Length width = size.width();
    Length height = size.height();
    if (length != null && width != null && height != null) {
      LengthUnit unit = length.unit();
      if (width.unit() != unit || height.unit() != unit) {
        throw new InvalidInputException("Give all three dimensions in the same unit.");
      }
      this.lengthValue = length.value();
      this.widthValue = width.value();
      this.heightValue = height.value();
      this.lengthUnit = unit;
    }
    Weight weight = size.weight();
    if (weight != null) {
      this.weightValue = weight.value();
      this.weightUnit = weight.unit();
    }
    this.stage = PieceStage.RECEIVED;
    this.feeAmount = fee.amount().amount();
    this.feeCurrency = fee.amount().currency().getCurrencyCode();
    this.feeScheduleVersion = fee.scheduleVersion() == 0 ? null : fee.scheduleVersion();
    this.feeTaxCategory = fee.taxCategory();
    this.feeBreakdown = fee.breakdown();
    this.receivedAt = receivedAt;
  }

  /** The words a member gives a piece at drop-off, each already checked for length. */
  record Description(
      @Nullable String description, @Nullable String clayBody, @Nullable String makerMark) {}

  UUID id() {
    return id;
  }

  int number() {
    return number;
  }

  UUID makerAccountId() {
    return makerAccountId;
  }

  PieceStage stage() {
    return stage;
  }

  long version() {
    return Objects.requireNonNull(version, "A piece has a version once it's saved");
  }

  /** Throws unless the piece may move from where it is to {@code target}. */
  void checkMove(PieceStage target) {
    if (!stage.canMoveTo(target)) {
      throw new InvalidInputException("A piece can't go from %s to %s.".formatted(stage, target));
    }
  }

  /**
   * Moves the piece on, if that move is allowed from where it is. Reaching READY starts the hold,
   * until {@code holdUntil}.
   */
  void moveTo(PieceStage target, Instant now, @Nullable Instant holdUntilIfReady) {
    checkMove(target);
    if (target == PieceStage.READY) {
      this.readyAt = now;
      this.holdUntil = holdUntilIfReady;
    }
    this.stage = target;
  }

  /** The piece as the API shows it, with what the row doesn't carry passed in. */
  PieceView toView(
      @Nullable String makerName, List<PieceView.Photo> photos, List<PieceView.Event> timeline) {
    return new PieceView(
        id,
        number,
        makerAccountId,
        makerName,
        description,
        clayBody,
        makerMark,
        size(),
        stage,
        new Money(feeAmount, Currency.getInstance(feeCurrency)),
        feeScheduleVersion,
        feeTaxCategory,
        feeBreakdown,
        receivedAt,
        readyAt,
        holdUntil,
        version(),
        photos,
        timeline);
  }

  private PieceSize size() {
    LengthUnit unit = lengthUnit;
    BigDecimal lengthOf = lengthValue;
    BigDecimal widthOf = widthValue;
    BigDecimal heightOf = heightValue;
    BigDecimal weighed = weightValue;
    WeightUnit weighedIn = weightUnit;
    Weight weight = weighed == null || weighedIn == null ? null : new Weight(weighed, weighedIn);
    if (unit == null || lengthOf == null || widthOf == null || heightOf == null) {
      return new PieceSize(null, null, null, weight);
    }
    return new PieceSize(
        new Length(lengthOf, unit), new Length(widthOf, unit), new Length(heightOf, unit), weight);
  }
}
