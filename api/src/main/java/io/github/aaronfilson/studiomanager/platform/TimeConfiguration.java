package io.github.aaronfilson.studiomanager.platform;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The application's one clock. Tests replace it with one they can set (plan 20, section 3).
 *
 * <p>It ticks in microseconds because PostgreSQL stores microseconds: a time the API returns is
 * then the same time it reads back later.
 */
@Configuration(proxyBeanMethods = false)
class TimeConfiguration {

    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }

}
