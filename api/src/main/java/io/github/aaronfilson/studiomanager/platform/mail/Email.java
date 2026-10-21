package io.github.aaronfilson.studiomanager.platform.mail;

import java.util.Objects;

/**
 * One plain-text email to one person.
 *
 * @param to the recipient's address
 * @param subject the subject line
 * @param text the body, as plain text
 */
public record Email(String to, String subject, String text) {

    public Email {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(text, "text");
    }

}
