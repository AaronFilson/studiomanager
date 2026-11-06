package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Whether the call is acting as a platform admin: someone who runs the service, not a studio role
 * (ADR 24).
 *
 * <p>The role comes from Entra as the {@code Platform.Admin} app role, in the access token's {@code
 * roles} claim. The security configuration maps it to {@link #AUTHORITY}. Only requests to {@code
 * /api/v1/admin/**} that carry it are bound with {@link #callAs}, and while bound, {@link
 * TenantAwareJpaTransactionManager} sets {@code app.platform_admin}, without which the database
 * refuses writes to the platform settings.
 */
public final class PlatformAdmin {

  /** The app role's value in the token's {@code roles} claim. */
  public static final String APP_ROLE = "Platform.Admin";

  /** The authority the role maps to. */
  public static final String AUTHORITY = "APPROLE_" + APP_ROLE;

  private static final ScopedValue<Boolean> BOUND = ScopedValue.newInstance();

  private PlatformAdmin() {}

  /** Whether this call is bound as a platform admin. */
  public static boolean isBound() {
    return BOUND.isBound() && BOUND.get();
  }

  /** Whether the signed-in caller has the role, bound or not. For showing the admin page. */
  public static boolean callerHasRole() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && hasRole(authentication);
  }

  static boolean hasRole(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(AUTHORITY::equals);
  }

  /** Runs {@code operation} bound as a platform admin. */
  public static <T, X extends Throwable> T callAs(ScopedValue.CallableOp<T, X> operation) throws X {
    Objects.requireNonNull(operation, "operation");
    return ScopedValue.where(BOUND, Boolean.TRUE).call(operation);
  }
}
