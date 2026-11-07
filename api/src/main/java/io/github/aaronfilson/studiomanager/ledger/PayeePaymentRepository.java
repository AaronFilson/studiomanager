package io.github.aaronfilson.studiomanager.ledger;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Payments sent in the bound studio ({@code @TenantId}). */
interface PayeePaymentRepository extends JpaRepository<PayeePayment, UUID> {}
