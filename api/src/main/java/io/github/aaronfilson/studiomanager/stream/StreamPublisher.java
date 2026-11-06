package io.github.aaronfilson.studiomanager.stream;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import io.github.aaronfilson.studiomanager.kiln.FiringCompleted;
import io.github.aaronfilson.studiomanager.piece.PieceReceived;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MemberJoined;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.supplies.SupplySold;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns domain events into the stream contract, inside the transaction that caused them. The
 * stream event is published there too, so the outbox (Modulith's publication registry) records
 * it in the same commit, and the Kafka externalizer sends it after commit. A rollback sends
 * nothing; a broker that's down delays the event instead of losing it.
 *
 * <p>Only present when {@code studiomanager.stream.enabled} is true.
 */
@Component
@ConditionalOnBooleanProperty("studiomanager.stream.enabled")
class StreamPublisher {

    private final ApplicationEventPublisher events;
    private final StudioService studios;

    StreamPublisher(ApplicationEventPublisher events, StudioService studios) {
        this.events = events;
        this.studios = studios;
    }

    @EventListener
    void on(FiringCompleted firing) {
        long makers = firing.pieces().stream().map(FiringCompleted.FiredPiece::makerAccountId).distinct().count();
        events.publishEvent(new StudioStreamEvent.FiringCompleted(firing.studioId(), now(), firing.firingId(),
                firing.kind().name().toLowerCase(Locale.ROOT), firing.kilnId(), firing.kilnName(), firing.pieces().size(),
                (int) makers));
    }

    @EventListener
    void on(PieceReceived piece) {
        if (piece.fee().amount().signum() == 0) {
            return;
        }
        events.publishEvent(new StudioStreamEvent.FeeCharged(TenantContext.require().studioId(),
                piece.receivedAt().truncatedTo(ChronoUnit.SECONDS), piece.makerAccountId(), "piece", piece.pieceId(),
                piece.fee().amount().toPlainString(), piece.fee().currency().getCurrencyCode(), piece.taxCategory()));
    }

    @EventListener
    void on(SupplySold sale) {
        if (sale.amount().signum() == 0) {
            return;
        }
        events.publishEvent(new StudioStreamEvent.FeeCharged(TenantContext.require().studioId(),
                sale.soldAt().truncatedTo(ChronoUnit.SECONDS), sale.memberId(), "supply_sale", sale.movementId(),
                sale.amount().toPlainString(), studios.current().currency().getCurrencyCode(), sale.taxCategory()));
    }

    @EventListener
    void on(MemberJoined joined) {
        events.publishEvent(new StudioStreamEvent.MemberJoined(joined.studioId(), now(), joined.accountId()));
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

}
