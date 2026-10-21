package io.github.aaronfilson.studiomanager.platform;

/** The requested thing does not exist, or is not visible to the caller. Becomes a 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

}
