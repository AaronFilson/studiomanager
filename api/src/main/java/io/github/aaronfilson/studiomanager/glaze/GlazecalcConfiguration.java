package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.crypto.SecretCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GlazecalcProperties.class)
class GlazecalcConfiguration {

  private static final Logger log = LoggerFactory.getLogger(GlazecalcConfiguration.class);

  @Bean(destroyMethod = "close")
  GlazecalcHttpClient glazecalcHttpClient(GlazecalcProperties properties) {
    if (properties.allowPrivateNetworks()) {
      log.warn(
          "Glazecalc requests may go to private addresses (studiomanager.glazecalc.allow-private-networks). "
              + "Never set this in a deployed environment.");
    }
    return new GlazecalcHttpClient(properties.allowPrivateNetworks());
  }

  @Bean
  GlazecalcTokenCipher glazecalcTokenCipher(GlazecalcProperties properties) {
    boolean keyConfigured = properties.tokenEncryptionKeyConfigured();
    if (properties.oauthConfigured() && !keyConfigured) {
      throw new IllegalStateException(
          "studiomanager.glazecalc.token-encryption-key must be configured when Glazecalc OAuth is enabled.");
    }
    return new GlazecalcTokenCipher(
        SecretCipher.fromKeyring(
            "studiomanager.glazecalc.token-encryption-key",
            properties.tokenEncryptionKey(),
            "glazecalc token"),
        keyConfigured);
  }

  /** Named wrapper so Spring keeps this SecretCipher distinct from other modules' ciphers. */
  record GlazecalcTokenCipher(SecretCipher cipher, boolean keyConfigured) {}
}
