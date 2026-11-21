package io.github.aaronfilson.studiomanager.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

class RegisterRateLimitTests {

  @Test
  void capsDynamicClientRegistrationsPerMinute() throws Exception {
    RegisterRateLimit filter = new RegisterRateLimit(2);
    FilterChain passThrough = (req, res) -> {};

    for (int i = 0; i < 2; i++) {
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/register");
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilter(request, response, passThrough);
      assertThat(response.getStatus()).isEqualTo(200);
    }

    MockHttpServletRequest blocked = new MockHttpServletRequest("POST", "/oauth2/register");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(blocked, response, passThrough);
    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isEqualTo("60");
  }

  @Test
  void capsPerIpWithoutExhaustingGlobalCeiling() throws Exception {
    RegisterRateLimit filter = new RegisterRateLimit(4, 2);
    FilterChain passThrough = (req, res) -> {};

    // IP A uses its 2 per-IP tokens, then is blocked
    for (int i = 0; i < 2; i++) {
      MockHttpServletRequest req = new MockHttpServletRequest("POST", "/oauth2/register");
      req.setRemoteAddr("198.51.100.1");
      MockHttpServletResponse res = new MockHttpServletResponse();
      filter.doFilter(req, res, passThrough);
      assertThat(res.getStatus()).isEqualTo(200);
    }
    MockHttpServletRequest blockedIpA = new MockHttpServletRequest("POST", "/oauth2/register");
    blockedIpA.setRemoteAddr("198.51.100.1");
    MockHttpServletResponse blockedIpARes = new MockHttpServletResponse();
    filter.doFilter(blockedIpA, blockedIpARes, passThrough);
    assertThat(blockedIpARes.getStatus()).isEqualTo(429);

    // IP B still gets its 2 requests (IP A's rejected calls did not consume global tokens)
    for (int i = 0; i < 2; i++) {
      MockHttpServletRequest req = new MockHttpServletRequest("POST", "/oauth2/register");
      req.setRemoteAddr("198.51.100.2");
      MockHttpServletResponse res = new MockHttpServletResponse();
      filter.doFilter(req, res, passThrough);
      assertThat(res.getStatus()).isEqualTo(200);
    }

    // Global ceiling (4 total) now blocks IP C as well
    MockHttpServletRequest ipC = new MockHttpServletRequest("POST", "/oauth2/register");
    ipC.setRemoteAddr("198.51.100.3");
    MockHttpServletResponse ipCRes = new MockHttpServletResponse();
    filter.doFilter(ipC, ipCRes, passThrough);
    assertThat(ipCRes.getStatus()).isEqualTo(429);
  }

  @Test
  void rejectsOversizedRegistrationPayloadBeforeDeserialization() throws Exception {
    RegisterRateLimit filter = new RegisterRateLimit(10, 10);
    byte[] oversized =
        ("{\"pad\":\"" + "a".repeat(RegisterRateLimit.MAX_BODY_BYTES + 128) + "\"}")
            .getBytes(StandardCharsets.UTF_8);

    // Rejected upfront via Content-Length
    MockHttpServletRequest withLength = new MockHttpServletRequest("POST", "/oauth2/register");
    withLength.setContent(oversized);
    MockHttpServletResponse withLengthRes = new MockHttpServletResponse();
    filter.doFilter(withLength, withLengthRes, (req, res) -> {});
    assertThat(withLengthRes.getStatus()).isEqualTo(413);

    // Also rejected when streaming without a Content-Length header
    MockHttpServletRequest chunked =
        new MockHttpServletRequest("POST", "/oauth2/register") {
          @Override
          public long getContentLengthLong() {
            return -1L;
          }
        };
    chunked.setContent(oversized);
    MockHttpServletResponse chunkedRes = new MockHttpServletResponse();
    filter.doFilter(chunked, chunkedRes, (req, res) -> req.getInputStream().readAllBytes());
    assertThat(chunkedRes.getStatus()).isEqualTo(413);
  }

  @Test
  void dynamicClientRepositoryEvictsLeastRecentlyUsedClient() {
    McpClientRepository repo = new McpClientRepository("entra-client", List.of(), 2);
    RegisteredClient first = dynamicClient("id-1", "mcp_1");
    RegisteredClient second = dynamicClient("id-2", "mcp_2");
    RegisteredClient third = dynamicClient("id-3", "mcp_3");

    repo.save(first);
    repo.save(second);
    // Access first via clientId so second becomes least recently used
    assertThat(repo.findByClientId("mcp_1")).isNotNull();

    repo.save(third);

    assertThat(repo.findByClientId("mcp_1")).isNotNull();
    assertThat(repo.findByClientId("mcp_2")).isNull();
    assertThat(repo.findById("id-2")).isNull();
    assertThat(repo.findByClientId("mcp_3")).isNotNull();
  }

  private static RegisteredClient dynamicClient(String id, String clientId) {
    return RegisteredClient.withId(id)
        .clientId(clientId)
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("http://127.0.0.1:4321/cb")
        .scope("read")
        .build();
  }
}
