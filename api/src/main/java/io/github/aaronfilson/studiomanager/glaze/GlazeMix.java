package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/** One mixed bucket of a studio glaze, with its specific gravity readings over time. */
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "glaze_mix")
class GlazeMix {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID recipeId;

  private UUID mixedBy;

  private Instant mixedAt;

  private BigDecimal dryWeightG;

  private @Nullable BigDecimal waterG;

  private @Nullable String notes;

  @OneToMany(mappedBy = "mix", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("readAt ASC, id ASC")
  private List<GlazeMixReading> readings = new ArrayList<>();

  @SuppressWarnings("NullAway.Init")
  protected GlazeMix() {}

  GlazeMix(
      UUID recipeId,
      UUID mixedBy,
      Instant mixedAt,
      BigDecimal dryWeightG,
      @Nullable BigDecimal waterG,
      @Nullable String notes) {
    this.id = Uuids.v7();
    this.recipeId = recipeId;
    this.mixedBy = mixedBy;
    this.mixedAt = mixedAt;
    this.dryWeightG = dryWeightG;
    this.waterG = waterG;
    this.notes = notes;
  }

  UUID id() {
    return id;
  }

  UUID recipeId() {
    return recipeId;
  }

  UUID mixedBy() {
    return mixedBy;
  }

  Instant mixedAt() {
    return mixedAt;
  }

  BigDecimal dryWeightG() {
    return dryWeightG;
  }

  @Nullable BigDecimal waterG() {
    return waterG;
  }

  @Nullable String notes() {
    return notes;
  }

  List<GlazeMixReading> readings() {
    return List.copyOf(readings);
  }

  GlazeMixReading addReading(
      UUID readBy, Instant readAt, BigDecimal specificGravity, @Nullable String note) {
    GlazeMixReading reading = new GlazeMixReading(this, readBy, readAt, specificGravity, note);
    readings.add(reading);
    return reading;
  }
}
