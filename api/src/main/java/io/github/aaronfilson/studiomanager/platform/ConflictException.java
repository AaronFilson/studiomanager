package io.github.aaronfilson.studiomanager.platform;

/**
 * The request clashes with the current state. Becomes a 409 whose problem type says which kind of
 * clash: a concurrent change, a duplicate, or anything else.
 */
public class ConflictException extends RuntimeException {

  private final ProblemType problemType;

  /** A clash with the current state that is neither a concurrent change nor a duplicate. */
  public ConflictException(String message) {
    this(ProblemType.CONFLICT, message);
  }

  private ConflictException(ProblemType problemType, String message) {
    super(message);
    this.problemType = problemType;
  }

  /** Someone else changed it since the caller read it. */
  public static ConflictException staleVersion(String message) {
    return new ConflictException(ProblemType.STALE_VERSION, message);
  }

  /** The same thing already exists. */
  public static ConflictException duplicate(String message) {
    return new ConflictException(ProblemType.DUPLICATE, message);
  }

  public ProblemType problemType() {
    return problemType;
  }
}
