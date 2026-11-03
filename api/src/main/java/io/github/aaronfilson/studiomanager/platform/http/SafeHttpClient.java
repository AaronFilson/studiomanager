package io.github.aaronfilson.studiomanager.platform.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
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

/**
 * POSTs to URLs that people outside the company typed in, such as a studio's webhook endpoint,
 * without letting them reach anything private (server-side request forgery).
 *
 * <ul>
 * <li>Only {@code https}, on port 443 or 8443.
 * <li>Every address the hostname resolves to must be public ({@link PublicAddresses}). The check
 * runs inside the connection's own DNS lookup, so the connection goes to an address that was
 * checked: a hostname that answers with a public address for the check and a private one for the
 * connection (DNS rebinding) gets nowhere.
 * <li>No redirects, cookies, proxy or retries; 5 seconds to connect, 10 to answer, and at most
 * {@link #MAX_RESPONSE_BYTES} of the response read.
 * </ul>
 *
 * <p>{@code allowPrivateNetworks} turns the address check off and allows {@code http}. It is for
 * local work and tests only, where the receiver runs on {@code localhost}.
 */
public final class SafeHttpClient implements AutoCloseable {

    public static final int MAX_RESPONSE_BYTES = 64 * 1024;
    private static final Set<Integer> HTTPS_PORTS = Set.of(-1, 443, 8443);

    /** A URL the filter refuses, with a reason a studio's staff can act on. */
    public static final class UnsafeUrlException extends RuntimeException {
        public UnsafeUrlException(String message) {
            super(message);
        }
    }

    /** Thrown from the DNS lookup, so the refusal stops the connection itself. */
    private static final class BlockedAddressException extends UnknownHostException {
        BlockedAddressException(String message) {
            super(message);
        }
    }

    /**
     * What the other end said.
     *
     * @param statusCode the HTTP status
     * @param body the start of the response, decoded as UTF-8
     */
    public record Response(int statusCode, String body) {
    }

    private final boolean allowPrivateNetworks;
    private final DnsResolver resolver;
    private final CloseableHttpClient client;

    public SafeHttpClient(String userAgent, boolean allowPrivateNetworks) {
        this(userAgent, allowPrivateNetworks, SystemDefaultDnsResolver.INSTANCE);
    }

    SafeHttpClient(String userAgent, boolean allowPrivateNetworks, DnsResolver dns) {
        this.allowPrivateNetworks = allowPrivateNetworks;
        this.resolver = new CheckingResolver(dns, allowPrivateNetworks);
        this.client = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setDnsResolver(resolver)
                        .setDefaultConnectionConfig(ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofSeconds(5))
                                .setSocketTimeout(Timeout.ofSeconds(10))
                                .build())
                        .setMaxConnTotal(50)
                        .setMaxConnPerRoute(4)
                        .build())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setRedirectsEnabled(false)
                        .setResponseTimeout(Timeout.ofSeconds(10))
                        .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                        .build())
                .disableRedirectHandling()
                .disableCookieManagement()
                .disableAutomaticRetries()
                .disableAuthCaching()
                .setUserAgent(userAgent)
                .build();
    }

    /**
     * Checks a URL when someone saves it: its form, then where it resolves to now. It is checked
     * again on every send, because DNS can change.
     *
     * @throws UnsafeUrlException with the reason, if it would be refused
     */
    public URI check(String url) {
        URI uri = parse(url);
        try {
            resolver.resolve(uri.getHost());
        } catch (BlockedAddressException e) {
            throw new UnsafeUrlException(e.getMessage());
        } catch (UnknownHostException e) {
            throw new UnsafeUrlException("%s doesn't resolve to any address.".formatted(uri.getHost()));
        }
        return uri;
    }

    /**
     * POSTs a JSON body.
     *
     * @throws UnsafeUrlException if the URL or any address it resolves to is refused
     * @throws IOException if it couldn't connect, or got no answer in time
     */
    public Response postJson(String url, Map<String, String> headers, String json) throws IOException {
        URI uri = parse(url);
        try {
            // Also covers an IP-literal host, in case the client ever connects to one without a lookup.
            // The connection's own lookup checks again, so what was checked is what's connected to.
            resolver.resolve(uri.getHost());
        } catch (BlockedAddressException e) {
            throw new UnsafeUrlException(e.getMessage());
        }
        HttpPost post = new HttpPost(uri);
        headers.forEach(post::setHeader);
        post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        try {
            return client.execute(post, response -> new Response(response.getCode(), read(response.getEntity())));
        } catch (BlockedAddressException e) {
            throw new UnsafeUrlException(e.getMessage());
        }
    }

    private URI parse(String url) {
        URI uri;
        try {
            uri = new URI(url.strip());
        } catch (URISyntaxException e) {
            throw new UnsafeUrlException("That isn't a valid URL.");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new UnsafeUrlException("The URL needs a host name, such as https://hooks.zapier.com/….");
        }
        if (uri.getRawUserInfo() != null) {
            throw new UnsafeUrlException("The URL can't contain a user name or password.");
        }
        if (allowPrivateNetworks && scheme.equals("http")) {
            return uri;
        }
        if (!scheme.equals("https")) {
            throw new UnsafeUrlException("The URL must start with https://.");
        }
        if (!HTTPS_PORTS.contains(uri.getPort())) {
            throw new UnsafeUrlException("The URL must use the standard HTTPS port (443) or 8443.");
        }
        return uri;
    }

    private static String read(@Nullable HttpEntity entity) throws IOException {
        if (entity == null) {
            return "";
        }
        try (InputStream in = entity.getContent()) {
            return in == null ? "" : new String(in.readNBytes(MAX_RESPONSE_BYTES), StandardCharsets.UTF_8);
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    /** The connection's DNS lookup, refusing any answer that includes a non-public address. */
    private record CheckingResolver(DnsResolver dns, boolean allowPrivateNetworks) implements DnsResolver {

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            InetAddress[] addresses = dns.resolve(host);
            if (!allowPrivateNetworks) {
                for (InetAddress address : addresses) {
                    var why = PublicAddresses.whyNot(address);
                    if (why.isPresent()) {
                        throw new BlockedAddressException("%s resolves to %s, which %s. Webhooks only go to public addresses."
                                .formatted(host, address.getHostAddress(), why.get()));
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
