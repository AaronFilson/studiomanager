package io.github.aaronfilson.studiomanager.supplies;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Stock movements in the bound studio ({@code @TenantId}). */
interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

  @Query("select coalesce(sum(m.quantity), 0.0BD) from StockMovement m where m.itemId = :itemId")
  BigDecimal onHand(UUID itemId);

  /** An item's movements, newest first. */
  List<StockMovement> findByItemIdOrderByCreatedAtDescIdDesc(UUID itemId, Limit limit);
}
