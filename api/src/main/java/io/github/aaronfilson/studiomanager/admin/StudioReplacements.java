package io.github.aaronfilson.studiomanager.admin;

import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Tells the admin's settings that one studio has replaced others, so what named the old ones names
 * the new one. The demo studio's reset uses it: it deletes the studio and seeds a new one, and the
 * platform's invoice readers would otherwise stop reading for it (docs/planning/24).
 */
@Service
public class StudioReplacements {

  private final InvoiceReaderAdmin invoiceReaders;

  StudioReplacements(InvoiceReaderAdmin invoiceReaders) {
    this.invoiceReaders = invoiceReaders;
  }

  /** Must be called bound as a platform admin: the settings store refuses anyone else. */
  public void replaced(Set<UUID> old, UUID replacement) {
    invoiceReaders.studioReplaced(old, replacement);
  }
}
