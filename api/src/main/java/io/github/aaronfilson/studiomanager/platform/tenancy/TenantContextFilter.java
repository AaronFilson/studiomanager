package io.github.aaronfilson.studiomanager.platform.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.PathContainer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Binds the {@link TenantContext} for an authenticated request.
 *
 * <p>Runs in the security filter chain after authorization. The studio comes from the URL ({@code
 * /api/v1/studios/{studioId}/...}) and is bound only once the caller's membership is confirmed, so
 * row-level security never sees a studio the caller doesn't belong to.
 */
public class TenantContextFilter extends OncePerRequestFilter {

  private static final PathPattern STUDIO_ROUTE =
      PathPatternParser.defaultInstance.parse("/api/v1/studios/{studioId}/**");
  private static final PathPattern STUDIO_AREA =
      PathPatternParser.defaultInstance.parse("/api/v1/studios/{studioId}/{area}/**");

  private final AccountResolver accounts;
  private final MembershipLookup memberships;
  private final RolePermissions rolePermissions;

  public TenantContextFilter(
      AccountResolver accounts, MembershipLookup memberships, RolePermissions rolePermissions) {
    this.accounts = accounts;
    this.memberships = memberships;
    this.rolePermissions = rolePermissions;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    TenantContext context;
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwt) {
      TenantContext caller = TenantContext.forAccount(accounts.resolve(jwt.getToken()));
      UUID studioId = studioIdFrom(request);
      context = studioId == null ? caller : inStudio(caller, studioId);
    } else if (authentication instanceof ApiTokenAuthentication token) {
      context = tokenContext(request, token.token());
    } else {
      chain.doFilter(request, response);
      return;
    }
    try {
      TenantContext.callAs(
          context,
          () -> {
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

  /**
   * A token works only in its own studio, only where {@link TokenPolicy} allows its scopes, and
   * only while its person holds {@code api_tokens.own} there. It holds what its person holds now
   * and its scopes allow ({@link TokenScope#allowedBy}), resolved on every request, so a change to
   * the person's role or their role's set applies to the token at once (plan 21 §5).
   */
  private TenantContext tokenContext(
      HttpServletRequest request, ApiTokenResolver.ResolvedToken token) {
    UUID studioId = studioIdFrom(request);
    if (studioId != null && !studioId.equals(token.studioId())) {
      throw new AccessDeniedException("This token works only in the studio it was made for.");
    }
    TokenPolicy.refusal(
            token.scopes(), request.getMethod(), studioId == null ? null : areaOf(request))
        .ifPresent(why -> {
          throw new AccessDeniedException(why);
        });
    TenantContext person = inStudio(TenantContext.forAccount(token.accountId()), token.studioId());
    if (!person.holds(Permission.API_TOKENS_OWN)) {
      throw new AccessDeniedException(
          "This token's owner may no longer use API tokens in this studio.");
    }
    EnumSet<Permission> held = EnumSet.noneOf(Permission.class);
    held.addAll(person.permissions());
    held.retainAll(TokenScope.allowedBy(token.scopes()));
    return person.viaToken(token.tokenId()).withPermissions(held);
  }

  /**
   * The caller in {@code studioId}, once their membership is confirmed, holding what their role and
   * duties there give them. The lookups run bound to the studio, so row-level security lets them
   * read its roles and duties.
   */
  private TenantContext inStudio(TenantContext caller, UUID studioId) {
    TenantContext member = caller.inStudio(studioId, roleIn(studioId, caller));
    return TenantContext.callAs(member, () -> rolePermissions.resolve(member));
  }

  private static String areaOf(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    PathPattern.PathMatchInfo match = STUDIO_AREA.matchAndExtract(PathContainer.parsePath(path));
    // STUDIO_AREA always captures {area}, so a match always has it
    return match == null ? "" : match.getUriVariables().getOrDefault("area", "");
  }

  private StudioRole roleIn(UUID studioId, TenantContext caller) {
    return TenantContext.callAs(caller, () -> memberships.roleOf(caller.accountId(), studioId))
        .orElseThrow(() -> new AccessDeniedException("You are not a member of this studio."));
  }

  private static @Nullable UUID studioIdFrom(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    PathPattern.PathMatchInfo match = STUDIO_ROUTE.matchAndExtract(PathContainer.parsePath(path));
    if (match == null) {
      return null;
    }
    try {
      return UUID.fromString(match.getUriVariables().get("studioId"));
    } catch (IllegalArgumentException notAUuid) {
      // No tenant to bind; MVC rejects the malformed path variable with a 400
      return null;
    }
  }
}
