package io.github.aaronfilson.studiomanager.auth;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/** Fresh RSA keys as PKCS#8 PEM, the form the signing-keys secret takes. */
final class TestKeys {

  /** The smallest key the bridge accepts. */
  static final int ACCEPTED_BITS = 2048;

  /** Deliberately too short: only for checking that the bridge refuses it. */
  static final int TOO_SHORT_BITS = 1024;

  private TestKeys() {}

  static String pem(int bits) {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(bits);
      byte[] der = generator.generateKeyPair().getPrivate().getEncoded();
      String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
      return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
