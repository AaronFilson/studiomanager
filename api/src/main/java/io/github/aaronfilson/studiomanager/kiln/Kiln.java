package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A kiln in the bound studio. Hibernate fills {@code studio_id} from the bound studio ({@code
 * TenantId}), and auditing fills {@code created_at} (ADR 21).
 *
 * <p>The fields that are null until the kiln is first saved ({@code studioId}, {@code createdAt},
 * {@code version}) are {@code @Nullable}; their accessors say when that's a bug.
 */
@Entity
@Table(name = "kiln")
@EntityListeners(AuditingEntityListener.class)
class Kiln {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private String name;

  private @Nullable String site;

  @Enumerated(EnumType.STRING)
  private KilnView.KilnType kind;

  private @Nullable Integer maxPiecesPerMember;

  private int totalFirings;

  private @Nullable String downReason;

  @CreatedDate private @Nullable Instant createdAt;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected Kiln() {}

  Kiln(KilnService.KilnSettings settings) {
    this.id = Uuids.v7();
    this.name = settings.name();
    this.site = settings.site();
    this.kind = settings.kind();
    this.maxPiecesPerMember = settings.maxPiecesPerMember();
  }

  UUID id() {
    return id;
  }

  String name() {
    return name;
  }

  /** The queue rule "at most N pieces per member in one load"; null for no cap. */
  @Nullable Integer maxPiecesPerMember() {
    return maxPiecesPerMember;
  }

  /** Why it's out of service; null when it's available. */
  @Nullable String downReason() {
    return downReason;
  }

  long version() {
    return Objects.requireNonNull(version, "A kiln has a version once it's saved");
  }

  void change(KilnService.KilnSettings settings) {
    this.name = settings.name();
    this.site = settings.site();
    this.kind = settings.kind();
    this.maxPiecesPerMember = settings.maxPiecesPerMember();
  }

  /** Takes the kiln out of service with a reason, or with null puts it back. */
  void setDownReason(@Nullable String reason) {
    this.downReason = reason;
  }

  /** Counts one more completed firing, which maintenance tasks counted in firings read. */
  void countFiring() {
    this.totalFirings++;
  }

  KilnView toView() {
    return new KilnView(
        id, name, site, kind, maxPiecesPerMember, totalFirings, downReason, version());
  }
}
