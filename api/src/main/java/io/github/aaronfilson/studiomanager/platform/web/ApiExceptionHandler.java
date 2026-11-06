package io.github.aaronfilson.studiomanager.platform.web;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.ProblemType;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns errors into RFC 9457 problem details. The base class covers Spring MVC's own exceptions
 * (validation, malformed JSON, type mismatches); the handlers here add the application's.
 *
 * <p>Every problem carries a {@code type} URI and a {@code code} from {@link ProblemType}, so a
 * client can tell a stale-version 409 from a duplicate 409 without reading the wording.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler
  ProblemDetail invalidInput(InvalidInputException ex) {
    return problem(HttpStatus.BAD_REQUEST, ProblemType.INVALID_INPUT, ex.getMessage());
  }

  @ExceptionHandler
  ProblemDetail notFound(NotFoundException ex) {
    return problem(HttpStatus.NOT_FOUND, ProblemType.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler
  ProblemDetail conflict(ConflictException ex) {
    return problem(HttpStatus.CONFLICT, ex.problemType(), ex.getMessage());
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail optimisticLock() {
    return problem(
        HttpStatus.CONFLICT,
        ProblemType.STALE_VERSION,
        "Someone else changed this at the same time. Reload and try again.");
  }

  /** Gives Spring MVC's own problems (bad JSON, failed validation, …) a type and code too. */
  @Override
  protected @Nullable ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    @Nullable ResponseEntity<Object> response =
        super.handleExceptionInternal(ex, body, headers, statusCode, request);
    if (response != null && response.getBody() instanceof ProblemDetail problemDetail) {
      ProblemType problemType =
          statusCode.value() == HttpStatus.BAD_REQUEST.value()
              ? ProblemType.INVALID_INPUT
              : ProblemType.OTHER;
      problemDetail.setType(problemType.uri());
      problemDetail.setProperty(ProblemType.CODE_PROPERTY, problemType.code());
    }
    return response;
  }

  private static ProblemDetail problem(
      HttpStatus status, ProblemType problemType, @Nullable String detail) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setType(problemType.uri());
    problemDetail.setTitle(problemType.title());
    problemDetail.setProperty(ProblemType.CODE_PROPERTY, problemType.code());
    return problemDetail;
  }
}
