package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Azure AI Document Intelligence, reached with the API's managed identity (infra/modules/
 * document_intelligence). A blank endpoint leaves the reader switched off.
 *
 * @param endpoint the account's endpoint, such as {@code
 *     https://<name>.cognitiveservices.azure.com}
 * @param usdPerThousandPages the price per 1,000 pages; 0 on the free F0 tier
 */
@ConfigurationProperties("studiomanager.invoices.azure")
record AzureReaderProperties(@Nullable String endpoint, BigDecimal usdPerThousandPages) {

  boolean configured() {
    return endpoint != null && endpoint.startsWith("https://");
  }
}
