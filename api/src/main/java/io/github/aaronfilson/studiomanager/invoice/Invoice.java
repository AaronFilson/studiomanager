package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A supplier invoice in the bound studio's inbox, from upload to confirmation. Hibernate fills
 * {@code studio_id} ({@code TenantId}); auditing fills who uploaded it and when (ADR 21).
 */
@Entity
@Table(name = "invoice")
@EntityListeners(AuditingEntityListener.class)
class Invoice {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @Enumerated(EnumType.STRING)
  private InvoiceStatus status;

  @Enumerated(EnumType.STRING)
  private InvoiceFileType fileType;

  private @Nullable Long byteSize;

  @Enumerated(EnumType.STRING)
  private @Nullable InvoiceReaderKind reader;

  private @Nullable String readNote;

  private @Nullable String vendorName;

  private @Nullable String invoiceNumber;

  private @Nullable LocalDate invoiceDate;

  private @Nullable BigDecimal subtotal;

  private @Nullable BigDecimal shipping;

  private @Nullable BigDecimal tax;

  private @Nullable BigDecimal total;

  @JdbcTypeCode(SqlTypes.JSON)
  private @Nullable String raw;

  @CreatedBy private @Nullable UUID uploadedBy;

  @CreatedDate private @Nullable Instant createdAt;

  private @Nullable UUID confirmedBy;

  private @Nullable Instant confirmedAt;

  @Version private @Nullable Long version;

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position")
  private List<InvoiceLine> lines = new ArrayList<>();

  @SuppressWarnings("NullAway.Init")
  protected Invoice() {}

  Invoice(InvoiceFileType fileType) {
    this.id = Uuids.v7();
    this.status = InvoiceStatus.UPLOADING;
    this.fileType = fileType;
  }

  UUID id() {
    return id;
  }

  UUID studioId() {
    return Objects.requireNonNull(studioId, "An invoice has a studio once it's saved");
  }

  InvoiceStatus status() {
    return status;
  }

  InvoiceFileType fileType() {
    return fileType;
  }

  long version() {
    return Objects.requireNonNull(version, "An invoice has a version once it's saved");
  }

  @Nullable String vendorName() {
    return vendorName;
  }

  List<InvoiceLine> lines() {
    return List.copyOf(lines);
  }

  /** The file arrived and passed its checks: it's ready for a reader. */
  void uploaded(long size) {
    requireStatus(InvoiceStatus.UPLOADING, "This invoice's file is already in.");
    this.byteSize = size;
    this.status = InvoiceStatus.READING;
  }

  /**
   * A reader's draft, or an empty one with the reason nobody could read it. Only an invoice still
   * being read takes it, so a late answer can't overwrite what staff have already changed.
   *
   * @param reader null when the draft is empty, for staff to fill in by hand
   */
  void filled(
      InvoiceDraft draft,
      List<LineMatch> matches,
      @Nullable InvoiceReaderKind reader,
      @Nullable String raw,
      @Nullable String note) {
    requireStatus(InvoiceStatus.READING, "This invoice has already been read.");
    this.reader = reader;
    this.raw = raw;
    this.readNote = note;
    replaceDraft(draft, matches);
  }

  /** Staff's corrections to the draft, which replace it whole. */
  void edited(InvoiceDraft draft, List<LineMatch> matches) {
    switch (status) {
      case READY -> replaceDraft(draft, matches);
      case NEEDS_LOOK -> replaceDraft(draft, matches);
      case UPLOADING -> throw stillBeingRead();
      case READING -> throw stillBeingRead();
      case CONFIRMED -> throw alreadyConfirmed();
    }
  }

  /** Staff checked it: its stock is received. */
  void confirmed(UUID accountId, Instant at) {
    switch (status) {
      case READY -> markConfirmed(accountId, at);
      case NEEDS_LOOK -> markConfirmed(accountId, at);
      case UPLOADING -> throw stillBeingRead();
      case READING -> throw stillBeingRead();
      case CONFIRMED -> throw alreadyConfirmed();
    }
  }

  private void markConfirmed(UUID accountId, Instant at) {
    this.status = InvoiceStatus.CONFIRMED;
    this.confirmedBy = accountId;
    this.confirmedAt = at;
  }

  private static ConflictException stillBeingRead() {
    return new ConflictException("This invoice is still being read. Try again in a moment.");
  }

  private static ConflictException alreadyConfirmed() {
    return new ConflictException("This invoice is already confirmed.");
  }

  /** The draft as it stands, in the shape readers produce. */
  InvoiceDraft draft() {
    return new InvoiceDraft(
        vendorName,
        invoiceNumber,
        invoiceDate,
        subtotal,
        shipping,
        tax,
        total,
        lines.stream().map(InvoiceLine::toDraftLine).toList());
  }

  InvoiceView toView(List<String> problems) {
    return new InvoiceView(
        id,
        status,
        fileType,
        reader,
        readNote,
        draft(),
        lines.stream().map(InvoiceLine::toView).toList(),
        problems,
        Objects.requireNonNull(uploadedBy, "An invoice has an uploader once it's saved"),
        Objects.requireNonNull(createdAt, "An invoice has a creation time once it's saved"),
        confirmedBy,
        confirmedAt,
        version());
  }

  InvoiceView.Summary toSummary() {
    return new InvoiceView.Summary(
        id,
        status,
        fileType,
        vendorName,
        invoiceNumber,
        invoiceDate,
        total,
        Objects.requireNonNull(createdAt, "An invoice has a creation time once it's saved"));
  }

  /** A line's supply item and how it was found; both null when it has none. */
  record LineMatch(@Nullable UUID supplyItemId, SupplyMatcher.@Nullable MatchSource source) {

    static LineMatch none() {
      return new LineMatch(null, null);
    }
  }

  private void replaceDraft(InvoiceDraft draft, List<LineMatch> matches) {
    if (matches.size() != draft.lines().size()) {
      throw new IllegalArgumentException("One match per line");
    }
    this.vendorName = draft.vendorName();
    this.invoiceNumber = draft.invoiceNumber();
    this.invoiceDate = draft.invoiceDate();
    this.subtotal = draft.subtotal();
    this.shipping = draft.shipping();
    this.tax = draft.tax();
    this.total = draft.total();
    lines.clear();
    for (int position = 0; position < draft.lines().size(); position++) {
      lines.add(
          new InvoiceLine(this, position, draft.lines().get(position), matches.get(position)));
    }
    this.status =
        InvoiceChecks.problems(draft).isEmpty() ? InvoiceStatus.READY : InvoiceStatus.NEEDS_LOOK;
  }

  private void requireStatus(InvoiceStatus expected, String otherwise) {
    if (status != expected) {
      throw new ConflictException(otherwise);
    }
  }
}
