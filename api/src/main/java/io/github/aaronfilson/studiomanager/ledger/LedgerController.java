package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.contract.ClearingBalanceDto;
import io.github.aaronfilson.studiomanager.contract.ImportPayoutRequestDto;
import io.github.aaronfilson.studiomanager.contract.LedgerAccountDto;
import io.github.aaronfilson.studiomanager.contract.LedgerApi;
import io.github.aaronfilson.studiomanager.contract.MemberBalanceDto;
import io.github.aaronfilson.studiomanager.contract.PayoutDto;
import io.github.aaronfilson.studiomanager.contract.PayoutListDto;
import io.github.aaronfilson.studiomanager.contract.ProviderDto;
import io.github.aaronfilson.studiomanager.contract.RecordPaymentRequestDto;
import io.github.aaronfilson.studiomanager.contract.SellStoredValueRequestDto;
import io.github.aaronfilson.studiomanager.contract.SetTaxRateRequestDto;
import io.github.aaronfilson.studiomanager.contract.StatementDto;
import io.github.aaronfilson.studiomanager.contract.StoredValueDto;
import io.github.aaronfilson.studiomanager.contract.StoredValueKindDto;
import io.github.aaronfilson.studiomanager.contract.StoredValueListDto;
import io.github.aaronfilson.studiomanager.contract.StoredValueMovementDto;
import io.github.aaronfilson.studiomanager.contract.SummaryLineDto;
import io.github.aaronfilson.studiomanager.contract.TaxRateDto;
import io.github.aaronfilson.studiomanager.contract.UseStoredValueRequestDto;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.MemberBalance;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Payout;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.StoredValueBalance;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.StoredValueKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.TaxRate;
import io.github.aaronfilson.studiomanager.platform.Capped;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LedgerController implements LedgerApi {

  private final LedgerService ledger;
  private final StoredValues storedValues;
  private final Payouts payouts;
  private final TaxRates taxRates;

  LedgerController(
      LedgerService ledger, StoredValues storedValues, Payouts payouts, TaxRates taxRates) {
    this.ledger = ledger;
    this.storedValues = storedValues;
    this.payouts = payouts;
    this.taxRates = taxRates;
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<List<MemberBalanceDto>> listBalances(UUID studioId) {
    return ResponseEntity.ok(ledger.balances().stream().map(LedgerController::toDto).toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<StatementDto> getStatement(UUID studioId, UUID accountId) {
    MemberBalance balance = ledger.balanceOf(accountId);
    return ResponseEntity.ok(
        new StatementDto()
            .memberId(accountId)
            .owed(balance.owed().toPlainString())
            .storedValues(
                storedValues.storedValuesOf(accountId).stream()
                    .map(LedgerController::toDto)
                    .toList()));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<MemberBalanceDto> recordPayment(
      UUID studioId, RecordPaymentRequestDto request) {
    return ResponseEntity.ok(
        toDto(
            ledger.recordPayment(
                request.getMemberId(),
                new BigDecimal(request.getAmount()),
                Provider.valueOf(request.getProvider().getValue()),
                request.getReference())));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<StoredValueListDto> listStoredValues(UUID studioId) {
    Capped<StoredValueDto> balances = storedValues.storedValues().map(LedgerController::toDto);
    return ResponseEntity.ok(
        new StoredValueListDto(balances.items(), balances.truncated(), balances.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<StoredValueDto> sellStoredValue(
      UUID studioId, SellStoredValueRequestDto request) {
    StoredValueBalance sold =
        storedValues.sellStoredValue(
            StoredValueKind.valueOf(request.getKind().getValue()),
            request.getHolder(),
            request.getLabel(),
            new BigDecimal(request.getAmount()),
            Provider.valueOf(request.getProvider().getValue()),
            request.getExpiresOn());
    return ResponseEntity.status(HttpStatus.CREATED).body(toDto(sold));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<StoredValueDto> findGiftCard(UUID studioId, String code) {
    return ResponseEntity.ok(toDto(storedValues.giftCard(code)));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<StoredValueDto> useStoredValue(
      UUID studioId, UUID storedValueId, UseStoredValueRequestDto request) {
    return ResponseEntity.ok(
        toDto(
            storedValues.useStoredValue(
                storedValueId, request.getMemberId(), new BigDecimal(request.getAmount()))));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<StoredValueDto> expireStoredValue(UUID studioId, UUID storedValueId) {
    return ResponseEntity.ok(toDto(storedValues.expireStoredValue(storedValueId)));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<List<StoredValueMovementDto>> getStoredValueMovements(
      LocalDate from, LocalDate to, UUID studioId) {
    return ResponseEntity.ok(
        storedValues.storedValueMovements(from, to).stream()
            .map(m ->
                new StoredValueMovementDto()
                    .kind(StoredValueKindDto.fromValue(m.kind().name()))
                    .opening(m.opening().toPlainString())
                    .sold(m.sold().toPlainString())
                    .used(m.used().toPlainString())
                    .expired(m.expired().toPlainString())
                    .closing(m.closing().toPlainString()))
            .toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<PayoutListDto> listPayouts(UUID studioId) {
    Capped<PayoutDto> listed = payouts.payouts().map(LedgerController::toDto);
    return ResponseEntity.ok(new PayoutListDto(listed.items(), listed.truncated(), listed.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<PayoutDto> importPayout(UUID studioId, ImportPayoutRequestDto request) {
    return ResponseEntity.ok(
        toDto(
            payouts.importPayout(
                Provider.valueOf(request.getProvider().getValue()),
                request.getExternalId(),
                request.getArrivalOn(),
                new BigDecimal(request.getGross()),
                new BigDecimal(request.getRefunds()),
                new BigDecimal(request.getFees()),
                new BigDecimal(request.getAdjustments()),
                new BigDecimal(request.getNet()))));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<List<ClearingBalanceDto>> getClearing(UUID studioId) {
    return ResponseEntity.ok(
        payouts.clearing().stream()
            .map(c ->
                new ClearingBalanceDto()
                    .provider(ProviderDto.fromValue(c.provider().name()))
                    .awaitingPayout(c.awaitingPayout().toPlainString()))
            .toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<List<SummaryLineDto>> getLedgerSummary(
      LocalDate from, LocalDate to, UUID studioId) {
    return ResponseEntity.ok(
        ledger.dailySummary(from, to).stream()
            .map(line ->
                new SummaryLineDto()
                    .date(line.date())
                    .account(LedgerAccountDto.fromValue(line.account().name()))
                    .taxCategory(line.taxCategory())
                    .debits(line.debits().toPlainString())
                    .credits(line.credits().toPlainString()))
            .toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<String> exportJournal(LocalDate from, LocalDate to, UUID studioId) {
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"studio-journal-%s-to-%s.csv\"".formatted(from, to))
        .body(JournalExport.csv(ledger.dailySummary(from, to)));
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<List<TaxRateDto>> listTaxRates(UUID studioId) {
    return ResponseEntity.ok(taxRates.taxRates().stream().map(LedgerController::toDto).toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<TaxRateDto> setTaxRate(UUID studioId, SetTaxRateRequestDto request) {
    return ResponseEntity.ok(
        toDto(
            taxRates.setTaxRate(
                request.getTaxCategory(),
                new BigDecimal(request.getRate()),
                request.getEffectiveFrom())));
  }

  private static MemberBalanceDto toDto(MemberBalance balance) {
    return new MemberBalanceDto()
        .memberId(balance.memberId())
        .memberName(balance.memberName())
        .owed(balance.owed().toPlainString());
  }

  private static StoredValueDto toDto(StoredValueBalance sv) {
    return new StoredValueDto()
        .id(sv.id())
        .kind(StoredValueKindDto.fromValue(sv.kind().name()))
        .holder(sv.holder())
        .holderName(sv.holderName())
        .code(sv.code())
        .label(sv.label())
        .expiresOn(sv.expiresOn())
        .remaining(sv.remaining().toPlainString());
  }

  private static PayoutDto toDto(Payout payout) {
    return new PayoutDto()
        .id(payout.id())
        .provider(ProviderDto.fromValue(payout.provider().name()))
        .externalId(payout.externalId())
        .arrivalOn(payout.arrivalOn())
        .gross(payout.gross().toPlainString())
        .refunds(payout.refunds().toPlainString())
        .fees(payout.fees().toPlainString())
        .adjustments(payout.adjustments().toPlainString())
        .net(payout.net().toPlainString())
        .currency(payout.currency());
  }

  private static TaxRateDto toDto(TaxRate rate) {
    return new TaxRateDto()
        .id(rate.id())
        .taxCategory(rate.taxCategory())
        .rate(rate.rate().toPlainString())
        .effectiveFrom(rate.effectiveFrom());
  }
}
