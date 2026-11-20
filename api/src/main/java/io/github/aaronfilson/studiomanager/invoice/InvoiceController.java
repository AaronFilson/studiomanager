package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.contract.ComparedReadDto;
import io.github.aaronfilson.studiomanager.contract.ConfirmInvoiceRequestDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceDateYearSourceDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceDocumentTypeDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceFileTypeDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceLineDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceLineKindDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceListDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceMatchSourceDto;
import io.github.aaronfilson.studiomanager.contract.InvoicePaymentDto;
import io.github.aaronfilson.studiomanager.contract.InvoicePaymentMethodDto;
import io.github.aaronfilson.studiomanager.contract.InvoicePaymentSourceDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceReadOutcomeDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceReaderDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceStatusDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceSummaryDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceUploadDto;
import io.github.aaronfilson.studiomanager.contract.InvoicesApi;
import io.github.aaronfilson.studiomanager.contract.ReaderComparisonDto;
import io.github.aaronfilson.studiomanager.contract.ReaderSkipReasonDto;
import io.github.aaronfilson.studiomanager.contract.SaveInvoiceDraftRequestDto;
import io.github.aaronfilson.studiomanager.contract.StartInvoiceUploadRequestDto;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InvoiceController implements InvoicesApi {

  private final InvoiceInbox inbox;
  private final InvoiceReview review;
  private final ReaderComparison comparison;
  private final InvoiceEvalExport evalExport;

  InvoiceController(
      InvoiceInbox inbox,
      InvoiceReview review,
      ReaderComparison comparison,
      InvoiceEvalExport evalExport) {
    this.inbox = inbox;
    this.review = review;
    this.comparison = comparison;
    this.evalExport = evalExport;
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'INVOICES_MANAGE')")
  public ResponseEntity<InvoiceListDto> listInvoices(UUID studioId) {
    Capped<InvoiceSummaryDto> listed = inbox.list(studioId).map(InvoiceController::toDto);
    return ResponseEntity.ok(
        new InvoiceListDto(listed.items(), listed.truncated(), listed.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'INVOICES_MANAGE')")
  public ResponseEntity<InvoiceUploadDto> startInvoiceUpload(
      UUID studioId, StartInvoiceUploadRequestDto request) {
    InvoiceInbox.Upload upload =
        inbox.startUpload(studioId, InvoiceFileType.valueOf(request.getFileType().getValue()));
    return ResponseEntity.created(
            URI.create("/api/v1/studios/%s/invoices/%s".formatted(studioId, upload.invoiceId())))
        .body(
            new InvoiceUploadDto(
                upload.invoiceId(),
                upload.uploadUrl(),
                utc(upload.expiresAt()),
                upload.maxBytes()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'INVOICES_MANAGE')")
  public ResponseEntity<InvoiceDto> completeInvoiceUpload(UUID studioId, UUID invoiceId) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(toDto(inbox.completeUpload(studioId, invoiceId)));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'INVOICES_MANAGE')")
  public ResponseEntity<InvoiceDto> getInvoice(UUID studioId, UUID invoiceId) {
    InvoiceInbox.Opened opened = inbox.open(studioId, invoiceId);
    return ResponseEntity.ok(toDto(opened.invoice()).fileUrl(opened.fileUrl()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'INVOICES_MANAGE')")
  public ResponseEntity<InvoiceDto> saveInvoiceDraft(
      UUID studioId, UUID invoiceId, SaveInvoiceDraftRequestDto request) {
    List<InvoiceReview.ReviewedLine> lines =
        request.getLines().stream()
            .map(line ->
                new InvoiceReview.ReviewedLine(
                    new InvoiceDraft.Line(
                        line.getDescription(),
                        line.getSku(),
                        amount(line.getQuantity()),
                        line.getUnit(),
                        amount(line.getUnitPrice()),
                        amount(line.getLineTotal()),
                        line.getKind() == null
                            ? null
                            : InvoiceDraft.LineKind.valueOf(line.getKind().getValue()),
                        nonNullStrings(line.getSerialNumbers()),
                        Boolean.TRUE.equals(line.getUnclear())),
                    line.getSupplyItemId()))
            .toList();
    List<InvoiceDraft.Payment> payments =
        request.getPayments() == null
            ? List.of()
            : request.getPayments().stream().map(InvoiceController::toPayment).toList();
    InvoiceDraft header =
        new InvoiceDraft(
            request.getDocumentType() == null
                ? null
                : InvoiceDraft.DocumentType.valueOf(request.getDocumentType().getValue()),
            request.getVendorName(),
            request.getInvoiceNumber(),
            request.getOrderNumber(),
            request.getInvoiceDate(),
            request.getInvoiceDatePrinted(),
            request.getInvoiceDateYear() == null
                ? null
                : InvoiceDraft.DateYearSource.valueOf(request.getInvoiceDateYear().getValue()),
            request.getDueDate(),
            amount(request.getSubtotal()),
            amount(request.getShipping()),
            amount(request.getTax()),
            amount(request.getTotal()),
            amount(request.getAmountPaid()),
            amount(request.getAmountDue()),
            payments,
            nonNullStrings(request.getCrossedOut()),
            request.getOtherPapers(),
            nonNullStrings(request.getNotes()),
            request.getRotation(),
            List.of());
    return ResponseEntity.ok(
        toDto(review.edit(studioId, invoiceId, header, lines, request.getExpectedVersion())));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'INVOICES_MANAGE')")
  public ResponseEntity<InvoiceDto> confirmInvoice(
      UUID studioId, UUID invoiceId, ConfirmInvoiceRequestDto request) {
    return ResponseEntity.ok(
        toDto(review.confirm(studioId, invoiceId, request.getExpectedVersion())));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'INVOICES_MANAGE')")
  public ResponseEntity<Void> discardInvoice(UUID studioId, UUID invoiceId) {
    inbox.discard(studioId, invoiceId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'STUDIO_AI_KEY_MANAGE')")
  public ResponseEntity<ReaderComparisonDto> compareInvoiceReaders(UUID studioId, UUID invoiceId) {
    return ResponseEntity.ok(
        new ReaderComparisonDto()
            .results(
                comparison.compare(studioId, invoiceId).stream()
                    .map(InvoiceController::toDto)
                    .toList()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'INVOICES_MANAGE')")
  public ResponseEntity<Resource> exportInvoiceEval(UUID studioId, UUID invoiceId) {
    byte[] zip = evalExport.zip(studioId, invoiceId);
    String filename = "invoice-eval-%s.zip".formatted(invoiceId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/zip"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(new ByteArrayResource(zip));
  }

  private static ComparedReadDto toDto(ReaderComparison.Compared compared) {
    InvoiceRead.Outcome outcome = compared.outcome();
    ComparedReadDto dto =
        new ComparedReadDto()
            .reader(InvoiceReaderDto.fromValue(compared.reader().name()))
            .outcome(
                outcome == null
                    ? InvoiceReadOutcomeDto.SKIPPED
                    : InvoiceReadOutcomeDto.fromValue(outcome.name()))
            .skipReason(
                compared.availability().ready()
                    ? null
                    : ReaderSkipReasonDto.fromValue(compared.availability().name()))
            .note(compared.note())
            .elapsedMs(compared.elapsedMs())
            .problems(compared.problems())
            .explanations(compared.explanations())
            .lines(List.of());
    ReaderResult.Usage usage = compared.usage();
    if (usage != null) {
      BigDecimal cost = usage.costUsd();
      dto.pages(usage.pages())
          .inputTokens(usage.inputTokens())
          .outputTokens(usage.outputTokens())
          .costUsd(cost == null ? null : cost.toPlainString());
    }
    InvoiceDraft draft = compared.draft();
    if (draft != null) {
      dto.documentType(
              draft.documentType() == null
                  ? null
                  : InvoiceDocumentTypeDto.fromValue(draft.documentType().name()))
          .vendorName(draft.vendorName())
          .invoiceNumber(draft.invoiceNumber())
          .invoiceDate(draft.invoiceDate())
          .subtotal(text(draft.subtotal()))
          .shipping(text(draft.shipping()))
          .tax(text(draft.tax()))
          .total(text(draft.total()))
          .payableAmount(text(draft.payableAmount()))
          .payments(draft.payments().stream().map(InvoiceController::toDto).toList())
          .lines(
              draft.lines().stream()
                  .map(line ->
                      new InvoiceLineDto()
                          .description(line.description())
                          .sku(line.sku())
                          .quantity(text(line.quantity()))
                          .unit(line.unit())
                          .unitPrice(text(line.unitPrice()))
                          .lineTotal(text(line.lineTotal()))
                          .kind(
                              line.kind() == null
                                  ? null
                                  : InvoiceLineKindDto.fromValue(line.kind().name()))
                          .serialNumbers(line.serialNumbers())
                          .unclear(line.unclear()))
                  .toList());
    }
    return dto;
  }

  private static InvoiceDto toDto(InvoiceView view) {
    InvoiceDraft draft = view.draft();
    InvoiceReaderKind reader = view.reader();
    return new InvoiceDto()
        .id(view.id())
        .status(InvoiceStatusDto.fromValue(view.status().name()))
        .fileType(InvoiceFileTypeDto.fromValue(view.fileType().name()))
        .reader(reader == null ? null : InvoiceReaderDto.fromValue(reader.name()))
        .readNote(view.readNote())
        .documentType(
            draft.documentType() == null
                ? null
                : InvoiceDocumentTypeDto.fromValue(draft.documentType().name()))
        .vendorName(draft.vendorName())
        .invoiceNumber(draft.invoiceNumber())
        .orderNumber(draft.orderNumber())
        .invoiceDate(draft.invoiceDate())
        .invoiceDatePrinted(draft.invoiceDatePrinted())
        .invoiceDateYear(
            draft.invoiceDateYear() == null
                ? null
                : InvoiceDateYearSourceDto.fromValue(draft.invoiceDateYear().name()))
        .dueDate(draft.dueDate())
        .subtotal(text(draft.subtotal()))
        .shipping(text(draft.shipping()))
        .tax(text(draft.tax()))
        .total(text(draft.total()))
        .amountPaid(text(draft.amountPaid()))
        .amountDue(text(draft.amountDue()))
        .payableAmount(text(draft.payableAmount()))
        .payments(draft.payments().stream().map(InvoiceController::toDto).toList())
        .crossedOut(draft.crossedOut())
        .otherPapers(draft.otherPapers())
        .notes(draft.notes())
        .rotation(draft.rotation())
        .lines(view.lines().stream().map(InvoiceController::toDto).toList())
        .problems(view.problems())
        .explanations(view.explanations())
        .uploadedBy(view.uploadedBy())
        .uploadedAt(utc(view.uploadedAt()))
        .confirmedBy(view.confirmedBy())
        .confirmedAt(view.confirmedAt() == null ? null : utc(view.confirmedAt()))
        .version(view.version());
  }

  private static InvoicePaymentDto toDto(InvoiceDraft.Payment payment) {
    return new InvoicePaymentDto()
        .date(payment.date())
        .amount(payment.amount().toPlainString())
        .method(
            payment.method() == null
                ? null
                : InvoicePaymentMethodDto.fromValue(payment.method().name()))
        .source(
            payment.source() == null
                ? null
                : InvoicePaymentSourceDto.fromValue(payment.source().name()));
  }

  private static InvoiceLineDto toDto(InvoiceView.Line line) {
    InvoiceDraft.Line read = line.line();
    SupplyMatcher.MatchSource source = line.matchSource();
    return new InvoiceLineDto()
        .description(read.description())
        .sku(read.sku())
        .quantity(text(read.quantity()))
        .unit(read.unit())
        .unitPrice(text(read.unitPrice()))
        .lineTotal(text(read.lineTotal()))
        .kind(InvoiceLineKindDto.fromValue(read.effectiveKind().name()))
        .serialNumbers(read.serialNumbers())
        .unclear(read.unclear())
        .supplyItemId(line.supplyItemId())
        .matchSource(source == null ? null : InvoiceMatchSourceDto.fromValue(source.name()));
  }

  private static InvoiceSummaryDto toDto(InvoiceView.Summary summary) {
    return new InvoiceSummaryDto()
        .id(summary.id())
        .status(InvoiceStatusDto.fromValue(summary.status().name()))
        .fileType(InvoiceFileTypeDto.fromValue(summary.fileType().name()))
        .vendorName(summary.vendorName())
        .invoiceNumber(summary.invoiceNumber())
        .invoiceDate(summary.invoiceDate())
        .total(text(summary.total()))
        .uploadedAt(utc(summary.uploadedAt()));
  }

  private static InvoiceDraft.Payment toPayment(InvoicePaymentDto payment) {
    BigDecimal paid = amount(payment.getAmount());
    if (paid == null) {
      throw new InvalidInputException("Each payment needs an amount.");
    }
    return new InvoiceDraft.Payment(
        payment.getDate(),
        paid,
        payment.getMethod() == null
            ? null
            : InvoiceDraft.PaymentMethod.valueOf(payment.getMethod().getValue()),
        payment.getSource() == null
            ? null
            : InvoiceDraft.PaymentSource.valueOf(payment.getSource().getValue()));
  }

  private static List<String> nonNullStrings(@Nullable List<String> values) {
    return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
  }

  /** The contract's pattern has already checked it's a plain decimal. */
  private static @Nullable BigDecimal amount(@Nullable String text) {
    return text == null || text.isBlank() ? null : new BigDecimal(text);
  }

  private static @Nullable String text(@Nullable BigDecimal amount) {
    return amount == null ? null : amount.toPlainString();
  }

  private static OffsetDateTime utc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
