package io.github.aaronfilson.studiomanager.supplies;

import java.math.BigDecimal;
import java.net.URI;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.CreatePurchaseRequestRequestDto;
import io.github.aaronfilson.studiomanager.contract.DecidePurchaseRequestDto;
import io.github.aaronfilson.studiomanager.contract.PurchaseRequestDto;
import io.github.aaronfilson.studiomanager.contract.PurchaseRequestStatusDto;
import io.github.aaronfilson.studiomanager.contract.SaveSupplyRequestDto;
import io.github.aaronfilson.studiomanager.contract.SellSupplyRequestDto;
import io.github.aaronfilson.studiomanager.contract.StockMovementDto;
import io.github.aaronfilson.studiomanager.contract.StockMovementKindDto;
import io.github.aaronfilson.studiomanager.contract.StockMovementRequestDto;
import io.github.aaronfilson.studiomanager.contract.SuppliesApi;
import io.github.aaronfilson.studiomanager.contract.SupplyItemDto;
import io.github.aaronfilson.studiomanager.contract.UpdateSupplyRequestDto;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Category;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Item;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.ItemSettings;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.MovementKind;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.PurchaseRequest;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.RequestStatus;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SupplyController implements SuppliesApi {

    private final SupplyService supplies;

    SupplyController(SupplyService supplies) {
        this.supplies = supplies;
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<SupplyItemDto>> listSupplies(UUID studioId) {
        return ResponseEntity.ok(supplies.items().stream().map(SupplyDtos::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<SupplyItemDto> createSupply(UUID studioId, SaveSupplyRequestDto request) {
        Item item = supplies.create(settings(request));
        return ResponseEntity.created(URI.create("/api/v1/studios/%s/supplies/%s".formatted(studioId, item.id())))
                .body(SupplyDtos.toDto(item));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<SupplyItemDto> updateSupply(UUID studioId, UUID itemId, UpdateSupplyRequestDto request) {
        return ResponseEntity.ok(SupplyDtos.toDto(supplies.update(itemId, settings(request.getItem()),
                request.getExpectedVersion())));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<StockMovementDto>> listStockMovements(UUID studioId, UUID itemId) {
        return ResponseEntity.ok(supplies.movements(itemId).stream()
                .map(m -> new StockMovementDto()
                        .id(m.id())
                        .kind(StockMovementKindDto.fromValue(m.kind().name()))
                        .quantity(m.quantity().stripTrailingZeros().toPlainString())
                        .memberId(m.memberId())
                        .memberName(m.memberName())
                        .note(m.note())
                        .createdAt(m.createdAt().atOffset(ZoneOffset.UTC)))
                .toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<SupplyItemDto> recordStockMovement(UUID studioId, UUID itemId, StockMovementRequestDto request) {
        return ResponseEntity.ok(SupplyDtos.toDto(supplies.recordMovement(itemId,
                MovementKind.valueOf(request.getKind().getValue()), new BigDecimal(request.getQuantity()),
                request.getNote())));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<SupplyItemDto> sellSupply(UUID studioId, UUID itemId, SellSupplyRequestDto request) {
        return ResponseEntity.ok(SupplyDtos.toDto(supplies.sell(itemId, request.getMemberId(),
                new BigDecimal(request.getQuantity()))));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<PurchaseRequestDto>> listPurchaseRequests(UUID studioId) {
        return ResponseEntity.ok(supplies.openRequests().stream().map(SupplyController::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<PurchaseRequestDto> createPurchaseRequest(UUID studioId, CreatePurchaseRequestRequestDto request) {
        PurchaseRequest created = supplies.request(request.getItemId(),
                request.getQuantity() == null ? null : new BigDecimal(request.getQuantity()), request.getNote());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<PurchaseRequestDto> decidePurchaseRequest(UUID studioId, UUID requestId,
            DecidePurchaseRequestDto request) {
        return ResponseEntity.ok(toDto(supplies.decide(requestId, RequestStatus.valueOf(request.getTo().getValue()),
                request.getExpectedVersion())));
    }

    private static ItemSettings settings(SaveSupplyRequestDto r) {
        return new ItemSettings(r.getName(), Category.valueOf(r.getCategory().getValue()), r.getUnit(), r.getSupplier(),
                r.getLeadTimeDays(), decimal(r.getParLevel()), decimal(r.getReorderQuantity()), decimal(r.getSalePrice()),
                r.getTaxCategory(), r.getSubstitute());
    }

    private static @Nullable BigDecimal decimal(@Nullable String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static PurchaseRequestDto toDto(PurchaseRequest r) {
        return new PurchaseRequestDto()
                .id(r.id())
                .itemId(r.itemId())
                .itemName(r.itemName())
                .quantity(r.quantity().stripTrailingZeros().toPlainString())
                .note(r.note())
                .status(PurchaseRequestStatusDto.fromValue(r.status().name()))
                .requestedBy(r.requestedBy())
                .requestedByName(r.requestedByName())
                .requestedAt(r.requestedAt().atOffset(ZoneOffset.UTC))
                .decidedByName(r.decidedByName())
                .version(r.version());
    }

}
