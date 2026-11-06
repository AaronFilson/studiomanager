package io.github.aaronfilson.studiomanager.webhook;

import io.github.aaronfilson.studiomanager.platform.crypto.SecretCipher;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Endpoints' signing secrets: made here, shown to staff once, and kept only encrypted.
 *
 * <p>A secret has to be readable again to sign with, so it can't be hashed like an invite code.
 * It's encrypted by {@link SecretCipher} under the webhook keyring from config, with the endpoint's
 * id as associated data, so a secret copied onto another endpoint's row won't decrypt.
 */
class WebhookSecrets {

  static final String PREFIX = "whsec_";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final SecretCipher cipher;

  WebhookSecrets(@Nullable String keyring) {
    this.cipher =
        SecretCipher.fromKeyring("studiomanager.webhooks.secret-key", keyring, "webhook secret");
  }

  /** A new secret in the Standard Webhooks form: {@code whsec_} and 32 random bytes in base64. */
  static String newSecret() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return PREFIX + Base64.getEncoder().encodeToString(bytes);
  }

  byte[] encrypt(UUID endpointId, String secret) {
    return cipher.encrypt(endpointId, secret);
  }

  String decrypt(UUID endpointId, byte[] stored) {
    return cipher.decrypt(endpointId, stored);
  }
}
