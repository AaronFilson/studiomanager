package io.github.aaronfilson.studiomanager.auth;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/** Fresh RSA keys as PKCS#8 PEM, the form the signing-keys secret takes. */
final class TestKeys {

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
