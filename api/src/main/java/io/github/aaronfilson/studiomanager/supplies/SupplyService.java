package io.github.aaronfilson.studiomanager.supplies;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Category;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Item;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.ItemSettings;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Movement;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.MovementKind;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.PurchaseRequest;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.RequestStatus;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clay and supplies in the bound studio. Stock on hand is the sum of movements, never a stored
 * number someone can forget to update.
 */
@Service
public class SupplyService {

    private static final String SELECT_ITEM = """
            select i.id, i.name, i.category, i.unit, i.supplier, i.lead_time_days, i.par_level, i.reorder_quantity,
                   i.sale_price, i.tax_category, i.substitute, i.version,
                   coalesce((select sum(m.quantity) from stock_movement m where m.item_id = i.id), 0) as on_hand,
                   coalesce((select sum(r.quantity) from purchase_request r
                             where r.item_id = i.id and r.status in ('APPROVED', 'ORDERED')), 0) as on_order
            from supply_item i
            """;

    private final JdbcClient jdbc;
    private final StudioMembers members;
    private final StudioService studios;
    private final AccountDirectory accounts;
    private final ApplicationEventPublisher events;

    SupplyService(JdbcClient jdbc, StudioMembers members, StudioService studios, AccountDirectory accounts,
            ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.members = members;
        this.studios = studios;
        this.accounts = accounts;
        this.events = events;
    }

