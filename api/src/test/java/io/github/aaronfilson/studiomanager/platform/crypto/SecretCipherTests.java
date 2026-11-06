package io.github.aaronfilson.studiomanager.platform.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SecretCipherTests {

  private static final String PROPERTY = "test.keyring";
  private static final String OLD_KEY = key((byte) 1);
  private static final String NEW_KEY = key((byte) 2);

  @Test
  void roundTripsAndBindsTheSecretToItsOwner() {
    SecretCipher cipher = SecretCipher.fromKeyring(PROPERTY, OLD_KEY, "test secret");
    UUID owner = UUID.randomUUID();
    byte[] sealed = cipher.encrypt(owner, "hunter2");
    assertThat(new String(sealed, StandardCharsets.ISO_8859_1)).doesNotContain("hunter2");
    assertThat(cipher.decrypt(owner, sealed)).isEqualTo("hunter2");
    assertThatThrownBy(() -> cipher.decrypt(UUID.randomUUID(), sealed))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void aBareKeyIsVersionOne() {
    SecretCipher cipher = SecretCipher.fromKeyring(PROPERTY, OLD_KEY, "test secret");
    assertThat(SecretCipher.versionOf(cipher.encrypt(UUID.randomUUID(), "x"))).isEqualTo(1);
  }

  @Test
  void afterRotationOldSecretsStillDecryptAndNewOnesUseTheCurrentKey() {
    UUID owner = UUID.randomUUID();
    byte[] sealedBefore =
        SecretCipher.fromKeyring(PROPERTY, "1:" + OLD_KEY, "test secret").encrypt(owner, "before");

    SecretCipher rotated =
        SecretCipher.fromKeyring(PROPERTY, "2:" + NEW_KEY + ", 1:" + OLD_KEY, "test secret");
    assertThat(rotated.decrypt(owner, sealedBefore)).isEqualTo("before");
    byte[] sealedAfter = rotated.encrypt(owner, "after");
    assertThat(SecretCipher.versionOf(sealedAfter)).isEqualTo(2);

    // Once the old key is retired, only secrets sealed under the new one decrypt
    SecretCipher retired = SecretCipher.fromKeyring(PROPERTY, "2:" + NEW_KEY, "test secret");
    assertThat(retired.decrypt(owner, sealedAfter)).isEqualTo("after");
    assertThatThrownBy(() -> retired.decrypt(owner, sealedBefore))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("key version 1");
  }

  @Test
  void aRelabelledVersionDoesNotDecrypt() {
    UUID owner = UUID.randomUUID();
    SecretCipher cipher =
        SecretCipher.fromKeyring(PROPERTY, "2:" + OLD_KEY + ",1:" + OLD_KEY, "test secret");
    byte[] sealed = cipher.encrypt(owner, "secret");
    sealed[0] = 1;
    assertThatThrownBy(() -> cipher.decrypt(owner, sealed))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void refusesBadKeyrings() {
    assertThatThrownBy(() -> SecretCipher.fromKeyring(PROPERTY, "c2hvcnQ=", "test secret"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> SecretCipher.fromKeyring(PROPERTY, "0:" + OLD_KEY, "test secret"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() ->
            SecretCipher.fromKeyring(PROPERTY, "1:" + OLD_KEY + ",1:" + NEW_KEY, "test secret"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void blankUsesTheDevelopmentKey() {
    SecretCipher cipher = SecretCipher.fromKeyring(PROPERTY, "", "test secret");
    UUID owner = UUID.randomUUID();
    assertThat(cipher.decrypt(owner, cipher.encrypt(owner, "local"))).isEqualTo("local");
  }

  private static String key(byte fill) {
    byte[] bytes = new byte[32];
    java.util.Arrays.fill(bytes, fill);
    return Base64.getEncoder().encodeToString(bytes);
  }
}
