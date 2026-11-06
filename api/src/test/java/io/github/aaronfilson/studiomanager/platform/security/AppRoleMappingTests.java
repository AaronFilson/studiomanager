package io.github.aaronfilson.studiomanager.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Entra puts scopes in {@code scp} and app roles in {@code roles}; both become authorities. */
class AppRoleMappingTests {

  @Test
  void scopesAndAppRolesBothBecomeAuthorities() {
    Jwt token =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("owner")
            .issuedAt(Instant.parse("2026-11-06T15:00:00Z"))
            .claim("scp", "access_as_user")
            .claim("roles", List.of("Platform.Admin"))
            .build();

    var authentication = SecurityConfiguration.scopesAndAppRoles().convert(token);

    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        // Spring Security also records how the caller signed in, as FACTOR_BEARER
        .containsExactlyInAnyOrder(
            "SCOPE_access_as_user", "APPROLE_Platform.Admin", "FACTOR_BEARER");
  }

  @Test
  void aTokenWithoutRolesHasOnlyItsScopes() {
    Jwt token =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("member")
            .claim("scp", "access_as_user")
            .build();

    var authentication = SecurityConfiguration.scopesAndAppRoles().convert(token);

    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("SCOPE_access_as_user", "FACTOR_BEARER");
  }
}
