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
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * One line of a recipe, at its position. The import-only columns ({@code amount_text}, {@code
 * chemistry}) aren't mapped: nothing here writes or shows them yet.
 */
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

  @Enumerated(EnumType.STRING)
  private GlazeBook.Unit unit;

  @SuppressWarnings("NullAway.Init")
  protected GlazeLine() {}

  GlazeLine(GlazeRecipe recipe, int position, GlazeBook.Line line) {
    this.id = Uuids.v7();
    this.recipe = recipe;
    this.position = position;
    this.kind = line.kind();
    this.material = line.material();
    this.amount = line.amount();
    this.unit = line.unit();
  }

  /** Puts a different line at this position. */
  void change(GlazeBook.Line line) {
    this.kind = line.kind();
    this.material = line.material();
    this.amount = line.amount();
    this.unit = line.unit();
  }

  /** Whether this position already holds {@code line}; amounts compare by value, not scale. */
  boolean holds(GlazeBook.Line line) {
    return kind == line.kind()
        && material.equals(line.material())
        && amount.compareTo(line.amount()) == 0
        && unit == line.unit();
  }

  GlazeBook.Line toLine() {
    return new GlazeBook.Line(kind, material, amount.stripTrailingZeros(), unit);
  }
}
