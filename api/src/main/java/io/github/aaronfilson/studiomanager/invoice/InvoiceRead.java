package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One run of a reader on an invoice: the outcome, the tokens or pages, the cost and the time.
 * Insert-only. The platform-paid readers' monthly caps are counted from these rows.
 */
@Entity
@Table(name = "invoice_read")
@EntityListeners(AuditingEntityListener.class)
class InvoiceRead {

  /** How the run ended. */
  enum Outcome {
    READ,
    REFUSED_CREDENTIALS,
    UNAVAILABLE,
    UNREADABLE
  }

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID invoiceId;

  @Enumerated(EnumType.STRING)
  private InvoiceReaderKind reader;

  @Enumerated(EnumType.STRING)
  private Outcome outcome;

  private @Nullable String model;

  private @Nullable Integer pages;

  private @Nullable Long inputTokens;

  private @Nullable Long outputTokens;

  private @Nullable BigDecimal costUsd;

  private long elapsedMs;

  @CreatedDate private @Nullable Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected InvoiceRead() {}

  InvoiceRead(
      UUID invoiceId,
      InvoiceReaderKind reader,
      Outcome outcome,
      ReaderResult.@Nullable Usage usage,
      long elapsedMs) {
    this.id = Uuids.v7();
    this.invoiceId = invoiceId;
    this.reader = reader;
    this.outcome = outcome;
    this.elapsedMs = elapsedMs;
    if (usage != null) {
      this.model = usage.model();
      this.pages = usage.pages();
      this.inputTokens = usage.inputTokens();
      this.outputTokens = usage.outputTokens();
      this.costUsd = usage.costUsd();
    }
  }
}
