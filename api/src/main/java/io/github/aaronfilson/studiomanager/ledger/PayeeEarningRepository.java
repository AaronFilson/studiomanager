package io.github.aaronfilson.studiomanager.ledger;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Shares and fees earned in the bound studio ({@code @TenantId}). */
interface PayeeEarningRepository extends JpaRepository<PayeeEarning, UUID> {}
