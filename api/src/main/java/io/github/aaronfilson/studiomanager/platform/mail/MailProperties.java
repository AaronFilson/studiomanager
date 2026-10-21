package io.github.aaronfilson.studiomanager.platform.mail;

import java.net.URI;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param sender who delivers email
 * @param from the sender address, which must belong to a domain the provider has verified
 * @param acsEndpoint the Communication Services resource, such as
 *     {@code https://acs-studiomanager-dev.unitedstates.communication.azure.com}; required for {@code acs}
 * @param webOrigin the web app's origin, for links in emails
 */
@ConfigurationProperties("studiomanager.mail")
public record MailProperties(Sender sender, String from, @Nullable URI acsEndpoint, URI webOrigin) {

    public enum Sender {
        /** Writes emails to the log. For local work and tests. */
        LOG,
        /** Azure Communication Services. */
        ACS
    }

}
