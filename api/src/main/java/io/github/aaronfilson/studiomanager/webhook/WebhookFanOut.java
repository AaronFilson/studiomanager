package io.github.aaronfilson.studiomanager.webhook;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.aaronfilson.studiomanager.kiln.FiringCompleted;
import io.github.aaronfilson.studiomanager.piece.PieceReceived;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MemberJoined;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.supplies.SupplySold;

import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Turns what happened in the studio into webhook events. Each payload carries what a Zap needs
 * without calling back: names and email addresses included, which the Webhooks page warns about.
 *
 * <p>Most run inside the transaction that caused them, so a rollback sends nothing. A member
 * joining happens outside any studio, so that one runs after commit, bound to the studio, and the
 * outbox retries it if it fails. Every event has a dedupe key, so running twice adds nothing.
 */
@Component
class WebhookFanOut {

    private final WebhookEvents events;
    private final AccountDirectory accounts;
    private final StudioService studios;
    private final StudioMembers members;
    private final TransactionOperations transactions;

    WebhookFanOut(WebhookEvents events, AccountDirectory accounts, StudioService studios, StudioMembers members,
            TransactionOperations transactions) {
        this.events = events;
        this.accounts = accounts;
        this.studios = studios;
        this.members = members;
        this.transactions = transactions;
    }

    /** One event per firing, grouped by maker the way the notices are, so a Zap runs once, not once per pot. */
    @EventListener
    void on(FiringCompleted firing) {
        Map<UUID, List<FiringCompleted.FiredPiece>> byMaker = firing.pieces().stream()
                .collect(Collectors.groupingBy(FiringCompleted.FiredPiece::makerAccountId, LinkedHashMap::new,
                        Collectors.toList()));
        Set<UUID> makers = byMaker.keySet();
        Map<UUID, String> names = accounts.displayNames(makers);
        Map<UUID, String> emails = accounts.emails(makers);
        List<Map<String, Object>> makerList = new ArrayList<>();
        byMaker.forEach((maker, pieces) -> {
            List<Map<String, Object>> pieceList = pieces.stream()
                    .sorted(Comparator.comparingInt(FiringCompleted.FiredPiece::number))
                    .map(piece -> {
                        Map<String, Object> p = new LinkedHashMap<>();
                        p.put("id", piece.pieceId().toString());
                        p.put("number", piece.number());
                        p.put("description", piece.description());
                        p.put("collect_by", piece.holdUntil() == null ? null : piece.holdUntil().toString());
                        return p;
                    }).toList();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("member", member(maker, names, emails));
            m.put("pieces", pieceList);
            makerList.add(m);
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("firing", Map.of("id", firing.firingId().toString(), "kind", firing.kind().name().toLowerCase(),
                "kiln", firing.kilnName(), "unloaded_at", now().toString()));
        data.put("makers", makerList);
        events.publish(WebhookEventType.PIECES_READY, "pieces.ready:" + firing.firingId(), data);
    }

    @EventListener
    void on(PieceReceived piece) {
        if (piece.fee().amount().signum() == 0) {
            return;
        }
        Map<String, Object> data = charge(piece.makerAccountId(), "Firing fee, piece " + piece.number(),
                piece.fee().amount().toPlainString(), piece.fee().currency().getCurrencyCode(), piece.taxCategory(),
                piece.receivedAt());
        data.put("source", Map.of("kind", "piece", "id", piece.pieceId().toString(), "number", piece.number()));
        events.publish(WebhookEventType.FEE_CHARGED, "fee.charged:piece:" + piece.pieceId(), data);
    }

    @EventListener
    void on(SupplySold sale) {
        if (sale.amount().signum() == 0) {
            return;
        }
        Map<String, Object> data = charge(sale.memberId(), sale.itemName() + " × " + sale.quantity().stripTrailingZeros()
                .toPlainString(), sale.amount().toPlainString(), studios.current().currency().getCurrencyCode(),
                sale.taxCategory(), sale.soldAt());
        data.put("source", Map.of("kind", "supply_sale", "id", sale.movementId().toString(), "item", sale.itemName(),
                "quantity", sale.quantity().stripTrailingZeros().toPlainString()));
        events.publish(WebhookEventType.FEE_CHARGED, "fee.charged:supply:" + sale.movementId(), data);
    }

    /** Runs after the invite's acceptance commits, bound to the studio it joined. */
    @Async
    @TransactionalEventListener
    void on(MemberJoined joined) {
        TenantContext member = TenantContext.forAccount(joined.accountId()).inStudio(joined.studioId(), StudioRole.MEMBER);
        TenantContext.callAs(member, () -> transactions.execute(tx -> {
            Set<UUID> ids = Set.of(joined.accountId());
            Map<String, Object> data = new LinkedHashMap<>();
            Map<String, Object> m = member(joined.accountId(), accounts.displayNames(ids), accounts.emails(ids));
            m.put("role", members.roleOf(joined.accountId()).map(role -> role.name().toLowerCase()).orElse("member"));
            data.put("member", m);
            data.put("joined_at", now().toString());
            events.publish(WebhookEventType.MEMBER_JOINED, "member.joined:" + joined.accountId(), data);
            return null;
        }));
    }

    private Map<String, Object> charge(UUID memberId, String description, String amount, String currency,
            @Nullable String taxCategory, Instant at) {
        Set<UUID> ids = Set.of(memberId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("member", member(memberId, accounts.displayNames(ids), accounts.emails(ids)));
        data.put("description", description);
        data.put("amount", amount);
        data.put("currency", currency);
        data.put("tax_category", taxCategory);
        data.put("charged_at", at.truncatedTo(ChronoUnit.SECONDS).toString());
        return data;
    }

    private static Map<String, Object> member(UUID id, Map<UUID, String> names, Map<UUID, String> emails) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id.toString());
        m.put("name", names.get(id));
        m.put("email", emails.get(id));
        return m;
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

}
