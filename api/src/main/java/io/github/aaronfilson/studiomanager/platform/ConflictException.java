package io.github.aaronfilson.studiomanager.platform;

/** The request clashes with the current state, for example a concurrent change. Becomes a 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

}
