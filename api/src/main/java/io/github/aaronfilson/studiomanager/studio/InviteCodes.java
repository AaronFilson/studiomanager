package io.github.aaronfilson.studiomanager.studio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Invite codes: random, URL-safe, and stored only as a hash. */
final class InviteCodes {

  private static final SecureRandom RANDOM = new SecureRandom();

  private InviteCodes() {}

  static String newCode() {
    byte[] bytes = new byte[18];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  // Must match app.accept_invite: sha256(convert_to(code, 'UTF8'))
  static byte[] sha256(String code) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Every JDK provides SHA-256", e);
    }
  }
}
