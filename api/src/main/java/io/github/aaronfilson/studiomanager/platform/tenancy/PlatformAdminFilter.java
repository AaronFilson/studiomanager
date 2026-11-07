package io.github.aaronfilson.studiomanager.platform.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Binds {@link PlatformAdmin} for a request to {@code /api/v1/admin/**} from someone signed in with
 * the {@code Platform.Admin} app role. Nowhere else, so the database binding is as narrow as the
 * routes that need it. Authorization has already refused anyone without the role; this only tells
 * the transactions.
 */
public class PlatformAdminFilter extends OncePerRequestFilter {

  private static final PathPattern ADMIN_ROUTE =
      PathPatternParser.defaultInstance.parse("/api/v1/admin/**");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    String path = request.getRequestURI().substring(request.getContextPath().length());
    boolean adminRoute = ADMIN_ROUTE.matches(PathContainer.parsePath(path));
    // Only a person's sign-in carries app roles; an API token never does
    if (!(adminRoute
        && authentication instanceof JwtAuthenticationToken signedIn
        && PlatformAdmin.hasRole(signedIn))) {
      chain.doFilter(request, response);
      return;
    }
    try {
      PlatformAdmin.callAs(() -> {
        chain.doFilter(request, response);
        return null;
      });
    } catch (ServletException | IOException | RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      // chain.doFilter declares only the exceptions above
      throw new ServletException(ex);
    }
  }
}
