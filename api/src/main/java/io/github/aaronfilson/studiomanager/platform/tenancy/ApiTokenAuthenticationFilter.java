package io.github.aaronfilson.studiomanager.platform.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Signs a request in with a personal API token ({@code Authorization: Bearer smpat_...}). Any other
 * bearer is left for the JWT resource server. An unknown, expired or revoked token gets a 401 here,
 * and is never passed on.
 */
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER = "Bearer ";

  private final ApiTokenResolver tokens;
  private final AuthenticationEntryPoint entryPoint;

  public ApiTokenAuthenticationFilter(
      ApiTokenResolver tokens, AuthenticationEntryPoint entryPoint) {
    this.tokens = tokens;
    this.entryPoint = entryPoint;
  }

  /** True for a bearer that's a personal API token, so the JWT resolver can skip it. */
  public static boolean isApiToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    return header != null && header.startsWith(BEARER + ApiTokenResolver.PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!isApiToken(request)) {
      chain.doFilter(request, response);
      return;
    }
    String bearer = request.getHeader(HttpHeaders.AUTHORIZATION).substring(BEARER.length()).strip();
    var token = tokens.resolve(bearer, request.getRemoteAddr());
    if (token.isEmpty()) {
      SecurityContextHolder.clearContext();
      entryPoint.commence(
          request,
          response,
          new BadCredentialsException(
              "This API token isn't valid. It may have expired or been revoked."));
      return;
    }
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new ApiTokenAuthentication(token.get()));
    SecurityContextHolder.setContext(context);
    try {
      chain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
