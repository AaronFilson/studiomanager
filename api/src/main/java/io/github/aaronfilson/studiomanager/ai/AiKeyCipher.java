package io.github.aaronfilson.studiomanager.ai;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Encrypts a studio's AI key at rest: AES-256-GCM under a key-encryption key from config (Key
 * Vault in Azure), with the studio's id as associated data. The same scheme as webhook secrets
 * (ADR 14), with its own key so one leaked key doesn't open both.
 */
@Component
class AiKeyCipher {

    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    AiKeyCipher(@Value("${studiomanager.ai.key-encryption-key:}") @Nullable String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            // Local work and tests. application-azure.yaml requires a real key, so this can't ship.
            LoggerFactory.getLogger(AiKeyCipher.class)
                    .warn("No studiomanager.ai.key-encryption-key; using the fixed development key");
            this.key = new SecretKeySpec(sha256("studiomanager ai keys, local development only"), "AES");
            return;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Key.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("studiomanager.ai.key-encryption-key must be base64", e);
        }
        if (bytes.length != 32) {
            throw new IllegalStateException("studiomanager.ai.key-encryption-key must be 32 bytes (AES-256)");
        }
        this.key = new SecretKeySpec(bytes, "AES");
    }

    byte[] encrypt(UUID studioId, String secret) {
        byte[] nonce = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(studioId));
            byte[] sealed = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(Math.addExact(NONCE_BYTES, sealed.length)).put(nonce).put(sealed).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Couldn't encrypt an AI key", e);
        }
    }

    String decrypt(UUID studioId, byte[] stored) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, stored, 0, NONCE_BYTES));
            cipher.updateAAD(aad(studioId));
            return new String(cipher.doFinal(stored, NONCE_BYTES, stored.length - NONCE_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // Never include the ciphertext or key in the message
            throw new IllegalStateException("Couldn't decrypt the AI key of studio " + studioId, e);
        }
    }

    private static byte[] aad(UUID studioId) {
        return ByteBuffer.allocate(16).putLong(studioId.getMostSignificantBits())
                .putLong(studioId.getLeastSignificantBits()).array();
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

}
