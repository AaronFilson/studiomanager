package io.github.aaronfilson.studiomanager.platform.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.aaronfilson.studiomanager.platform.http.SafeHttpClient.UnsafeUrlException;
import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.Test;

/** The SSRF filter, with DNS faked so nothing leaves the machine. */
class SafeHttpClientTests {

    @Test
    void refusesAnythingButHttpsOnTheUsualPorts() throws Exception {
        try (var client = new SafeHttpClient("test", false, dns("8.8.8.8"))) {
            assertThatThrownBy(() -> client.check("http://hooks.example.com/x")).isInstanceOf(UnsafeUrlException.class)
                    .hasMessageContaining("https://");
            assertThatThrownBy(() -> client.check("ftp://hooks.example.com/x")).isInstanceOf(UnsafeUrlException.class);
            assertThatThrownBy(() -> client.check("https://hooks.example.com:6379/x"))
                    .isInstanceOf(UnsafeUrlException.class).hasMessageContaining("port");
            assertThatThrownBy(() -> client.check("https://user:pass@hooks.example.com/x"))
                    .isInstanceOf(UnsafeUrlException.class).hasMessageContaining("password");
            assertThatThrownBy(() -> client.check("https:///nohost")).isInstanceOf(UnsafeUrlException.class);
            assertThat(client.check("https://hooks.example.com/catch/1").getHost()).isEqualTo("hooks.example.com");
            assertThat(client.check("https://hooks.example.com:8443/x").getPort()).isEqualTo(8443);
        }
    }

    @Test
    void refusesAHostWithAnyPrivateAddress() throws Exception {
        // One public answer and one private: refused, because the connection might pick either
        try (var client = new SafeHttpClient("test", false, dns("8.8.8.8", "10.0.0.5"))) {
            assertThatThrownBy(() -> client.check("https://sneaky.example.com/"))
                    .isInstanceOf(UnsafeUrlException.class).hasMessageContaining("10.0.0.5")
                    .hasMessageContaining("private");
        }
    }

    @Test
    void refusesIpLiteralsAndTheMetadataEndpoint() throws Exception {
        try (var client = new SafeHttpClient("test", false)) {
            assertThatThrownBy(() -> client.check("https://169.254.169.254/metadata"))
                    .isInstanceOf(UnsafeUrlException.class).hasMessageContaining("metadata");
            assertThatThrownBy(() -> client.check("https://[::1]/")).isInstanceOf(UnsafeUrlException.class);
            assertThatThrownBy(() -> client.postJson("https://127.0.0.1/", Map.of(), "{}"))
                    .isInstanceOf(UnsafeUrlException.class).hasMessageContaining("loopback");
        }
    }

    @Test
    void checksAgainWhenSendingSoRebindingGetsNowhere() throws Exception {
        // Public when the URL is saved, loopback by the time it's sent
        AtomicInteger lookups = new AtomicInteger();
        DnsResolver rebinding = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                return new InetAddress[] {
                        InetAddress.getByName(lookups.getAndIncrement() == 0 ? "8.8.8.8" : "127.0.0.1") };
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                return host;
            }
        };
        try (var client = new SafeHttpClient("test", false, rebinding)) {
            client.check("https://rebind.example.com/");
            assertThatThrownBy(() -> client.postJson("https://rebind.example.com/", Map.of(), "{}"))
                    .isInstanceOf(UnsafeUrlException.class).hasMessageContaining("127.0.0.1");
        }
    }

    @Test
    void allowsLocalhostOnlyWhenTold() throws Exception {
        try (var client = new SafeHttpClient("test", true)) {
            assertThat(client.check("http://localhost:8080/hook").getPort()).isEqualTo(8080);
        }
        try (var client = new SafeHttpClient("test", false)) {
            assertThatThrownBy(() -> client.check("http://localhost:8080/hook")).isInstanceOf(UnsafeUrlException.class);
        }
    }

    private static DnsResolver dns(String... addresses) {
        return new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                InetAddress[] resolved = new InetAddress[addresses.length];
                for (int i = 0; i < addresses.length; i++) {
                    resolved[i] = InetAddress.getByName(addresses[i]);
                }
                return resolved;
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                return host;
            }
        };
    }

}
