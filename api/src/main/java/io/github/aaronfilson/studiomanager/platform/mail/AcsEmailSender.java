package io.github.aaronfilson.studiomanager.platform.mail;

import java.net.URI;
import java.time.Duration;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.core.util.polling.PollResponse;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends through Azure Communication Services as the API's managed identity, so there is no
 * connection string or key to keep. {@code DefaultAzureCredential} picks the user-assigned
 * identity from {@code AZURE_CLIENT_ID}, as the passwordless Postgres login does.
 */
class AcsEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(AcsEmailSender.class);

    private static final Duration SEND_TIMEOUT = Duration.ofMinutes(2);

    private final EmailClient client;
    private final String from;

    AcsEmailSender(URI endpoint, String from) {
        this.client = new EmailClientBuilder()
                .endpoint(endpoint.toString())
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        this.from = from;
    }

    @Override
    public void send(Email email) {
        EmailMessage message = new EmailMessage()
                .setSenderAddress(from)
                .setToRecipients(new EmailAddress(email.to()))
                .setSubject(email.subject())
                .setBodyPlainText(email.text());
        // Throws if ACS refuses the request or doesn't finish in time, so the outbox retries it
        PollResponse<EmailSendResult> response = client.beginSend(message).waitForCompletion(SEND_TIMEOUT);
        EmailSendResult result = response.getValue();
        if (result.getStatus() != EmailSendStatus.SUCCEEDED) {
            // Accepted but not delivered, such as a bad address: sending again wouldn't help
            log.warn("Email {} ended {}: {}", result.getId(), result.getStatus(),
                    result.getError() == null ? "no detail" : result.getError().getMessage());
        }
    }

}
