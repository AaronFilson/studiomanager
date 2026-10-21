package io.github.aaronfilson.studiomanager.platform;

/** Input that is well-formed but not acceptable, such as an unknown time zone. Becomes a 400. */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }

}
