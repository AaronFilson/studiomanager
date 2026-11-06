package io.github.aaronfilson.studiomanager.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Puts the token call limit in front of every API route. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
class RateLimitWebConfiguration implements WebMvcConfigurer {

  private final TokenCallLimits tokenCallLimits;

  RateLimitWebConfiguration(TokenCallLimits tokenCallLimits) {
    this.tokenCallLimits = tokenCallLimits;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(tokenCallLimits).addPathPatterns("/api/**");
  }
}