    /** Every item, low stock first. Members see the list too, so they know what's in and what to use instead. */
    @Transactional(readOnly = true)
    public List<Item> items() {
        return jdbc.sql(SELECT_ITEM + "where not i.archived order by i.category, i.name")
                .query(SupplyService::mapItem).list().stream()
                .sorted((a, b) -> Boolean.compare(b.low(), a.low()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Item item(UUID id) {
        return jdbc.sql(SELECT_ITEM + "where i.id = :id").param("id", id)
                .query(SupplyService::mapItem).optional()
                .orElseThrow(() -> new NotFoundException("No such item."));
    }

    /** Items at or below their par level with nothing already on order. */
    @Transactional(readOnly = true)
    public List<Item> needingOrder() {
        return items().stream().filter(Item::needsOrdering).toList();
    }

    @Transactional
    public Item create(ItemSettings settings) {
        TenantContext caller = requireStaff();
        validate(settings);
        UUID id = Uuids.v7();
        try {
            bind(jdbc.sql("""
                    insert into supply_item (id, studio_id, name, category, unit, supplier, lead_time_days, par_level,
                                             reorder_quantity, sale_price, tax_category, substitute, version)
                    values (:id, :studioId, :name, :category, :unit, :supplier, :lead, :par, :reorder, :price, :tax,
                            :substitute, 0)
                    """), settings)
                    .param("id", id)
                    .param("studioId", caller.studioId())
                    .update();
        } catch (DuplicateKeyException e) {
            throw new InvalidInputException("There's already an item called '%s'.".formatted(settings.name().strip()));
        }
        return item(id);
    }

    @Transactional
    public Item update(UUID id, ItemSettings settings, long expectedVersion) {
        requireStaff();
        validate(settings);
        int updated;
        try {
            updated = bind(jdbc.sql("""
                    update supply_item set name = :name, category = :category, unit = :unit, supplier = :supplier,
                           lead_time_days = :lead, par_level = :par, reorder_quantity = :reorder, sale_price = :price,
                           tax_category = :tax, substitute = :substitute, version = version + 1
                    where id = :id and version = :version
                    """), settings)
                    .param("id", id)
                    .param("version", expectedVersion)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new InvalidInputException("There's already an item called '%s'.".formatted(settings.name().strip()));
        }
        if (updated == 0) {
            item(id);
            throw new ConflictException("Someone else changed this item at the same time. Reload and try again.");
        }
        return item(id);
    }

    /**
     * Records stock coming in, being used, or a count. For a count, {@code quantity} is what is on
     * the shelf, and the movement records the difference from the books.
     */
    @Transactional
    public Item recordMovement(UUID itemId, MovementKind kind, BigDecimal quantity, @Nullable String note) {
        TenantContext caller = requireStaff();
        Item item = item(itemId);
        BigDecimal change = switch (kind) {
            case RECEIVED -> positive(quantity);
            case USED -> positive(quantity).negate();
            case COUNTED -> {
                if (quantity.signum() < 0) {
                    throw new InvalidInputException("A count can't be negative.");
                }
                yield quantity.subtract(item.onHand());
            }
            case SOLD -> throw new InvalidInputException("Record sales with the sale endpoint, so the member is charged.");
        };
        if (change.signum() != 0) {
            insertMovement(caller, itemId, kind, change, null, note);
        }
        return item(itemId);
    }

    /** Sells an item to a member: stock goes down, and the price (plus tax) goes on their balance. */
    @Transactional
    public Item sell(UUID itemId, UUID memberId, BigDecimal quantity) {
        TenantContext caller = requireStaff();
        Item item = item(itemId);
        if (item.salePrice() == null) {
            throw new InvalidInputException("%s isn't for sale; give it a price first.".formatted(item.name()));
        }
        if (members.roleOf(memberId).isEmpty()) {
            throw new InvalidInputException("That person isn't a member of this studio.");
        }
        BigDecimal amount = item.salePrice().multiply(positive(quantity))
                .setScale(studios.current().currency().getDefaultFractionDigits(), RoundingMode.HALF_UP);
        Instant now = now();
        UUID movementId = insertMovement(caller, itemId, MovementKind.SOLD, quantity.negate(), memberId, null);
        events.publishEvent(new SupplySold(movementId, memberId, item.name(), quantity, amount, item.taxCategory(), now));
        return item(itemId);
    }

    @Transactional(readOnly = true)
    public List<Movement> movements(UUID itemId) {
        requireStaff();
        item(itemId);
        List<Movement> raw = jdbc.sql("""
                select id, kind, quantity, member_id, note, created_at from stock_movement
                where item_id = :id order by created_at desc limit 200
                """)
                .param("id", itemId)
                .query((rs, row) -> new Movement(rs.getObject("id", UUID.class), MovementKind.valueOf(rs.getString("kind")),
                        rs.getBigDecimal("quantity"), rs.getObject("member_id", UUID.class), null, rs.getString("note"),
                        instant(rs, "created_at")))
                .list();
        Map<UUID, String> names = accounts.displayNames(raw.stream().map(Movement::memberId).filter(Objects::nonNull).toList());
        return raw.stream()
                .map(m -> new Movement(m.id(), m.kind(), m.quantity(), m.memberId(),
                        m.memberId() == null ? null : names.get(m.memberId()), m.note(), m.createdAt()))
                .toList();
    }

    // ---- Purchase requests

    /** Asks for more of an item. Without a quantity, the item's usual reorder quantity. */
    @Transactional
    public PurchaseRequest request(UUID itemId, @Nullable BigDecimal quantity, @Nullable String note) {
        TenantContext caller = requireStaff();
        Item item = item(itemId);
        BigDecimal amount = quantity != null ? positive(quantity) : item.reorderQuantity();
        if (amount == null) {
            throw new InvalidInputException("How many? This item has no usual reorder quantity.");
        }
        UUID id = Uuids.v7();
        jdbc.sql("""
                insert into purchase_request (id, studio_id, item_id, quantity, note, status, requested_by, requested_at, version)
                values (:id, :studioId, :item, :quantity, :note, 'REQUESTED', :by, :now, 0)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("item", itemId)
                .param("quantity", amount)
                .param("note", clean(note))
                .param("by", caller.accountId())
                .param("now", utc(now()))
                .update();
        return request(id);
    }

    /**
     * Moves a request on. The owner approves or rejects; staff mark approved requests ordered and
     * then received, which puts the stock on the shelf.
     */
    @Transactional
    public PurchaseRequest decide(UUID requestId, RequestStatus to, long expectedVersion) {
        TenantContext caller = requireStaff();
        PurchaseRequest request = request(requestId);
        boolean owner = caller.role() == StudioRole.OWNER;
        boolean allowed = switch (to) {
            case APPROVED, REJECTED -> request.status() == RequestStatus.REQUESTED && owner;
            case ORDERED -> request.status() == RequestStatus.APPROVED;
            case RECEIVED -> request.status() == RequestStatus.APPROVED || request.status() == RequestStatus.ORDERED;
            case REQUESTED -> false;
        };
        if (!allowed) {
            if ((to == RequestStatus.APPROVED || to == RequestStatus.REJECTED) && !owner) {
                throw new AccessDeniedException("Only the owner approves purchases.");
            }
            throw new InvalidInputException("A %s request can't become %s."
                    .formatted(request.status().name().toLowerCase(), to.name().toLowerCase()));
        }
        boolean decision = to == RequestStatus.APPROVED || to == RequestStatus.REJECTED;
        int updated = jdbc.sql("""
                update purchase_request set status = :to, version = version + 1,
                       decided_by = case when :decision then :me else decided_by end,
                       decided_at = case when :decision then :now else decided_at end
                where id = :id and version = :version
                """)
                .param("to", to.name())
                .param("decision", decision)
                .param("me", caller.accountId())
                .param("now", utc(now()))
                .param("id", requestId)
                .param("version", expectedVersion)
                .update();
        if (updated == 0) {
            throw new ConflictException("Someone else changed this request at the same time. Reload and try again.");
        }
        if (to == RequestStatus.RECEIVED) {
            insertMovement(caller, request.itemId(), MovementKind.RECEIVED, request.quantity(), null, "Purchase received");
        }
        return request(requestId);
    }

    /** Open requests (requested, approved, ordered), oldest first. */
    @Transactional(readOnly = true)
    public List<PurchaseRequest> openRequests() {
        requireStaff();
        return requests("where r.status in ('REQUESTED', 'APPROVED', 'ORDERED') order by r.requested_at", Map.of());
    }

    private PurchaseRequest request(UUID id) {
        return requests("where r.id = :id", Map.of("id", id)).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("No such purchase request."));
    }

    private List<PurchaseRequest> requests(String where, Map<String, Object> params) {
        var spec = jdbc.sql("""
                select r.id, r.item_id, i.name as item_name, r.quantity, r.note, r.status, r.requested_by, r.requested_at,
                       r.decided_by, r.version
                from purchase_request r join supply_item i on i.id = r.item_id
                """ + where);
        for (var param : params.entrySet()) {
            spec = spec.param(param.getKey(), param.getValue());
        }
        List<UUID> deciders = new ArrayList<>();
        List<PurchaseRequest> raw = spec.query((rs, row) -> {
            deciders.add(rs.getObject("decided_by", UUID.class));
            return new PurchaseRequest(rs.getObject("id", UUID.class), rs.getObject("item_id", UUID.class),
                    rs.getString("item_name"), rs.getBigDecimal("quantity"), rs.getString("note"),
                    RequestStatus.valueOf(rs.getString("status")), rs.getObject("requested_by", UUID.class), null,
                    instant(rs, "requested_at"), null, rs.getLong("version"));
        }).list();
        List<UUID> ids = new ArrayList<>(raw.stream().map(PurchaseRequest::requestedBy).toList());
        deciders.stream().filter(Objects::nonNull).forEach(ids::add);
        Map<UUID, String> names = accounts.displayNames(ids);
        List<PurchaseRequest> named = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            PurchaseRequest r = raw.get(i);
            UUID decider = deciders.get(i);
            named.add(new PurchaseRequest(r.id(), r.itemId(), r.itemName(), r.quantity(), r.note(), r.status(),
                    r.requestedBy(), names.get(r.requestedBy()), r.requestedAt(),
                    decider == null ? null : names.get(decider), r.version()));
        }
        return named;
    }

    // ---- Internals

    private UUID insertMovement(TenantContext caller, UUID itemId, MovementKind kind, BigDecimal quantity,
            @Nullable UUID memberId, @Nullable String note) {
        UUID id = Uuids.v7();
        jdbc.sql("""
                insert into stock_movement (id, studio_id, item_id, kind, quantity, member_id, note, created_by, created_at)
                values (:id, :studioId, :item, :kind, :quantity, :member, :note, :by, :now)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("item", itemId)
                .param("kind", kind.name())
                .param("quantity", quantity)
                .param("member", memberId)
                .param("note", clean(note))
                .param("by", caller.accountId())
                .param("now", utc(now()))
                .update();
        return id;
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, ItemSettings s) {
        return spec
                .param("name", s.name().strip())
                .param("category", s.category().name())
                .param("unit", s.unit().strip())
                .param("supplier", clean(s.supplier()))
                .param("lead", s.leadTimeDays())
                .param("par", s.parLevel())
                .param("reorder", s.reorderQuantity())
                .param("price", s.salePrice())
                .param("tax", clean(s.taxCategory()))
                .param("substitute", clean(s.substitute()));
    }

    private static void validate(ItemSettings s) {
        if (s.name() == null || s.name().isBlank() || s.name().strip().length() > 80) {
            throw new InvalidInputException("Name the item in 1 to 80 characters.");
        }
        if (s.unit() == null || s.unit().isBlank() || s.unit().strip().length() > 30) {
            throw new InvalidInputException("Say what one unit is, such as \"25 lb bag\" (up to 30 characters).");
        }
        if (s.leadTimeDays() != null && (s.leadTimeDays() < 0 || s.leadTimeDays() > 365)) {
            throw new InvalidInputException("Lead time is 0 to 365 days.");
        }
        if (s.salePrice() != null && s.salePrice().signum() < 0) {
            throw new InvalidInputException("A price can't be negative.");
        }
        if (s.reorderQuantity() != null && s.reorderQuantity().signum() <= 0) {
            throw new InvalidInputException("A reorder quantity must be more than zero.");
        }
    }

    private static BigDecimal positive(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new InvalidInputException("The quantity must be more than zero.");
        }
        return quantity;
    }

    private static @Nullable String clean(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String stripped = text.strip();
        return stripped.length() > 200 ? stripped.substring(0, 200) : stripped;
    }

    private static Item mapItem(ResultSet rs, int row) throws SQLException {
        return new Item(rs.getObject("id", UUID.class), rs.getString("name"), Category.valueOf(rs.getString("category")),
                rs.getString("unit"), rs.getString("supplier"), rs.getObject("lead_time_days", Integer.class),
                rs.getBigDecimal("par_level"), rs.getBigDecimal("reorder_quantity"), rs.getBigDecimal("sale_price"),
                rs.getString("tax_category"), rs.getString("substitute"), rs.getBigDecimal("on_hand"),
                rs.getBigDecimal("on_order"), rs.getLong("version"));
    }

    private static TenantContext requireStaff() {
        TenantContext caller = TenantContext.require();
        if (caller.role() == null || !caller.role().includes(StudioRole.STAFF)) {
            throw new AccessDeniedException("Only staff can do this.");
        }
        return caller;
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

}
