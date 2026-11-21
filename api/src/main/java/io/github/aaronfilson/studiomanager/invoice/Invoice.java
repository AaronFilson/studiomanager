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
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
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

  @Enumerated(EnumType.STRING)
  private InvoiceDraft.@Nullable DocumentType documentType;

  private @Nullable String vendorName;

  private @Nullable String invoiceNumber;

  private @Nullable String orderNumber;

  private @Nullable LocalDate invoiceDate;

  private @Nullable String invoiceDatePrinted;

  @Enumerated(EnumType.STRING)
  private InvoiceDraft.@Nullable DateYearSource invoiceDateYear;

  private @Nullable LocalDate dueDate;

  private @Nullable BigDecimal subtotal;

  private @Nullable BigDecimal shipping;

  private @Nullable BigDecimal tax;

  private @Nullable BigDecimal total;

  private @Nullable BigDecimal amountPaid;

  private @Nullable BigDecimal amountDue;

  @JdbcTypeCode(SqlTypes.JSON)
  private String payments = "[]";

  @JdbcTypeCode(SqlTypes.JSON)
  private String crossedOut = "[]";

  private @Nullable String otherPapers;

  @JdbcTypeCode(SqlTypes.JSON)
  private String notes = "[]";

  private @Nullable Integer rotation;

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

  @Nullable InvoiceReaderKind reader() {
    return reader;
  }

  long version() {
    return Objects.requireNonNull(version, "An invoice has a version once it's saved");
  }

  @Nullable String vendorName() {
    return vendorName;
  }

  @Nullable String invoiceNumber() {
    return invoiceNumber;
  }

  @Nullable LocalDate invoiceDate() {
    return invoiceDate;
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
        documentType,
        vendorName,
        invoiceNumber,
        orderNumber,
        invoiceDate,
        invoiceDatePrinted,
        invoiceDateYear,
        dueDate,
        subtotal,
        shipping,
        tax,
        total,
        amountPaid,
        amountDue,
        InvoiceJson.readPayments(payments),
        InvoiceJson.readStrings(crossedOut),
        otherPapers,
        InvoiceJson.readStrings(notes),
        rotation,
        lines.stream().map(InvoiceLine::toDraftLine).toList());
  }

  InvoiceView toView(List<String> problems) {
    InvoiceDraft current = draft();
    return new InvoiceView(
        id,
        status,
        fileType,
        reader,
        readNote,
        current,
        lines.stream().map(InvoiceLine::toView).toList(),
        problems,
        InvoiceChecks.explanations(current),
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
    this.documentType = draft.documentType();
    this.vendorName = draft.vendorName();
    this.invoiceNumber = draft.invoiceNumber();
    this.orderNumber = draft.orderNumber();
    this.invoiceDate = draft.invoiceDate();
    this.invoiceDatePrinted = draft.invoiceDatePrinted();
    this.invoiceDateYear = draft.invoiceDateYear();
    this.dueDate = draft.dueDate();
    this.subtotal = draft.subtotal();
    this.shipping = draft.shipping();
    this.tax = draft.tax();
    this.total = draft.total();
    this.amountPaid = draft.amountPaid();
    this.amountDue = draft.amountDue();
    this.payments = InvoiceJson.writePayments(draft.payments());
    this.crossedOut = InvoiceJson.writeStrings(draft.crossedOut());
    this.otherPapers = draft.otherPapers();
    this.notes = InvoiceJson.writeStrings(draft.notes());
    this.rotation = draft.rotation();
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
