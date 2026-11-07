package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.contract.ConfirmInvoiceRequestDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceFileTypeDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceLineDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceListDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceMatchSourceDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceReaderDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceStatusDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceSummaryDto;
import io.github.aaronfilson.studiomanager.contract.InvoiceUploadDto;
import io.github.aaronfilson.studiomanager.contract.InvoicesApi;
import io.github.aaronfilson.studiomanager.contract.SaveInvoiceDraftRequestDto;
import io.github.aaronfilson.studiomanager.contract.StartInvoiceUploadRequestDto;
import io.github.aaronfilson.studiomanager.platform.Capped;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InvoiceController implements InvoicesApi {

  private final InvoiceInbox inbox;
  private final InvoiceReview review;

  InvoiceController(InvoiceInbox inbox, InvoiceReview review) {
    this.inbox = inbox;
    this.review = review;
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<InvoiceListDto> listInvoices(UUID studioId) {
    Capped<InvoiceSummaryDto> listed = inbox.list(studioId).map(InvoiceController::toDto);
    return ResponseEntity.ok(
        new InvoiceListDto(listed.items(), listed.truncated(), listed.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
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
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<InvoiceDto> completeInvoiceUpload(UUID studioId, UUID invoiceId) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(toDto(inbox.completeUpload(studioId, invoiceId)));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<InvoiceDto> getInvoice(UUID studioId, UUID invoiceId) {
    InvoiceInbox.Opened opened = inbox.open(studioId, invoiceId);
    return ResponseEntity.ok(toDto(opened.invoice()).fileUrl(opened.fileUrl()));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
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
                        amount(line.getLineTotal())),
                    line.getSupplyItemId()))
            .toList();
    InvoiceDraft header =
        new InvoiceDraft(
            request.getVendorName(),
            request.getInvoiceNumber(),
            request.getInvoiceDate(),
            amount(request.getSubtotal()),
            amount(request.getShipping()),
            amount(request.getTax()),
            amount(request.getTotal()),
            List.of());
    return ResponseEntity.ok(
        toDto(review.edit(studioId, invoiceId, header, lines, request.getExpectedVersion())));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<InvoiceDto> confirmInvoice(
      UUID studioId, UUID invoiceId, ConfirmInvoiceRequestDto request) {
    return ResponseEntity.ok(
        toDto(review.confirm(studioId, invoiceId, request.getExpectedVersion())));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<Void> discardInvoice(UUID studioId, UUID invoiceId) {
    inbox.discard(studioId, invoiceId);
    return ResponseEntity.noContent().build();
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
        .vendorName(draft.vendorName())
        .invoiceNumber(draft.invoiceNumber())
        .invoiceDate(draft.invoiceDate())
        .subtotal(text(draft.subtotal()))
        .shipping(text(draft.shipping()))
        .tax(text(draft.tax()))
        .total(text(draft.total()))
        .lines(view.lines().stream().map(InvoiceController::toDto).toList())
        .problems(view.problems())
        .uploadedBy(view.uploadedBy())
        .uploadedAt(utc(view.uploadedAt()))
        .confirmedBy(view.confirmedBy())
        .confirmedAt(view.confirmedAt() == null ? null : utc(view.confirmedAt()))
        .version(view.version());
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
