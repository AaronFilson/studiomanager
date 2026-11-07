package io.github.aaronfilson.studiomanager.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.Test;

class SigningKeysTests {

  @Test
  void theFirstKeySignsAndTheRestArePublishedWithoutTheirPrivateHalf() {
    String current = TestKeys.pem(TestKeys.ACCEPTED_BITS);
    String retiring = TestKeys.pem(TestKeys.ACCEPTED_BITS);

    SigningKeys keys = SigningKeys.fromPem(current + retiring);

    assertThat(keys.signingKey().isPrivate()).isTrue();
    assertThat(keys.keySet().getKeys()).hasSize(2);
    JWK published = keys.keySet().getKeys().get(1);
    assertThat(published.isPrivate()).isFalse();
    assertThat(published.getKeyID()).isNotEqualTo(keys.signingKey().getKeyID());
  }

  @Test
  void theKeyIdIsStableForTheSameKey() {
    String pem = TestKeys.pem(TestKeys.ACCEPTED_BITS);

    assertThat(SigningKeys.fromPem(pem).signingKey().getKeyID())
        .isEqualTo(SigningKeys.fromPem(pem).signingKey().getKeyID());
  }

  @Test
  void refusesASecretWithNoKey() {
    assertThatThrownBy(() -> SigningKeys.fromPem("not a key"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no PKCS#8");
  }

  @Test
  void refusesAShortKey() {
    assertThatThrownBy(() -> SigningKeys.fromPem(TestKeys.pem(TestKeys.TOO_SHORT_BITS)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("2048");
  }
}
