package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.kiln.KilnCreated;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Asset;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.AssetKind;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.UniqueViolations;
import java.time.Clock;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adding, describing, taking down and archiving assets. Kilns get theirs from the kiln module. */
@Service
class AssetChanges {

  private final MaintainedAssetRepository assets;
  private final MaintenanceService reads;
  private final Clock clock;

  AssetChanges(MaintainedAssetRepository assets, MaintenanceService reads, Clock clock) {
    this.assets = assets;
    this.reads = reads;
    this.clock = clock;
  }

  @Transactional
  public Asset createAsset(
      AssetKind kind, String name, @Nullable String location, @Nullable String notes) {
    if (kind == AssetKind.KILN) {
      throw new InvalidInputException(
          "Add kilns on the kilns page; they appear here by themselves.");
    }
    MaintainedAsset asset =
        MaintainedAsset.equipment(
            kind,
            requireName(name),
            MaintenanceText.optional(location, 80, "location"),
            MaintenanceText.optional(notes, 1000, "notes"));
    saveNamed(asset);
    return reads.asset(asset.id(), clock.instant());
  }

  /**
   * Changes the location and notes, and the name of anything but a kiln (which takes the kiln's).
   */
  @Transactional
  public Asset updateAsset(
      UUID assetId,
      @Nullable String name,
      @Nullable String location,
      @Nullable String notes,
      long expectedVersion) {
    MaintainedAsset asset = findAtVersion(assetId, expectedVersion);
    String newName = asset.isKiln() || name == null ? null : requireName(name);
    asset.describe(
        newName,
        MaintenanceText.optional(location, 80, "location"),
        MaintenanceText.optional(notes, 1000, "notes"));
    saveNamed(asset);
    return reads.asset(assetId, clock.instant());
  }

  /**
   * Takes an asset out of service with a reason or, with no reason, puts it back. Not for kilns.
   */
  @Transactional
  public Asset setDown(UUID assetId, @Nullable String reason, long expectedVersion) {
    if (reads.findAsset(assetId).isKiln()) {
      throw new ConflictException(
          "Mark a kiln down on the kiln itself, where it also stops loads.");
    }
    MaintainedAsset asset = findAtVersion(assetId, expectedVersion);
    asset.setDownReason(MaintenanceText.optional(reason, 200, "reason"));
    assets.flush();
    return reads.asset(assetId, clock.instant());
  }

  /** Stops tracking an asset and its tasks. Its records are kept. Not for kilns. */
  @Transactional
  public void archiveAsset(UUID assetId) {
    MaintainedAsset asset = reads.findAsset(assetId);
    if (asset.isKiln()) {
      throw new ConflictException("A kiln is tracked for as long as it exists.");
    }
    asset.archive();
    assets.flush();
  }

  /**
   * Every kiln is maintained, so a new one gets its asset in the same transaction. Flushed at once,
   * since other listeners of the same event may run SQL after this one.
   */
  @EventListener
  void onKilnCreated(KilnCreated kiln) {
    assets.saveAndFlush(MaintainedAsset.ofKiln(kiln.kilnId()));
  }

  /**
   * Loads an asset and checks it's still the version the client saw. {@code @Version} then catches
   * anyone who changes it between this load and the commit.
   */
  private MaintainedAsset findAtVersion(UUID assetId, long expectedVersion) {
    MaintainedAsset asset = reads.findAsset(assetId);
    if (asset.version() != expectedVersion) {
      throw MaintenanceText.staleVersion();
    }
    return asset;
  }

  /**
   * Writes the asset now, inside the try, so a taken name is reported here rather than at commit,
   * and so the view returned carries the new version.
   */
  private void saveNamed(MaintainedAsset asset) {
    try {
      assets.saveAndFlush(asset);
    } catch (DataIntegrityViolationException e) {
      // Only an asset with a name of its own can clash; a kiln's asset has none
      if (UniqueViolations.isUniqueViolation(e)) {
        throw new InvalidInputException(
            "There's already something called '%s'.".formatted(asset.name()));
      }
      throw e;
    }
  }

  private static String requireName(String name) {
    String cleaned = name.strip();
    if (cleaned.isEmpty() || cleaned.length() > 80) {
      throw new InvalidInputException("A name of 1 to 80 characters is needed.");
    }
    return cleaned;
  }
}
