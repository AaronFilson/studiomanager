package io.github.aaronfilson.studiomanager.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
}
