package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google Document AI's Invoice Parser, reached keylessly from Azure (infra/gcp). Blank values leave
 * the reader switched off: it never says it can read.
 *
 * @param location the processor's location, such as {@code us} or {@code eu}
 * @param processorName {@code projects/<number>/locations/<location>/processors/<id>}
 * @param workloadIdentityAudience the pool provider's full name, {@code
 *     //iam.googleapis.com/projects/<number>/locations/global/workloadIdentityPools/<pool>/providers/<provider>}
 * @param serviceAccountEmail the service account the federated identity impersonates
 * @param azureTokenScope the scope of the managed-identity token sent to Google, which must be the
 *     audience the provider accepts
 * @param usdPerTenPages the price of each block of 10 pages, which Google bills per document
 */
@ConfigurationProperties("studiomanager.invoices.google")
record GoogleReaderProperties(
    @Nullable String location,
    @Nullable String processorName,
    @Nullable String workloadIdentityAudience,
    @Nullable String serviceAccountEmail,
    @Nullable String azureTokenScope,
    BigDecimal usdPerTenPages) {

  boolean configured() {
    return present(location)
        && present(processorName)
        && present(workloadIdentityAudience)
        && present(serviceAccountEmail)
        && present(azureTokenScope);
  }

  private static boolean present(@Nullable String value) {
    return value != null && !value.isBlank();
  }
}
