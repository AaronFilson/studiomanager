package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/** One specific gravity reading recorded on a studio glaze mix bucket. */
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "glaze_mix_reading")
class GlazeMixReading {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mix_id")
  private GlazeMix mix;

  private UUID readBy;

  private Instant readAt;

  private BigDecimal specificGravity;

  private @Nullable String note;

  @SuppressWarnings("NullAway.Init")
  protected GlazeMixReading() {}

  GlazeMixReading(
      GlazeMix mix,
      UUID readBy,
      Instant readAt,
      BigDecimal specificGravity,
      @Nullable String note) {
    this.id = Uuids.v7();
    this.mix = mix;
    this.readBy = readBy;
    this.readAt = readAt;
    this.specificGravity = specificGravity;
    this.note = note;
  }

  UUID id() {
    return id;
  }

  UUID readBy() {
    return readBy;
  }

  Instant readAt() {
    return readAt;
  }

  BigDecimal specificGravity() {
    return specificGravity;
  }

  @Nullable String note() {
    return note;
  }
}
