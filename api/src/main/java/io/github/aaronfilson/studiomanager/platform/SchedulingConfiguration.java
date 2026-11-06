package io.github.aaronfilson.studiomanager.platform;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Background jobs run only in the web application, never in the migration job. The Docker build's
 * AOT training run turns them off ({@code studiomanager.scheduling.enabled=false}): scheduled beans
 * are always built at startup, and building them needs the database the build doesn't have.
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnBooleanProperty(name = "studiomanager.scheduling.enabled", matchIfMissing = true)
@EnableScheduling
class SchedulingConfiguration {}
