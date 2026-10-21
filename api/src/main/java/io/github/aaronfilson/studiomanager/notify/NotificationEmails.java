package io.github.aaronfilson.studiomanager.notify;

import java.net.URI;
import java.util.List;

import io.github.aaronfilson.studiomanager.platform.mail.Email;
import io.github.aaronfilson.studiomanager.platform.mail.EmailSender;
import io.github.aaronfilson.studiomanager.platform.mail.MailProperties;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Emails each notification to its member. Members don't open an app to check whether their
 * pieces are out, so the inbox alone isn't enough.
 *
 * <p>Runs after the notice commits, from the event publication registry: if the send throws, the
 * publication stays incomplete and {@code OutboxRetry} tries again.
 */
@Component
class NotificationEmails {

    private static final Logger log = LoggerFactory.getLogger(NotificationEmails.class);

    private final AccountDirectory accounts;
    private final EmailSender sender;
    private final URI webOrigin;

    NotificationEmails(AccountDirectory accounts, EmailSender sender, MailProperties mail) {
        this.accounts = accounts;
        this.sender = sender;
        this.webOrigin = mail.webOrigin();
    }

    @ApplicationModuleListener
    void on(NotificationCreated notification) {
        String address = accounts.emails(List.of(notification.accountId())).get(notification.accountId());
        if (address == null) {
            // The identity provider gave no email; the notice is still in their inbox
            log.debug("No email for account {}, notification {} stays in the inbox only",
                    notification.accountId(), notification.notificationId());
            return;
        }
        sender.send(email(address, notification));
    }

    private Email email(String address, NotificationCreated notification) {
        String link = webOrigin.resolve("/studios/" + notification.studioId()).toString();
        String text = """
                %s

                See all your notices: %s

                You're getting this because you're a member of %s."""
                .formatted(notification.body(), link, notification.studioName());
        return new Email(address, notification.studioName() + ": " + notification.title(), text);
    }

}
