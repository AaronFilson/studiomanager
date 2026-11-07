package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * What the two document-service readers share: an HTTP client with timeouts, and turning a failed
 * call into an {@link InvoiceReadException} whose message names only the provider and the status,
 * never the provider's body, which can quote the invoice.
 */
final class DocumentServiceCalls {

  static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  static final Duration READ_TIMEOUT = Duration.ofMinutes(2);

  private DocumentServiceCalls() {}

  static RestClient client() {
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    factory.setReadTimeout(READ_TIMEOUT);
    return RestClient.builder().requestFactory(factory).build();
  }

  static InvoiceReadException failure(String provider, RestClientException e) {
    if (e instanceof HttpStatusCodeException status) {
      int code = status.getStatusCode().value();
      if (code == 401 || code == 403) {
        return new InvoiceReadException(
            InvoiceReadException.Failure.REFUSED_CREDENTIALS,
            "%s refused the platform's sign-in (%d). The admin needs to check its setup."
                .formatted(provider, code));
      }
      if (status.getStatusCode().is4xxClientError() && code != 408 && code != 429) {
        return new InvoiceReadException(
            InvoiceReadException.Failure.UNREADABLE,
            "%s couldn't take this file (%d). Fill this one in by hand.".formatted(provider, code));
      }
      return new InvoiceReadException(
          InvoiceReadException.Failure.UNAVAILABLE,
          "%s answered %d. Fill this one in by hand, or upload it again later."
              .formatted(provider, code));
    }
    return unreachable(provider);
  }

  static InvoiceReadException unreachable(String provider) {
    return new InvoiceReadException(
        InvoiceReadException.Failure.UNAVAILABLE,
        "Couldn't reach %s. Fill this one in by hand, or upload it again later."
            .formatted(provider));
  }

  static InvoiceReadException unreadable(String provider) {
    return new InvoiceReadException(
        InvoiceReadException.Failure.UNREADABLE,
        "%s's answer wasn't an invoice it could read. Fill it in by hand.".formatted(provider));
  }

  /** At least one page: a service that reads a file has read a page of it. */
  static int billedPages(int pages) {
    return Math.max(1, pages);
  }

  /** Google bills each document in whole blocks of 10 pages. */
  static BigDecimal googleCost(int pages, BigDecimal usdPerTenPages) {
    int blocks = Math.addExact(pages, 9) / 10;
    return usdPerTenPages.multiply(BigDecimal.valueOf(blocks)).setScale(6, RoundingMode.HALF_UP);
  }

  static BigDecimal azureCost(int pages, BigDecimal usdPerThousandPages) {
    return usdPerThousandPages
        .multiply(BigDecimal.valueOf(pages))
        .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
  }
}
