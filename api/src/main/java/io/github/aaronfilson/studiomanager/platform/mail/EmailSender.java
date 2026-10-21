package io.github.aaronfilson.studiomanager.platform.mail;

/**
 * Hands an email to the provider. Call it from an {@code @ApplicationModuleListener}, never from
 * a request's own transaction: the listener runs after commit and is retried through the event
 * publication registry, so a send that throws is attempted again later.
 *
 * <p>Delivery is at least once. A send that reached the provider but threw afterwards (a timeout
 * on the response, say) is retried and can arrive twice.
 */
@FunctionalInterface
public interface EmailSender {

    /** Sends {@code email}, or throws if the provider didn't accept it. */
    void send(Email email);

}
