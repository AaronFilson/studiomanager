package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
 * <p>Runs in the security filter chain after authorization. The studio comes from the URL
 * ({@code /api/v1/studios/{studioId}/...}) and is bound only once the caller's membership is
 * confirmed, so row-level security never sees a studio the caller doesn't belong to.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    private static final PathPattern STUDIO_ROUTE =
            PathPatternParser.defaultInstance.parse("/api/v1/studios/{studioId}/**");

    private final AccountResolver accounts;
    private final MembershipLookup memberships;

    public TenantContextFilter(AccountResolver accounts, MembershipLookup memberships) {
        this.accounts = accounts;
        this.memberships = memberships;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication)) {
            chain.doFilter(request, response);
            return;
        }
        TenantContext caller = TenantContext.forAccount(accounts.resolve(authentication.getToken()));
        UUID studioId = studioIdFrom(request);
        TenantContext context = studioId == null ? caller : caller.inStudio(studioId, roleIn(studioId, caller));
        try {
            TenantContext.callAs(context, () -> {
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
