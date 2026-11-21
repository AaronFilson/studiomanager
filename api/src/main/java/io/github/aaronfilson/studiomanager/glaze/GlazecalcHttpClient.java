package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.http.PublicAddresses;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSRF-safe HTTP client for calling Glazecalc ({@code /api/shared/recipe}, {@code /oauth/token},
 * and {@code /api/export}). Checks every resolved address through {@link PublicAddresses} at DNS
 * resolution time unless {@code allowPrivateNetworks} is enabled (tests only).
 */
final class GlazecalcHttpClient implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(GlazecalcHttpClient.class);
  private static final int MAX_RESPONSE_BYTES = GlazeImportParser.MAX_IMPORT_BYTES + 1;

  record Response(int statusCode, String body) {}

  static final class UnsafeTargetException extends RuntimeException {
    UnsafeTargetException(String message) {
      super(message);
    }
  }

  private static final class BlockedAddressException extends UnknownHostException {
    private final String reason;

    BlockedAddressException(String reason) {
      super(reason);
      this.reason = reason;
    }

    String reason() {
      return reason;
    }
  }

  private final boolean allowPrivateNetworks;
  private final DnsResolver resolver;
  private final CloseableHttpClient client;

  GlazecalcHttpClient(boolean allowPrivateNetworks) {
    this.allowPrivateNetworks = allowPrivateNetworks;
    this.resolver = new CheckingResolver(SystemDefaultDnsResolver.INSTANCE, allowPrivateNetworks);
    this.client =
        HttpClients.custom()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setDnsResolver(resolver)
                    .setDefaultConnectionConfig(
                        ConnectionConfig.custom()
                            .setConnectTimeout(Timeout.ofSeconds(5))
                            .setSocketTimeout(Timeout.ofSeconds(10))
                            .build())
                    .setMaxConnTotal(20)
                    .setMaxConnPerRoute(4)
                    .build())
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setRedirectsEnabled(false)
                    .setResponseTimeout(Timeout.ofSeconds(10))
                    .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                    .build())
            .disableRedirectHandling()
            .disableCookieManagement()
            .disableAutomaticRetries()
            .disableAuthCaching()
            .setUserAgent("StudioManager-Glazecalc/1")
            .build();
  }

  Response get(URI uri, Map<String, String> headers) throws IOException {
    validateUri(uri);
    HttpGet get = new HttpGet(uri);
    headers.forEach(get::setHeader);
    try {
      return client.execute(
          get, response -> new Response(response.getCode(), read(response.getEntity())));
    } catch (BlockedAddressException e) {
      throw new UnsafeTargetException(e.reason());
    }
  }

  Response postForm(URI uri, Map<String, String> formFields) throws IOException {
    validateUri(uri);
    String encoded =
        formFields.entrySet().stream()
            .map(e ->
                URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    HttpPost post = new HttpPost(uri);
    post.setHeader("Accept", "application/json");
    post.setEntity(new StringEntity(encoded, ContentType.APPLICATION_FORM_URLENCODED));
    try {
      return client.execute(
          post, response -> new Response(response.getCode(), read(response.getEntity())));
    } catch (BlockedAddressException e) {
      throw new UnsafeTargetException(e.reason());
    }
  }

  private void validateUri(URI uri) throws IOException {
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    if (uri.getHost() == null || uri.getHost().isBlank()) {
      throw new UnsafeTargetException("Glazecalc URL must have a host.");
    }
    if (!scheme.equals("https") && !(allowPrivateNetworks && scheme.equals("http"))) {
      throw new UnsafeTargetException("Glazecalc URL must use https://.");
    }
    try {
      resolver.resolve(uri.getHost());
    } catch (BlockedAddressException e) {
      throw new UnsafeTargetException(e.reason());
    }
  }

  private static String read(@Nullable HttpEntity entity) throws IOException {
    if (entity == null) {
      return "";
    }
    try (InputStream in = entity.getContent()) {
      if (in == null) {
        return "";
      }
      byte[] bytes = in.readNBytes(MAX_RESPONSE_BYTES);
      if (bytes.length > GlazeImportParser.MAX_IMPORT_BYTES) {
        throw new IOException("Glazecalc response exceeded the 2 MB limit.");
      }
      return new String(bytes, StandardCharsets.UTF_8);
    }
  }

  @Override
  public void close() throws IOException {
    client.close();
  }

  private record CheckingResolver(DnsResolver dns, boolean allowPrivateNetworks)
      implements DnsResolver {

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
      InetAddress[] addresses = dns.resolve(host);
      if (!allowPrivateNetworks) {
        for (InetAddress address : addresses) {
          var why = PublicAddresses.whyNot(address);
          if (why.isPresent()) {
            log.warn(
                "Blocked Glazecalc request to {} (resolved to {}, which {})",
                host,
                address.getHostAddress(),
                why.get());
            throw new BlockedAddressException("Glazecalc requests only go to public addresses.");
          }
        }
      }
      return addresses;
    }

    @Override
    public String resolveCanonicalHostname(String host) throws UnknownHostException {
      return dns.resolveCanonicalHostname(host);
    }
  }
}
