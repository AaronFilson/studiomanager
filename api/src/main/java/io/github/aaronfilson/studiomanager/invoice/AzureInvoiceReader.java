package io.github.aaronfilson.studiomanager.invoice;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads an invoice with Azure AI Document Intelligence's prebuilt invoice model, paid by the
 * platform (docs/planning/20, "Invoice readers"; ADR 18).
 *
 * <p>Signed in as the API's managed identity, which holds {@code Cognitive Services User} on the
 * account, the same way the database login works: no key. The analyze call is asynchronous: it
 * answers 202 with an {@code Operation-Location} to poll until the result is ready.
 */
@Component
@EnableConfigurationProperties(AzureReaderProperties.class)
class AzureInvoiceReader implements InvoiceReader {

  static final String PROVIDER = "Azure AI Document Intelligence";
  static final String MODEL = "prebuilt-invoice";
  static final String API_VERSION = "2024-11-30";

  private static final String SCOPE = "https://cognitiveservices.azure.com/.default";
  private static final Duration POLL_EVERY = Duration.ofSeconds(1);
  private static final int MOST_POLLS = 90;

  private final AzureReaderProperties properties;
  private final PlatformReaderGate gate;
  private final JsonMapper json;
  private final RestClient http;
  private volatile @Nullable TokenCredential credential;

  AzureInvoiceReader(AzureReaderProperties properties, PlatformReaderGate gate, JsonMapper json) {
    this.properties = properties;
    this.gate = gate;
    this.json = json;
    this.http = DocumentServiceCalls.client();
  }

  @Override
  public InvoiceReaderKind kind() {
    return InvoiceReaderKind.AZURE_DOCUMENT_INTELLIGENCE;
  }

  @Override
  public boolean canReadForBoundStudio() {
    return properties.configured() && gate.allows(kind());
  }

  @Override
  public ReaderResult read(InvoiceFile file) {
    if (!properties.configured()) {
      throw DocumentServiceCalls.unreachable(PROVIDER);
    }
    return readWith(accessToken(), file);
  }

  /** One analysis with an access token already in hand: the reader's own, or the live eval's. */
  ReaderResult readWith(String accessToken, InvoiceFile file) {
    String endpoint = endpoint();
    URI analyze =
        URI.create(
            "%s/documentintelligence/documentModels/%s:analyze?api-version=%s"
                .formatted(endpoint, MODEL, API_VERSION));
    URI operation;
    try {
      ResponseEntity<Void> started =
          http.post()
              .uri(analyze)
              .headers(headers -> headers.setBearerAuth(accessToken))
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  json.writeValueAsString(
                      Map.of("base64Source", Base64.getEncoder().encodeToString(file.content()))))
              .retrieve()
              .toBodilessEntity();
      operation = operationUri(endpoint, started.getHeaders().getFirst("Operation-Location"));
    } catch (RestClientException e) {
      throw DocumentServiceCalls.failure(PROVIDER, e);
    }
    for (int poll = 0; poll < MOST_POLLS; poll++) {
      pause();
      String body;
      try {
        body =
            http.get()
                .uri(operation)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(String.class);
      } catch (RestClientException e) {
        throw DocumentServiceCalls.failure(PROVIDER, e);
      }
      DocumentIntelligenceAnswer answer = parse(body);
      if (answer.succeeded()) {
        return toResult(answer);
      }
      if (answer.failed()) {
        throw DocumentServiceCalls.unreadable(PROVIDER);
      }
    }
    throw new InvoiceReadException(
        InvoiceReadException.Failure.UNAVAILABLE,
        "Azure AI Document Intelligence took too long. Fill this one in by hand.");
  }

  /** A finished analysis as a result: the draft, the trimmed raw answer, the pages and cost. */
  ReaderResult toResult(DocumentIntelligenceAnswer answer) {
    int pages = DocumentServiceCalls.billedPages(answer.pages());
    return new ReaderResult(
        answer.toDraft(),
        json.writeValueAsString(answer),
        new ReaderResult.Usage(
            MODEL,
            pages,
            null,
            null,
            DocumentServiceCalls.azureCost(pages, properties.usdPerThousandPages())));
  }

  DocumentIntelligenceAnswer parse(@Nullable String body) {
    if (body == null) {
      throw DocumentServiceCalls.unreadable(PROVIDER);
    }
    try {
      return json.readValue(body, DocumentIntelligenceAnswer.class);
    } catch (JacksonException e) {
      throw DocumentServiceCalls.unreadable(PROVIDER);
    }
  }

  /**
   * Where to poll. It must be on the account's own endpoint: the token goes with each poll, so a
   * header pointing anywhere else is refused rather than followed.
   */
  static URI operationUri(String endpoint, @Nullable String location) {
    if (location == null || !location.startsWith(endpoint + "/")) {
      throw DocumentServiceCalls.unreadable(PROVIDER);
    }
    try {
      return URI.create(location);
    } catch (IllegalArgumentException e) {
      throw DocumentServiceCalls.unreadable(PROVIDER);
    }
  }

  private String endpoint() {
    String endpoint = Objects.requireNonNull(properties.endpoint());
    return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
  }

  private String accessToken() {
    try {
      AccessToken token = credential().getTokenSync(new TokenRequestContext().addScopes(SCOPE));
      return token.getToken();
    } catch (RuntimeException e) {
      throw new InvoiceReadException(
          InvoiceReadException.Failure.UNAVAILABLE,
          "Couldn't sign in to Azure AI Document Intelligence. The admin needs to check its setup.");
    }
  }

  private TokenCredential credential() {
    TokenCredential held = credential;
    if (held == null) {
      synchronized (this) {
        held = credential;
        if (held == null) {
          held = new DefaultAzureCredentialBuilder().build();
          credential = held;
        }
      }
    }
    return held;
  }

  private static void pause() {
    try {
      Thread.sleep(POLL_EVERY);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InvoiceReadException(
          InvoiceReadException.Failure.UNAVAILABLE,
          "Reading was interrupted. Fill this one in by hand.");
    }
  }
}
