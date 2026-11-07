package io.github.aaronfilson.studiomanager.invoice;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ExternalAccountCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdentityPoolCredentials;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads an invoice with Google Document AI's Invoice Parser, paid by the platform
 * (docs/planning/20, "Invoice readers"; ADR 18).
 *
 * <p>Keyless from Azure: the API's managed-identity token is traded for a Google token through
 * workload identity federation, which then impersonates a service account holding {@code
 * roles/documentai.apiUser} (infra/gcp). There is no service-account key anywhere.
 *
 * <p>The REST endpoint, through Spring's {@code RestClient} and google-auth-library alone: the gRPC
 * client library would bring gRPC, protobuf and more Guava, for one call.
 */
@Component
@EnableConfigurationProperties(GoogleReaderProperties.class)
class GoogleInvoiceReader implements InvoiceReader {

  static final String PROVIDER = "Google Document AI";
  static final String MODEL = "documentai-invoice-parser";

  private static final String TOKEN_URL = "https://sts.googleapis.com/v1/token";
  private static final String CLOUD_PLATFORM = "https://www.googleapis.com/auth/cloud-platform";

  /** Only what the mapping reads, so the raw answer kept is small: no full text, no layout. */
  private static final String FIELD_MASK = "entities,pages.pageNumber";

  private final GoogleReaderProperties properties;
  private final PlatformReaderGate gate;
  private final JsonMapper json;
  private final RestClient http;
  private volatile @Nullable GoogleCredentials credentials;

  GoogleInvoiceReader(GoogleReaderProperties properties, PlatformReaderGate gate, JsonMapper json) {
    this.properties = properties;
    this.gate = gate;
    this.json = json;
    this.http = DocumentServiceCalls.client();
  }

  @Override
  public InvoiceReaderKind kind() {
    return InvoiceReaderKind.GOOGLE_DOCUMENT_AI;
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

  /**
   * One request with a Google access token already in hand: the reader's own, or the live eval's.
   */
  ReaderResult readWith(String accessToken, InvoiceFile file) {
    Map<String, Object> request =
        Map.of(
            "rawDocument",
            Map.of(
                "content",
                Base64.getEncoder().encodeToString(file.content()),
                "mimeType",
                file.type().mediaType()),
            "fieldMask",
            FIELD_MASK);
    String body;
    try {
      body =
          http.post()
              .uri(processUrl())
              .headers(headers -> headers.setBearerAuth(accessToken))
              .contentType(MediaType.APPLICATION_JSON)
              .body(json.writeValueAsString(request))
              .retrieve()
              .body(String.class);
    } catch (RestClientException e) {
      throw DocumentServiceCalls.failure(PROVIDER, e);
    }
    return toResult(body);
  }

  /** The answer as a result: the draft, the trimmed raw answer, the pages and their cost. */
  ReaderResult toResult(@Nullable String body) {
    if (body == null) {
      throw DocumentServiceCalls.unreadable(PROVIDER);
    }
    DocumentAiAnswer answer;
    try {
      answer = json.readValue(body, DocumentAiAnswer.class);
    } catch (JacksonException e) {
      throw DocumentServiceCalls.unreadable(PROVIDER);
    }
    if (answer.document() == null) {
      throw DocumentServiceCalls.unreadable(PROVIDER);
    }
    int pages = DocumentServiceCalls.billedPages(answer.pages());
    return new ReaderResult(
        answer.toDraft(),
        json.writeValueAsString(answer),
        new ReaderResult.Usage(
            MODEL,
            pages,
            null,
            null,
            DocumentServiceCalls.googleCost(pages, properties.usdPerTenPages())));
  }

  private String processUrl() {
    return "https://%s-documentai.googleapis.com/v1/%s:process"
        .formatted(properties.location(), properties.processorName());
  }

  private String accessToken() {
    GoogleCredentials held = credentials();
    try {
      held.refreshIfExpired();
    } catch (IOException e) {
      throw new InvoiceReadException(
          InvoiceReadException.Failure.UNAVAILABLE,
          "Couldn't sign in to Google Document AI. The admin needs to check its setup.");
    }
    AccessToken token = held.getAccessToken();
    if (token == null) {
      throw DocumentServiceCalls.unreachable(PROVIDER);
    }
    return token.getTokenValue();
  }

  private GoogleCredentials credentials() {
    GoogleCredentials held = credentials;
    if (held == null) {
      synchronized (this) {
        held = credentials;
        if (held == null) {
          held = federated(properties);
          credentials = held;
        }
      }
    }
    return held;
  }

  /** Workload identity federation with the managed identity's token as the subject token. */
  static GoogleCredentials federated(GoogleReaderProperties properties) {
    return IdentityPoolCredentials.newBuilder()
        .setSubjectTokenSupplier(
            new ManagedIdentitySubjectToken(
                new DefaultAzureCredentialBuilder().build(),
                Objects.requireNonNull(properties.azureTokenScope())))
        .setAudience(Objects.requireNonNull(properties.workloadIdentityAudience()))
        .setSubjectTokenType(ExternalAccountCredentials.SubjectTokenTypes.JWT)
        .setTokenUrl(TOKEN_URL)
        .setServiceAccountImpersonationUrl(
            "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/%s:generateAccessToken"
                .formatted(properties.serviceAccountEmail()))
        .setScopes(List.of(CLOUD_PLATFORM))
        .build();
  }
}
