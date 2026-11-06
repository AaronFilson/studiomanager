package io.github.aaronfilson.studiomanager.webhook;

import io.github.aaronfilson.studiomanager.platform.http.SafeHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WebhookProperties.class)
class WebhookConfiguration {

  private static final Logger log = LoggerFactory.getLogger(WebhookConfiguration.class);

  @Bean(destroyMethod = "close")
  SafeHttpClient webhookHttpClient(WebhookProperties properties) {
    if (properties.allowPrivateNetworks()) {
      log.warn(
          "Webhooks may go to private addresses (studiomanager.webhooks.allow-private-networks). "
              + "Never set this in a deployed environment.");
    }
    return new SafeHttpClient("StudioManager-Webhooks/1", properties.allowPrivateNetworks());
  }

  @Bean
  WebhookSecrets webhookSecrets(WebhookProperties properties) {
    return new WebhookSecrets(properties.secretKey());
  }
}
