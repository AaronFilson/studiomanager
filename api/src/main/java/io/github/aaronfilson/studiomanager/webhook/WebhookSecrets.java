package io.github.aaronfilson.studiomanager.webhook;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

/**
 * Endpoints' signing secrets: made here, shown to staff once, and kept only encrypted.
 *
 * <p>A secret has to be readable again to sign with, so it can't be hashed like an invite code.
 * It's encrypted with AES-GCM under a key from config. The endpoint's id is the associated data, so
 * a secret copied onto another endpoint's row won't decrypt.
 */
class WebhookSecrets {

  static final String PREFIX = "whsec_";
  private static final int NONCE_BYTES = 12;
  private static final int TAG_BITS = 128;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final SecretKeySpec key;

  WebhookSecrets(@Nullable String base64Key) {
    if (base64Key == null || base64Key.isBlank()) {
      // Local work and tests. application-azure.yaml requires a real key, so this can't ship.
      LoggerFactory.getLogger(WebhookSecrets.class)
          .warn("No studiomanager.webhooks.secret-key; using the fixed development key");
      this.key = new SecretKeySpec(sha256("studiomanager local development only"), "AES");
      return;
    }
    byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(base64Key.strip());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("studiomanager.webhooks.secret-key must be base64", e);
    }
    if (bytes.length != 32) {
      throw new IllegalStateException(
          "studiomanager.webhooks.secret-key must be 32 bytes (AES-256)");
    }
    this.key = new SecretKeySpec(bytes, "AES");
  }

  /** A new secret in the Standard Webhooks form: {@code whsec_} and 32 random bytes in base64. */
  static String newSecret() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return PREFIX + Base64.getEncoder().encodeToString(bytes);
  }

  byte[] encrypt(UUID endpointId, String secret) {
    byte[] nonce = new byte[NONCE_BYTES];
    RANDOM.nextBytes(nonce);
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
      cipher.updateAAD(aad(endpointId));
      byte[] sealed = cipher.doFinal(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return ByteBuffer.allocate(nonce.length + sealed.length).put(nonce).put(sealed).array();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Couldn't encrypt a webhook secret", e);
    }
  }

  String decrypt(UUID endpointId, byte[] stored) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, stored, 0, NONCE_BYTES));
      cipher.updateAAD(aad(endpointId));
      byte[] plain = cipher.doFinal(stored, NONCE_BYTES, stored.length - NONCE_BYTES);
      return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(
          "Couldn't decrypt the secret of webhook endpoint " + endpointId, e);
    }
  }

  private static byte[] sha256(String text) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  private static byte[] aad(UUID endpointId) {
    return ByteBuffer.allocate(16)
        .putLong(endpointId.getMostSignificantBits())
        .putLong(endpointId.getLeastSignificantBits())
        .array();
  }
}
