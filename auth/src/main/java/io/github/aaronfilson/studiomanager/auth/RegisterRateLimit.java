package io.github.aaronfilson.studiomanager.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Caps RFC 7591 dynamic client registrations per minute on this replica, using a shared Bucket4j
 * bucket like {@link AuthorizeRateLimit}.
 */
final class RegisterRateLimit extends OncePerRequestFilter {

  static final String REGISTER_PATH = "/oauth2/register";

  private final Bucket bucket;

  RegisterRateLimit(int requestsPerMinute) {
    if (requestsPerMinute < 1) {
      throw new IllegalStateException(
          "studiomanager.bridge.mcp.register-requests-per-minute must be at least 1.");
    }
    this.bucket =
        Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(requestsPerMinute)
                    .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                    .build())
            .build();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !REGISTER_PATH.equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (bucket.tryConsume(1)) {
      chain.doFilter(request, response);
      return;
    }
    response.setHeader("Retry-After", "60");
    response.sendError(
        HttpStatus.TOO_MANY_REQUESTS.value(), "Too many client registrations; try again soon.");
  }
}
