package io.github.aaronfilson.studiomanager.demo;

import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The people of Demo Clay Co. Each is an ordinary account, made the way a first sign-in makes one
 * ({@link AccountResolver#resolve}), but with an issuer no identity provider uses, so the token
 * validation refuses anyone claiming to be them. They have no email address, so nothing the demo
 * does sends mail.
 */
enum DemoPeople {
  MARA("demo-owner", "Mara Lindqvist", StudioRole.OWNER),
  THEO("demo-staff", "Theo Okafor", StudioRole.STAFF),
  PRIYA("demo-member-priya", "Priya Shah", StudioRole.MEMBER),
  SAM("demo-member-sam", "Sam Rivera", StudioRole.MEMBER),
  JUN("demo-member-jun", "Jun Takahashi", StudioRole.MEMBER);

  /** The issuer on every demo account. Not a URL, so no identity provider can issue it. */
  static final String ISSUER = "urn:studiomanager:demo";

  private final String subject;
  private final String displayName;
  private final StudioRole role;

  DemoPeople(String subject, String displayName, StudioRole role) {
    this.subject = subject;
    this.displayName = displayName;
    this.role = role;
  }

  StudioRole role() {
    return role;
  }

  /** This person's account id, creating the account the first time. */
  UUID accountId(AccountResolver accounts) {
    return accounts.resolve(
        Jwt.withTokenValue("demo-seed")
            .header("alg", "none")
            .claim("iss", ISSUER)
            .subject(subject)
            .claim("name", displayName)
            .build());
  }
}
