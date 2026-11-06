package io.github.aaronfilson.studiomanager.ai;

import io.github.aaronfilson.studiomanager.platform.crypto.SecretCipher;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Encrypts a studio's AI key at rest with {@link SecretCipher}, under the AI keyring from config
 * (Key Vault in Azure), with the studio's id as associated data. The same scheme as webhook secrets
 * (ADR 14), with its own keyring so one leaked key doesn't open both.
 */
@Component
class AiKeyCipher {

  private final SecretCipher cipher;

  AiKeyCipher(@Value("${studiomanager.ai.key-encryption-key:}") @Nullable String keyring) {
    this.cipher =
        SecretCipher.fromKeyring("studiomanager.ai.key-encryption-key", keyring, "AI key");
  }

  byte[] encrypt(UUID studioId, String secret) {
    return cipher.encrypt(studioId, secret);
  }

  String decrypt(UUID studioId, byte[] stored) {
    return cipher.decrypt(studioId, stored);
  }
}
