package io.github.aaronfilson.studiomanager.platform;

import java.sql.SQLException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

/**
 * Tells a broken unique constraint apart from the other integrity errors, whichever way Spring
 * translated it.
 *
 * <p>{@code JdbcClient} reports one as a {@link DuplicateKeyException}. A Hibernate flush reports a
 * {@link DataIntegrityViolationException}, with PostgreSQL's SQLState {@code 23505} on the {@link
 * SQLException} underneath. A CHECK or foreign-key failure is a bug, not a duplicate, so it must
 * not be reported to a person as "that name is taken".
 */
public final class UniqueViolations {

  /** PostgreSQL's {@code unique_violation}. */
  private static final String UNIQUE_VIOLATION = "23505";

  private UniqueViolations() {}

  public static boolean isUniqueViolation(DataIntegrityViolationException exception) {
    if (exception instanceof DuplicateKeyException) {
      return true;
    }
    for (@Nullable Throwable cause = exception.getCause();
        cause != null;
        cause = cause.getCause()) {
      if (cause instanceof SQLException sqlException
          && UNIQUE_VIOLATION.equals(sqlException.getSQLState())) {
        return true;
      }
    }
    return false;
  }
}
