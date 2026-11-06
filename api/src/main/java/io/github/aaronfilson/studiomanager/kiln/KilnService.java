package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.UniqueViolations;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The bound studio's kilns and their availability. Their upkeep is in the maintenance module. */
@Service
public class KilnService {

  private final KilnRepository kilns;
  private final ApplicationEventPublisher events;

  KilnService(KilnRepository kilns, ApplicationEventPublisher events) {
    this.kilns = kilns;
    this.events = events;
  }

  @Transactional(readOnly = true)
  public List<KilnView> list() {
    return kilns.findAllInOrder().stream().map(Kiln::toView).toList();
  }

  @Transactional(readOnly = true)
  public KilnView get(UUID kilnId) {
    return find(kilnId).toView();
  }

  @Transactional
  public KilnView create(KilnSettings settings) {
    Kiln kiln = new Kiln(settings);
    saveNamed(kiln, settings.name());
    // Maintenance's listener inserts the kiln's asset with SQL in this transaction, so the kiln
    // row has to be written first (flushed by saveNamed)
    events.publishEvent(new KilnCreated(kiln.id(), TenantContext.require().requireStudioId()));
    return kiln.toView();
  }

  @Transactional
  public KilnView update(UUID kilnId, KilnSettings settings, long expectedVersion) {
    Kiln kiln = findAtVersion(kilnId, expectedVersion);
    kiln.change(settings);
    saveNamed(kiln, settings.name());
    return kiln.toView();
  }

  /** Takes a kiln out of service (with a reason) or, with a null reason, puts it back. */
  @Transactional
  public KilnView setDown(UUID kilnId, @Nullable String reason, long expectedVersion) {
    String cleaned = reason == null || reason.isBlank() ? null : reason.strip();
    if (cleaned != null && cleaned.length() > 200) {
      throw new InvalidInputException("Keep the reason under 200 characters.");
    }
    Kiln kiln = findAtVersion(kilnId, expectedVersion);
    kiln.setDownReason(cleaned);
    kilns.flush();
    return kiln.toView();
  }

  /** What staff can set on a kiln. */
  public record KilnSettings(
      String name,
      @Nullable String site,
      KilnView.KilnType kind,
      @Nullable Integer maxPiecesPerMember) {

    public KilnSettings {
      name = name.strip();
      if (name.isEmpty() || name.length() > 60) {
        throw new InvalidInputException("A kiln needs a name of 1 to 60 characters.");
      }
      site = site == null || site.isBlank() ? null : site.strip();
      if (site != null && site.length() > 60) {
        throw new InvalidInputException("Keep the site name under 60 characters.");
      }
    }
  }

  /** For the firing services in this package, which change a kiln as part of a firing. */
  Kiln find(UUID kilnId) {
    return kilns.findById(kilnId).orElseThrow(() -> new NotFoundException("No such kiln."));
  }

  /**
   * Loads a kiln and checks it's still the version the client saw. {@code @Version} then catches
   * anyone who changes it between this load and the commit.
   */
  private Kiln findAtVersion(UUID kilnId, long expectedVersion) {
    Kiln kiln = find(kilnId);
    if (kiln.version() != expectedVersion) {
      throw ConflictException.staleVersion(
          "Someone else changed this kiln at the same time. Reload and try again.");
    }
    return kiln;
  }

  /**
   * Writes the kiln now, inside the try, so a taken name is reported here rather than at commit,
   * and so the view returned carries the new version.
   */
  private void saveNamed(Kiln kiln, String name) {
    try {
      kilns.saveAndFlush(kiln);
    } catch (DataIntegrityViolationException e) {
      if (UniqueViolations.isUniqueViolation(e)) {
        throw new InvalidInputException("There's already a kiln called '%s'.".formatted(name));
      }
      throw e;
    }
  }
}
