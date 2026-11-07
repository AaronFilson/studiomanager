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
 * Caps how many sign-ins this replica starts per minute, across all callers (plan 18: rate limits
 * on /authorize). One shared bucket rather than one per address: the bridge sees Container Apps'
 * proxy, and a per-address limit would mean trusting a forwarded header. A real sign-in needs one
 * request, so the cap only bites on floods; the bridge runs one replica (ADR 20).
 */
final class AuthorizeRateLimit extends OncePerRequestFilter {

  static final String AUTHORIZE_PATH = "/oauth2/authorize";

  private final Bucket bucket;

  AuthorizeRateLimit(int requestsPerMinute) {
    if (requestsPerMinute < 1) {
      throw new IllegalStateException(
          "studiomanager.bridge.authorize-requests-per-minute must be at least 1.");
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
    return !AUTHORIZE_PATH.equals(request.getRequestURI());
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
    response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many sign-ins; try again soon.");
  }
}
