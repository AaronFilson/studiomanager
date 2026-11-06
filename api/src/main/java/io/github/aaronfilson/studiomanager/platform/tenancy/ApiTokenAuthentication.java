package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** A request signed in with a personal API token rather than a person's access token. */
public final class ApiTokenAuthentication extends AbstractAuthenticationToken {

  /**
   * The authority every token-authenticated request carries, next to a person's {@code
   * SCOPE_access_as_user}.
   */
  public static final String AUTHORITY = "API_TOKEN";

  private final ApiTokenResolver.ResolvedToken token;

  public ApiTokenAuthentication(ApiTokenResolver.ResolvedToken token) {
    super(List.of(new SimpleGrantedAuthority(AUTHORITY)));
    this.token = token;
    setAuthenticated(true);
  }

  public ApiTokenResolver.ResolvedToken token() {
    return token;
  }

  @Override
  public Object getCredentials() {
    // The secret is never kept after it's checked
    return "";
  }

  @Override
  public Object getPrincipal() {
    return token.accountId();
  }
}
