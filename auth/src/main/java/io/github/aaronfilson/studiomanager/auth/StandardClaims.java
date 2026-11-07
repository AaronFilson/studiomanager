package io.github.aaronfilson.studiomanager.auth;

/** The OpenID Connect claim names the bridge issues. */
final class StandardClaims {

  static final String SUBJECT = "sub";
  static final String EMAIL = "email";
  static final String EMAIL_VERIFIED = "email_verified";
  static final String NAME = "name";
  static final String PICTURE = "picture";

  private StandardClaims() {}
}
