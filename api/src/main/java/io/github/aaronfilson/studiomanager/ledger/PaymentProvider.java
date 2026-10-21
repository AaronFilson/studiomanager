package io.github.aaronfilson.studiomanager.ledger;

import java.math.BigDecimal;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;

/**
 * Charges cards through one processor (Wave 2). The ledger never depends on which: a charge
 * lands in {@link LedgerAccount#PROCESSOR_CLEARING} tagged with the provider, and the provider's
 * payouts clear it, exactly as payouts imported from any other tool a studio uses.
 *
 * <p>The first implementation is chosen with the pilot studio: Stripe (Connect, with the studio
 * owning its account) or Square (OAuth). Several providers can coexist, since nothing in the
 * books assumes there is only one.
 */
public interface PaymentProvider {

    Provider provider();

    /** Charges {@code amount} against a member's saved card, returning the processor's charge id. */
    String charge(UUID memberId, BigDecimal amount, String description, String idempotencyKey);

    void refund(String chargeId, BigDecimal amount, String idempotencyKey);

}
