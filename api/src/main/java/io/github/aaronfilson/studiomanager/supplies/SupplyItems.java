package io.github.aaronfilson.studiomanager.supplies;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.UniqueViolations;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Item;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.ItemSettings;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adding items and changing their settings. Staff only. */
@Service
public class SupplyItems {

  private final SupplyItemRepository itemRepository;
  private final SupplyService supplies;
  private final StudioPermissions permissions;

  SupplyItems(
      SupplyItemRepository itemRepository, SupplyService supplies, StudioPermissions permissions) {
    this.itemRepository = itemRepository;
    this.supplies = supplies;
    this.permissions = permissions;
  }

  @Transactional
  public Item create(ItemSettings settings) {
    permissions.require(Permission.SUPPLIES_MANAGE, SupplyService.ONLY_STAFF);
    ItemSettings checked = check(settings);
    SupplyItem item = new SupplyItem(checked);
    saveNamed(item, checked.name());
    return supplies.item(item.id());
  }

  @Transactional
  public Item update(UUID id, ItemSettings settings, long expectedVersion) {
    permissions.require(Permission.SUPPLIES_MANAGE, SupplyService.ONLY_STAFF);
    ItemSettings checked = check(settings);
    SupplyItem item =
        itemRepository.findById(id).orElseThrow(() -> new NotFoundException("No such item."));
    // @Version then catches anyone who changes it between this load and the commit
    if (item.version() != expectedVersion) {
      throw ConflictException.staleVersion(
          "Someone else changed this item at the same time. Reload and try again.");
    }
    item.change(checked);
    saveNamed(item, checked.name());
    return supplies.item(id);
  }

  /**
   * Writes the item now, inside the try, so a taken name is reported here rather than at commit,
   * and so the view returned carries the new version.
   */
  private void saveNamed(SupplyItem item, String name) {
    try {
      itemRepository.saveAndFlush(item);
    } catch (DataIntegrityViolationException e) {
      if (UniqueViolations.isUniqueViolation(e)) {
        throw new InvalidInputException("There's already an item called '%s'.".formatted(name));
      }
      throw e;
    }
  }

  /** Validates settings and returns them trimmed. */
  private static ItemSettings check(ItemSettings settings) {
    String name = settings.name().strip();
    if (name.isEmpty() || name.length() > 80) {
      throw new InvalidInputException("Name the item in 1 to 80 characters.");
    }
    String unit = settings.unit().strip();
    if (unit.isEmpty() || unit.length() > 30) {
      throw new InvalidInputException(
          "Say what one unit is, such as \"25 lb bag\" (up to 30 characters).");
    }
    Integer leadTimeDays = settings.leadTimeDays();
    if (leadTimeDays != null && (leadTimeDays < 0 || leadTimeDays > 365)) {
      throw new InvalidInputException("Lead time is 0 to 365 days.");
    }
    if (settings.salePrice() != null && settings.salePrice().signum() < 0) {
      throw new InvalidInputException("A price can't be negative.");
    }
    if (settings.reorderQuantity() != null && settings.reorderQuantity().signum() <= 0) {
      throw new InvalidInputException("A reorder quantity must be more than zero.");
    }
    return new ItemSettings(
        name,
        settings.category(),
        unit,
        SupplyService.clean(settings.supplier()),
        leadTimeDays,
        settings.parLevel(),
        settings.reorderQuantity(),
        settings.salePrice(),
        SupplyService.clean(settings.taxCategory()),
        SupplyService.clean(settings.substitute()));
  }
}
