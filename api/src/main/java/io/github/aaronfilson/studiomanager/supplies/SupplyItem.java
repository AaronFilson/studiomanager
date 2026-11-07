package io.github.aaronfilson.studiomanager.supplies;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Category;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.ItemSettings;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * Something the studio stocks. Its stock isn't stored here: on hand is the sum of its {@link
 * StockMovement}s, so there's no number someone can forget to update.
 */
@Entity
@Table(name = "supply_item")
class SupplyItem {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private String name;

  @Enumerated(EnumType.STRING)
  private Category category;

  private String unit;

  private @Nullable String supplier;

  private @Nullable Integer leadTimeDays;

  private @Nullable BigDecimal parLevel;

  private @Nullable BigDecimal reorderQuantity;

  private @Nullable BigDecimal salePrice;

  private @Nullable String taxCategory;

  private @Nullable String substitute;

  private boolean archived;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected SupplyItem() {}

  /** A new item from settings already checked and trimmed. */
  SupplyItem(ItemSettings settings) {
    this.id = Uuids.v7();
    apply(settings);
  }

  UUID id() {
    return id;
  }

  String name() {
    return name;
  }

  @Nullable BigDecimal reorderQuantity() {
    return reorderQuantity;
  }

  @Nullable BigDecimal salePrice() {
    return salePrice;
  }

  @Nullable String taxCategory() {
    return taxCategory;
  }

  /** What members should use while it's out; null when there's nothing to suggest. */
  @Nullable String substitute() {
    return substitute;
  }

  long version() {
    return Objects.requireNonNull(version, "An item has a version once it's saved");
  }

  /** Applies settings already checked and trimmed. */
  void change(ItemSettings settings) {
    apply(settings);
  }

  private void apply(ItemSettings settings) {
    this.name = settings.name();
    this.category = settings.category();
    this.unit = settings.unit();
    this.supplier = settings.supplier();
    this.leadTimeDays = settings.leadTimeDays();
    this.parLevel = settings.parLevel();
    this.reorderQuantity = settings.reorderQuantity();
    this.salePrice = settings.salePrice();
    this.taxCategory = settings.taxCategory();
    this.substitute = settings.substitute();
  }

  SupplyModel.Item toView(BigDecimal onHand, BigDecimal onOrder) {
    return new SupplyModel.Item(
        id,
        name,
        category,
        unit,
        supplier,
        leadTimeDays,
        parLevel,
        reorderQuantity,
        salePrice,
        taxCategory,
        substitute,
        onHand,
        onOrder,
        version());
  }
}
