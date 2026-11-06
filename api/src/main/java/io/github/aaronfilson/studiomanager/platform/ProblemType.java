package io.github.aaronfilson.studiomanager.platform;

import java.net.URI;

/**
 * The kinds of error the API reports. Each becomes a problem's {@code type} URI ({@code
 * /problems/<code>}) and its {@code code} property, so web and mobile can tell one 409 from another
 * without matching the wording, and the wording can change safely.
 *
 * <p>The contract lists the same codes in the {@code ProblemCode} schema.
 */
public enum ProblemType {
  /** Well-formed but not acceptable input, or a request Spring MVC couldn't read. 400. */
  INVALID_INPUT("invalid-input", "Invalid input"),
  /** No valid access token. 401. */
  UNAUTHENTICATED("unauthenticated", "Sign-in required"),
  /** The caller may not do this. 403. */
  FORBIDDEN("forbidden", "Not allowed"),
  /** No such thing, or not one the caller may see. 404. */
  NOT_FOUND("not-found", "Not found"),
  /** Someone else changed it since the caller last read it. Reload and try again. 409. */
  STALE_VERSION("stale-version", "Changed by someone else"),
  /** The same thing already exists. 409. */
  DUPLICATE("duplicate", "Already exists"),
  /** The request clashes with the current state, other than the two above. 409. */
  CONFLICT("conflict", "Conflict"),
  /** Any other error, including Spring MVC's own (an unsupported method, for example). */
  OTHER("other", "Error");

  /** The name of the extension property that carries {@link #code()}. */
  public static final String CODE_PROPERTY = "code";

  private final String code;
  private final String title;

  ProblemType(String code, String title) {
    this.code = code;
    this.title = title;
  }

  /** The machine-readable code, for example {@code stale-version}. */
  public String code() {
    return code;
  }

  /** A short, fixed summary of the kind of problem. */
  public String title() {
    return title;
  }

  /** The problem's {@code type}, a URI reference such as {@code /problems/stale-version}. */
  public URI uri() {
    return URI.create("/problems/" + code);
  }
}
