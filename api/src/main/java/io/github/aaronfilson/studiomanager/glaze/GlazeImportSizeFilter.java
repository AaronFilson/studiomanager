package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.ProblemType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Enforces the 2 MB payload limit on {@code POST /api/v1/studios/{studioId}/glazes/import} before
 * Jackson buffers the request body, returning RFC 9457 {@code 413 Payload Too Large}.
 */
@Component
class GlazeImportSizeFilter extends OncePerRequestFilter {

  private static final Pattern IMPORT_PATH =
      Pattern.compile("^/api/v1/studios/[^/]+/glazes/import$");

  private final JsonMapper json;

  GlazeImportSizeFilter(JsonMapper json) {
    this.json = json;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!"POST".equalsIgnoreCase(request.getMethod())
        || !IMPORT_PATH.matcher(request.getRequestURI()).matches()) {
      filterChain.doFilter(request, response);
      return;
    }

    long contentLength = request.getContentLengthLong();
    if (contentLength > GlazeImportParser.MAX_IMPORT_BYTES) {
      writeTooLarge(response);
      return;
    }

    try {
      filterChain.doFilter(new LimitedRequest(request), response);
    } catch (RuntimeException | ServletException e) {
      if (isTooLarge(e)) {
        if (!response.isCommitted()) {
          writeTooLarge(response);
        }
        return;
      }
      throw e;
    }
  }

  private static boolean isTooLarge(Throwable error) {
    for (Throwable cur = error; cur != null; cur = cur.getCause()) {
      if (cur instanceof BodyTooLargeException) {
        return true;
      }
    }
    return false;
  }

  private void writeTooLarge(HttpServletResponse response) throws IOException {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONTENT_TOO_LARGE,
            "The glaze export file exceeds the 2 MB limit.");
    detail.setType(ProblemType.INVALID_INPUT.uri());
    detail.setTitle("File too large");
    detail.setProperty(ProblemType.CODE_PROPERTY, ProblemType.INVALID_INPUT.code());
    response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    json.writeValue(response.getOutputStream(), detail);
  }

  private static final class BodyTooLargeException extends RuntimeException {}

  private static final class LimitedRequest extends HttpServletRequestWrapper {

    LimitedRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      return new LimitedStream(super.getInputStream());
    }
  }

  private static final class LimitedStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private long readBytes;

    LimitedStream(ServletInputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
      int b = delegate.read();
      if (b >= 0) {
        count(1);
      }
      return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int n = delegate.read(b, off, len);
      if (n > 0) {
        count(n);
      }
      return n;
    }

    private void count(int n) {
      readBytes += n;
      if (readBytes > GlazeImportParser.MAX_IMPORT_BYTES) {
        throw new BodyTooLargeException();
      }
    }

    @Override
    public boolean isFinished() {
      return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      delegate.setReadListener(readListener);
    }
  }
}
