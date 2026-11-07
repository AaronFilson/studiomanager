package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.invoice.InvoiceReaderKind;
import io.github.aaronfilson.studiomanager.invoice.PlatformReaderRules;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The invoice reader rules before the admin saves any: each platform-paid reader on for the demo
 * studio only, so the platform pays nothing unless the admin turns a reader on for someone else
 * (docs/planning/20, "Invoice readers"). With no demo studio configured, that's nobody.
 *
 * @param demoStudioId the demo studio, if there is one
 * @param studioMonthlyPages the default cap for one studio in a month
 * @param platformMonthlyPages the default cap for every studio together in a month; Azure's free F0
 *     tier stops at 500
 */
@ConfigurationProperties("studiomanager.invoices.platform-readers")
record InvoiceReaderDefaults(
    @Nullable UUID demoStudioId, int studioMonthlyPages, int platformMonthlyPages) {

  PlatformReaderRules.Rules rules() {
    Set<UUID> demo = demoStudioId == null ? Set.of() : Set.of(demoStudioId);
    PlatformReaderRules.ReaderRule rule =
        new PlatformReaderRules.ReaderRule(
            !demo.isEmpty(),
            demo.isEmpty() ? PlatformReaderRules.Scope.NONE : PlatformReaderRules.Scope.CHOSEN,
            demo,
            studioMonthlyPages,
            platformMonthlyPages);
    Map<InvoiceReaderKind, PlatformReaderRules.ReaderRule> readers =
        Arrays.stream(InvoiceReaderKind.values())
            .filter(InvoiceReaderKind::paidByPlatform)
            .collect(Collectors.toMap(Function.identity(), reader -> rule));
    return new PlatformReaderRules.Rules(readers, Map.of());
  }
}
