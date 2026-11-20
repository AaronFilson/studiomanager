package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

/** One line of a recipe, at its position, including optional imported amount text and chemistry. */
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "glaze_line")
class GlazeLine {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipe_id")
  private GlazeRecipe recipe;

  private int position;

  @Enumerated(EnumType.STRING)
  private GlazeBook.LineKind kind;

  private String material;

  private BigDecimal amount;

  private @Nullable String amountText;

  @Enumerated(EnumType.STRING)
  private GlazeBook.Unit unit;

  @JdbcTypeCode(SqlTypes.JSON)
  private @Nullable String chemistry;

  @SuppressWarnings("NullAway.Init")
  protected GlazeLine() {}

  GlazeLine(GlazeRecipe recipe, int position, GlazeBook.Line line) {
    this.id = Uuids.v7();
    this.recipe = recipe;
    this.position = position;
    this.kind = line.kind();
    this.material = line.material();
    this.amount = line.amount();
    this.amountText = line.amountText();
    this.unit = line.unit();
    this.chemistry = GlazeChemistryJson.toJson(line.chemistry());
  }

  /** Puts a different line at this position. */
  void change(GlazeBook.Line line) {
    this.kind = line.kind();
    this.material = line.material();
    this.amount = line.amount();
    this.amountText = line.amountText();
    this.unit = line.unit();
    this.chemistry = GlazeChemistryJson.toJson(line.chemistry());
  }

  /** Whether this position already holds {@code line}; amounts compare by value, not scale. */
  boolean holds(GlazeBook.Line line) {
    return kind == line.kind()
        && material.equals(line.material())
        && amount.compareTo(line.amount()) == 0
        && Objects.equals(amountText, line.amountText())
        && unit == line.unit()
        && Objects.equals(GlazeChemistryJson.fromJson(chemistry), line.chemistry());
  }

  GlazeBook.Line toLine() {
    return new GlazeBook.Line(
        kind,
        material,
        amount.stripTrailingZeros(),
        amountText,
        unit,
        GlazeChemistryJson.fromJson(chemistry));
  }
}
