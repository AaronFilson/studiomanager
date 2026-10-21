package io.github.aaronfilson.studiomanager;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the email sender with one that records instead of sending. */
@TestConfiguration(proxyBeanMethods = false)
public class TestEmailConfiguration {

    @Bean
    @Primary
    RecordingEmailSender recordingEmailSender() {
        return new RecordingEmailSender();
    }

}
