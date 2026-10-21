package io.github.aaronfilson.studiomanager.platform.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Writes each email to the log instead of sending it, so local work needs no provider. */
class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    private final String from;

    LoggingEmailSender(String from) {
        this.from = from;
    }

    @Override
    public void send(Email email) {
        log.info("Email from {} to {}\nSubject: {}\n\n{}", from, email.to(), email.subject(), email.text());
    }

}
