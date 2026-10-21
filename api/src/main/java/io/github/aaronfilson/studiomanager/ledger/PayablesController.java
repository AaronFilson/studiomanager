package io.github.aaronfilson.studiomanager.ledger;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.ConsignmentItemDto;
import io.github.aaronfilson.studiomanager.contract.ConsignmentSaleDto;
import io.github.aaronfilson.studiomanager.contract.DiscountsFromDto;
import io.github.aaronfilson.studiomanager.contract.EarningDto;
import io.github.aaronfilson.studiomanager.contract.EarningKindDto;
import io.github.aaronfilson.studiomanager.contract.NecLineDto;
import io.github.aaronfilson.studiomanager.contract.NecReportDto;
import io.github.aaronfilson.studiomanager.contract.PayPayeeRequestDto;
import io.github.aaronfilson.studiomanager.contract.PayablesApi;
import io.github.aaronfilson.studiomanager.contract.PayeeDto;
import io.github.aaronfilson.studiomanager.contract.PayeePaymentDto;
import io.github.aaronfilson.studiomanager.contract.PayeeStatementDto;
import io.github.aaronfilson.studiomanager.contract.PayeeStatementLineDto;
import io.github.aaronfilson.studiomanager.contract.PaymentMethodDto;
import io.github.aaronfilson.studiomanager.contract.ProviderDto;
import io.github.aaronfilson.studiomanager.contract.RecordEarningRequestDto;
import io.github.aaronfilson.studiomanager.contract.ReturnConsignmentRequestDto;
import io.github.aaronfilson.studiomanager.contract.SavePayeeRequestDto;
import io.github.aaronfilson.studiomanager.contract.SellConsignmentRequestDto;
import io.github.aaronfilson.studiomanager.contract.TakeInConsignmentRequestDto;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.ConsignmentItem;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.ConsignmentSale;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.DiscountsFrom;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.EarningKind;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.NecReport;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.Payee;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PayeeChanges;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PaymentMethod;
import io.github.aaronfilson.studiomanager.platform.Csv;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PayablesController implements PayablesApi {

    private final PayablesService payables;

    PayablesController(PayablesService payables) {
        this.payables = payables;
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<PayeeDto>> listPayees(UUID studioId) {
        return ResponseEntity.ok(payables.payees().stream().map(PayablesController::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<PayeeDto> createPayee(UUID studioId, SavePayeeRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(payables.createPayee(changes(request))));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<PayeeDto> updatePayee(UUID studioId, UUID payeeId, SavePayeeRequestDto request) {
        return ResponseEntity.ok(toDto(payables.updatePayee(payeeId, changes(request))));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<PayeeStatementDto> getPayeeStatement(LocalDate from, LocalDate to, UUID studioId, UUID payeeId) {
        var statement = payables.statement(payeeId, from, to);
        return ResponseEntity.ok(new PayeeStatementDto()
                .payeeId(statement.payeeId())
                .payeeName(statement.payeeName())
                .from(statement.from())
                .to(statement.to())
                .opening(statement.opening().toPlainString())
                .lines(statement.lines().stream()
                        .map(line -> new PayeeStatementLineDto()
                                .date(line.date())
                                .kind(line.kind().name())
                                .memo(line.memo())
                                .amount(line.amount().toPlainString()))
                        .toList())
                .closing(statement.closing().toPlainString()));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<EarningDto> recordEarning(UUID studioId, UUID payeeId, RecordEarningRequestDto request) {
        var earning = payables.recordEarning(payeeId, EarningKind.valueOf(request.getKind().getValue()),
                request.getDescription(), request.getEarnedOn(), decimal(request.getAmount()),
                decimal(request.getGross()), decimal(request.getShare()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new EarningDto()
                .id(earning.id())
                .payeeId(earning.payeeId())
                .kind(EarningKindDto.fromValue(earning.kind().name()))
                .description(earning.description())
                .earnedOn(earning.earnedOn())
                .gross(plain(earning.gross()))
                .share(plain(earning.share()))
                .amount(earning.amount().toPlainString()));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<PayeePaymentDto> payPayee(UUID studioId, UUID payeeId, PayPayeeRequestDto request) {
        var payment = payables.pay(payeeId, new BigDecimal(request.getAmount()),
                PaymentMethod.valueOf(request.getMethod().getValue()), request.getReference(), request.getPaidOn());
        return ResponseEntity.status(HttpStatus.CREATED).body(new PayeePaymentDto()
                .id(payment.id())
                .payeeId(payment.payeeId())
                .paidOn(payment.paidOn())
                .amount(payment.amount().toPlainString())
                .method(PaymentMethodDto.fromValue(payment.method().name()))
                .reference(payment.reference()));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<ConsignmentItemDto>> listConsignmentItems(UUID studioId, @Nullable UUID payeeId,
            Boolean all) {
        return ResponseEntity.ok(payables.consignmentItems(payeeId, !Boolean.TRUE.equals(all)).stream()
                .map(PayablesController::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<ConsignmentItemDto> takeInConsignment(UUID studioId, TakeInConsignmentRequestDto request) {
        ConsignmentItem item = payables.takeIn(request.getPayeeId(), request.getDescription(),
                new BigDecimal(request.getRetailPrice()), request.getQuantity() == null ? 1 : request.getQuantity(),
                decimal(request.getConsignorShare()), request.getReceivedOn());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(item));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<ConsignmentSaleDto> sellConsignment(UUID studioId, UUID itemId, SellConsignmentRequestDto request) {
        ConsignmentSale sale = payables.sell(itemId, request.getQuantity() == null ? 1 : request.getQuantity(),
                decimal(request.getPrice()), Provider.valueOf(request.getProvider().getValue()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(sale));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<ConsignmentItemDto> returnConsignment(UUID studioId, UUID itemId,
            ReturnConsignmentRequestDto request) {
        return ResponseEntity.ok(toDto(payables.returnToArtist(itemId, request.getQuantity())));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<ConsignmentSaleDto>> listConsignmentSales(UUID studioId, @Nullable UUID payeeId) {
        return ResponseEntity.ok(payables.consignmentSales(payeeId).stream().map(PayablesController::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<ConsignmentSaleDto> refundConsignmentSale(UUID studioId, UUID saleId) {
        return ResponseEntity.ok(toDto(payables.refund(saleId)));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<NecReportDto> getNecReport(Integer year, UUID studioId) {
        NecReport report = payables.necReport(year);
        return ResponseEntity.ok(new NecReportDto()
                .year(report.year())
                .threshold(report.threshold().toPlainString())
                .payees(report.payees().stream()
                        .map(line -> new NecLineDto()
                                .payeeId(line.payeeId())
                                .name(line.name())
                                .email(line.email())
                                .w9ReceivedOn(line.w9ReceivedOn())
                                .reportable(line.reportable().toPlainString())
                                .paidThroughNetworks(line.paidThroughNetworks().toPlainString())
                                .meetsThreshold(line.meetsThreshold()))
                        .toList()));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<String> exportNecReport(Integer year, UUID studioId) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"1099-nec-%d.csv\"".formatted(year))
                .body(necCsv(payables.necReport(year)));
    }

    /**
     * The year's list for whoever files the forms. Tax ids aren't held here, so the filer adds
     * them from the W-9s; "Paid through networks" is shown so nobody adds it in by mistake.
     */
    static String necCsv(NecReport report) {
        Csv csv = new Csv().row(List.of("Payee", "Email", "W-9 received", "Box 1 nonemployee compensation",
                "Paid through card or payment networks (on a 1099-K; not included)", "At or over threshold"));
        for (var line : report.payees()) {
            csv.row(java.util.Arrays.asList(line.name(), line.email(),
                    line.w9ReceivedOn() == null ? "MISSING" : line.w9ReceivedOn().toString(), line.reportable(),
                    line.paidThroughNetworks(), line.meetsThreshold() ? "Yes" : "No"));
        }
        return csv.toString();
    }

    private static PayeeChanges changes(SavePayeeRequestDto request) {
        return new PayeeChanges(request.getName(), request.getEmail(), request.getAccountId(),
                Boolean.TRUE.equals(request.getFiles1099()), request.getW9ReceivedOn(),
                decimal(request.getConsignorShare()),
                request.getDiscountsFrom() == null ? DiscountsFrom.SHARED : DiscountsFrom.valueOf(request.getDiscountsFrom().getValue()),
                Boolean.TRUE.equals(request.getArchived()));
    }

    private static PayeeDto toDto(Payee payee) {
        return new PayeeDto()
                .id(payee.id())
                .name(payee.name())
                .email(payee.email())
                .accountId(payee.accountId())
                .accountName(payee.accountName())
                .files1099(payee.files1099())
                .w9ReceivedOn(payee.w9ReceivedOn())
                .consignorShare(plain(payee.consignorShare()))
                .discountsFrom(DiscountsFromDto.fromValue(payee.discountsFrom().name()))
                .archived(payee.archived())
                .owed(payee.owed().toPlainString());
    }

    private static ConsignmentItemDto toDto(ConsignmentItem item) {
        return new ConsignmentItemDto()
                .id(item.id())
                .payeeId(item.payeeId())
                .payeeName(item.payeeName())
                .description(item.description())
                .retailPrice(item.retailPrice().toPlainString())
                .consignorShare(plain(item.consignorShare()))
                .discountsFrom(DiscountsFromDto.fromValue(item.discountsFrom().name()))
                .quantity(item.quantity())
                .sold(item.sold())
                .returned(item.returned())
                .inStock(item.inStock())
                .receivedOn(item.receivedOn());
    }

    private static ConsignmentSaleDto toDto(ConsignmentSale sale) {
        return new ConsignmentSaleDto()
                .id(sale.id())
                .itemId(sale.itemId())
                .description(sale.description())
                .quantity(sale.quantity())
                .price(sale.price().toPlainString())
                .consignorAmount(sale.consignorAmount().toPlainString())
                .tax(sale.tax().toPlainString())
                .provider(ProviderDto.fromValue(sale.provider().name()))
                .soldOn(sale.soldOn())
                .refundedOn(sale.refundedOn());
    }

    private static @Nullable BigDecimal decimal(@Nullable String value) {
        return value == null ? null : new BigDecimal(value);
    }

    /** A share as the shortest decimal, so 0.6000 reads "0.6". */
    private static @Nullable String plain(@Nullable BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

}
