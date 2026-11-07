package io.github.aaronfilson.studiomanager.platform.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

/**
 * Encrypts secrets the API must read back (webhook signing secrets, studios' AI keys) with
 * AES-256-GCM under a keyring, so the key can rotate without making stored secrets unreadable.
 *
 * <p>The stored form is one key-version byte, a 12-byte nonce, then the ciphertext and tag. New
 * secrets use the current key; a secret sealed under an older key still decrypts while that key is
 * on the ring. The owner's id (the endpoint or the studio) and the version are the associated data,
 * so a secret copied onto another row, or relabelled with another version, won't decrypt.
 *
 * <p>The keyring is one config value: comma-separated {@code version:base64key} entries, current
 * first, for example {@code 2:NEWKEY...,1:OLDKEY...}. A bare base64 key is version 1. Versions run
 * from 1 to 255. Each use has its own keyring, so one leaked key doesn't open both (ADR 14).
 */
public final class SecretCipher {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int VERSION_BYTES = 1;
  private static final int NONCE_BYTES = 12;
  private static final int HEADER_BYTES = VERSION_BYTES + NONCE_BYTES;
  private static final int TAG_BITS = 128;
  private static final int KEY_BYTES = 32;
  private static final int MAX_VERSION = 255;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final int currentVersion;
  private final SecretKeySpec currentKey;
  private final Map<Integer, SecretKeySpec> keysByVersion;
  private final String purpose;

  private SecretCipher(
      int currentVersion,
      SecretKeySpec currentKey,
      Map<Integer, SecretKeySpec> keysByVersion,
      String purpose) {
    this.currentVersion = currentVersion;
    this.currentKey = currentKey;
    this.keysByVersion = Map.copyOf(keysByVersion);
    this.purpose = purpose;
  }

  /**
   * Reads a keyring from config.
   *
   * @param propertyName the config property, named in errors
   * @param keyring the property's value; blank uses a fixed development key, for local work and
   *     tests only (the azure profile requires a real one)
   * @param purpose what the secrets are, for error messages, such as "webhook secret"
   */
  public static SecretCipher fromKeyring(
      String propertyName, @Nullable String keyring, String purpose) {
    if (keyring == null || keyring.isBlank()) {
      LoggerFactory.getLogger(SecretCipher.class)
          .warn("No {}; using the fixed development key", propertyName);
      SecretKeySpec developmentKey =
          new SecretKeySpec(sha256("studiomanager " + purpose + ", local development only"), "AES");
      return new SecretCipher(1, developmentKey, Map.of(1, developmentKey), purpose);
    }
    Map<Integer, SecretKeySpec> keysByVersion = new LinkedHashMap<>();
    int currentVersion = 0;
    SecretKeySpec currentKey = null;
    for (String entry : keyring.split(",", -1)) {
      String trimmedEntry = entry.strip();
      if (trimmedEntry.isEmpty()) {
        // A trailing or doubled comma, as split without a limit used to drop silently
        continue;
      }
      int separator = trimmedEntry.indexOf(':');
      int version =
          separator < 0 ? 1 : parseVersion(propertyName, trimmedEntry.substring(0, separator));
      SecretKeySpec key = parseKey(propertyName, trimmedEntry.substring(separator + 1));
      if (keysByVersion.put(version, key) != null) {
        throw new IllegalStateException(propertyName + " lists key version " + version + " twice");
      }
      if (currentKey == null) {
        currentVersion = version;
        currentKey = key;
      }
    }
    if (currentKey == null) {
      throw new IllegalStateException(propertyName + " has no keys");
    }
    return new SecretCipher(currentVersion, currentKey, keysByVersion, purpose);
  }

  /** The key version a stored secret was sealed under. */
  public static int versionOf(byte[] stored) {
    return Byte.toUnsignedInt(stored[0]);
  }

  public byte[] encrypt(UUID ownerId, String secret) {
    byte[] nonce = new byte[NONCE_BYTES];
    RANDOM.nextBytes(nonce);
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, currentKey, new GCMParameterSpec(TAG_BITS, nonce));
      cipher.updateAAD(associatedData(ownerId, currentVersion));
      byte[] sealed = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
      return ByteBuffer.allocate(Math.addExact(HEADER_BYTES, sealed.length))
          .put((byte) currentVersion)
          .put(nonce)
          .put(sealed)
          .array();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Couldn't encrypt a " + purpose, e);
    }
  }

  public String decrypt(UUID ownerId, byte[] stored) {
    if (stored.length <= HEADER_BYTES) {
      throw new IllegalStateException("The stored " + purpose + " of " + ownerId + " is too short");
    }
    int version = versionOf(stored);
    SecretKeySpec key = keysByVersion.get(version);
    if (key == null) {
      throw new IllegalStateException(
          "The "
              + purpose
              + " of "
              + ownerId
              + " needs key version "
              + version
              + ", not on the ring");
    }
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(
          Cipher.DECRYPT_MODE,
          key,
          new GCMParameterSpec(TAG_BITS, stored, VERSION_BYTES, NONCE_BYTES));
      cipher.updateAAD(associatedData(ownerId, version));
      byte[] plain = cipher.doFinal(stored, HEADER_BYTES, stored.length - HEADER_BYTES);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      // Never include the ciphertext or the key in the message
      throw new IllegalStateException("Couldn't decrypt the " + purpose + " of " + ownerId, e);
    }
  }

  private static int parseVersion(String propertyName, String text) {
    int version;
    try {
      version = Integer.parseInt(text.strip());
    } catch (NumberFormatException e) {
      throw new IllegalStateException(propertyName + " has a key version that isn't a number", e);
    }
    if (version < 1 || version > MAX_VERSION) {
      throw new IllegalStateException(propertyName + " key versions run from 1 to " + MAX_VERSION);
    }
    return version;
  }

  private static SecretKeySpec parseKey(String propertyName, String base64Key) {
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(base64Key.strip());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(propertyName + " keys must be base64", e);
    }
    if (keyBytes.length != KEY_BYTES) {
      throw new IllegalStateException(propertyName + " keys must be 32 bytes (AES-256)");
    }
    return new SecretKeySpec(keyBytes, "AES");
  }

  private static byte[] associatedData(UUID ownerId, int version) {
    return ByteBuffer.allocate(17)
        .putLong(ownerId.getMostSignificantBits())
        .putLong(ownerId.getLeastSignificantBits())
        .put((byte) version)
        .array();
  }

  private static byte[] sha256(String text) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
