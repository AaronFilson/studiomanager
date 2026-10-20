package io.github.aaronfilson.studiomanager.platform;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Background jobs run only in the web application, never in the migration job. */
@Configuration
@ConditionalOnWebApplication
@EnableScheduling
class SchedulingConfiguration {
}
