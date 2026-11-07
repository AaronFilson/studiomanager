package io.github.aaronfilson.studiomanager.invoice;

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
 * One line of an invoice. The supply item stays a {@code UUID}: it's another module's table (ADR
 * 21, rule 2).
 */
@Entity
@Table(name = "invoice_line")
class InvoiceLine {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id")
  private Invoice invoice;

  private int position;

  private String description;

  private @Nullable String sku;

  private @Nullable BigDecimal quantity;

  private @Nullable String unit;

  private @Nullable BigDecimal unitPrice;

  private @Nullable BigDecimal lineTotal;

  private @Nullable UUID supplyItemId;

  @Enumerated(EnumType.STRING)
  private SupplyMatcher.@Nullable MatchSource matchSource;

  @SuppressWarnings("NullAway.Init")
  protected InvoiceLine() {}

  InvoiceLine(Invoice invoice, int position, InvoiceDraft.Line line, Invoice.LineMatch match) {
    this.id = Uuids.v7();
    this.invoice = invoice;
    this.position = position;
    this.description = line.description();
    this.sku = line.sku();
    this.quantity = line.quantity();
    this.unit = line.unit();
    this.unitPrice = line.unitPrice();
    this.lineTotal = line.lineTotal();
    this.supplyItemId = match.supplyItemId();
    this.matchSource = match.source();
  }

  @Nullable UUID supplyItemId() {
    return supplyItemId;
  }

  @Nullable BigDecimal quantity() {
    return quantity;
  }

  String description() {
    return description;
  }

  @Nullable String sku() {
    return sku;
  }

  SupplyMatcher.@Nullable MatchSource matchSource() {
    return matchSource;
  }

  InvoiceDraft.Line toDraftLine() {
    return new InvoiceDraft.Line(description, sku, quantity, unit, unitPrice, lineTotal);
  }

  InvoiceView.Line toView() {
    return new InvoiceView.Line(toDraftLine(), supplyItemId, matchSource);
  }
}
