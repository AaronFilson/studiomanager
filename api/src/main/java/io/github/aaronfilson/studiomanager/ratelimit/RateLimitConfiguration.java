package io.github.aaronfilson.studiomanager.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
class RateLimitConfiguration {

  /**
   * Limits from configuration, until another {@link RateLimitSettings} is declared. A
   * {@code @Component} or {@code @Service} implementation (the admin module's live settings) is
   * found by component scanning before this method's condition is checked, so it replaces this one.
   */
  @Bean
  @ConditionalOnMissingBean(RateLimitSettings.class)
  RateLimitSettings configuredRateLimitSettings(RateLimitProperties properties) {
    return new ConfiguredRateLimitSettings(properties);
  }
}
