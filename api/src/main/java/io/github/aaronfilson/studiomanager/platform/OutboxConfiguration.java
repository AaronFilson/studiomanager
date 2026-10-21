package io.github.aaronfilson.studiomanager.platform;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Turns on asynchronous event listeners. Without it an {@code @ApplicationModuleListener} would
 * run on the request's thread after commit, so a slow email provider would hold up the response.
 */
@Configuration
@EnableAsync
class OutboxConfiguration {
}
