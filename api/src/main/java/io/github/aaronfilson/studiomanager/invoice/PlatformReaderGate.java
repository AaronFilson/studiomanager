package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.ai.AiKeys;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Whether a platform-paid reader may read for the bound studio now. Checked cheapest first: the
 * admin's rule, the owner's consent, then the studio's and the platform's pages this month.
 */
@Component
class PlatformReaderGate {

  private final PlatformReaderRules rules;
  private final AiKeys keys;
  private final PlatformReaderUsage usage;

  PlatformReaderGate(PlatformReaderRules rules, AiKeys keys, PlatformReaderUsage usage) {
    this.rules = rules;
    this.keys = keys;
    this.usage = usage;
  }

  boolean allows(InvoiceReaderKind reader) {
    UUID studioId = TenantContext.require().requireStudioId();
    PlatformReaderRules.ReaderRule rule = rules.current().rule(reader);
    if (!rule.covers(studioId) || !keys.platformReadersAllowed()) {
      return false;
    }
    return usage.studioPagesThisMonth(reader) < rule.studioMonthlyPages()
        && usage.platformPagesThisMonth(reader) < rule.platformMonthlyPages();
  }
}
