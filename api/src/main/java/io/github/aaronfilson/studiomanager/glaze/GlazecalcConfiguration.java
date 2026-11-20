package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.crypto.SecretCipher;
import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GlazecalcProperties.class)
class GlazecalcConfiguration {

  @Bean(destroyMethod = "close")
  GlazecalcHttpClient glazecalcHttpClient(GlazecalcProperties properties) {
    URI base = URI.create(properties.resolvedBaseUrl());
    String host = base.getHost() == null ? "" : base.getHost();
    boolean localTestBase =
        host.equals("127.0.0.1") || host.equals("localhost") || host.equals("::1");
    return new GlazecalcHttpClient(properties.allowPrivateNetworks() || localTestBase);
  }

  @Bean
  GlazecalcTokenCipher glazecalcTokenCipher(GlazecalcProperties properties) {
    return new GlazecalcTokenCipher(
        SecretCipher.fromKeyring(
            "studiomanager.glazecalc.token-encryption-key",
            properties.tokenEncryptionKey(),
            "glazecalc token"));
  }

  /** Named wrapper so Spring keeps this SecretCipher distinct from other modules' ciphers. */
  record GlazecalcTokenCipher(SecretCipher cipher) {}
}
