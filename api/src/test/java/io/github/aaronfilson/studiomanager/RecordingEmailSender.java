package io.github.aaronfilson.studiomanager;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.aaronfilson.studiomanager.platform.mail.Email;
import io.github.aaronfilson.studiomanager.platform.mail.EmailSender;

/**
 * Keeps sent emails in memory for tests to assert on. Tests share one context, so they look up
 * emails by their own (unique) recipients rather than clearing the list.
 */
public class RecordingEmailSender implements EmailSender {

    private final List<Email> sent = new CopyOnWriteArrayList<>();
    private final Set<String> failNext = ConcurrentHashMap.newKeySet();

    @Override
    public void send(Email email) {
        if (failNext.remove(email.to())) {
            throw new IllegalStateException("The provider is down (simulated)");
        }
        sent.add(email);
    }

    /** The emails sent to {@code address}, oldest first. */
    public List<Email> sentTo(String address) {
        return sent.stream().filter(email -> email.to().equals(address)).toList();
    }

    /** Makes the next send to {@code address} throw, as a provider outage would. */
    public void failNextSendTo(String address) {
        failNext.add(address);
    }

}
