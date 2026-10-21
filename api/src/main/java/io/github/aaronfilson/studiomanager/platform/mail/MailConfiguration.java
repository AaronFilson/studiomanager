package io.github.aaronfilson.studiomanager.platform.mail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
class MailConfiguration {

    @Bean
    EmailSender emailSender(MailProperties properties) {
        return switch (properties.sender()) {
            case LOG -> new LoggingEmailSender(properties.from());
            case ACS -> {
                if (properties.acsEndpoint() == null || properties.acsEndpoint().toString().isBlank()) {
                    throw new IllegalStateException("studiomanager.mail.acs-endpoint is required to send with ACS");
                }
                yield new AcsEmailSender(properties.acsEndpoint(), properties.from());
            }
        };
    }

}
