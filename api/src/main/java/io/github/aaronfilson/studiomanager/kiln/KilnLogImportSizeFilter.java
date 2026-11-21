package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.platform.ProblemType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Enforces the 2.5 MB request-body cap on {@code POST
 * /api/v1/studios/{studioId}/kilns/{kilnId}/log-imports} before Jackson buffers the payload (2 MB
 * CSV plus JSON envelope overhead), returning RFC 9457 {@code 413 Payload Too Large}.
 */
@Component
class KilnLogImportSizeFilter extends OncePerRequestFilter {

  /** 2.5 MB raw HTTP body cap (2 MB CSV plus JSON envelope and escaping overhead). */
  static final long MAX_REQUEST_BYTES = 2_621_440L;

  private static final Pattern IMPORT_PATH =
      Pattern.compile("^/api/v1/studios/[^/]+/kilns/[^/]+/log-imports$");

  private final JsonMapper json;

  KilnLogImportSizeFilter(JsonMapper json) {
    this.json = json;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!"POST".equalsIgnoreCase(request.getMethod())
        || !IMPORT_PATH.matcher(pathWithinApplication(request)).matches()) {
      filterChain.doFilter(request, response);
      return;
    }

    long contentLength = request.getContentLengthLong();
    if (contentLength > MAX_REQUEST_BYTES) {
      writeTooLarge(response);
      return;
    }

    try {
      filterChain.doFilter(new LimitedRequest(request), response);
    } catch (RuntimeException | ServletException | IOException e) {
      if (isTooLarge(e)) {
        if (!response.isCommitted()) {
          writeTooLarge(response);
        }
        return;
      }
      throw e;
    }
  }

  private static String pathWithinApplication(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
      return uri.substring(contextPath.length());
    }
    return uri;
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
            HttpStatus.CONTENT_TOO_LARGE, "The kiln log request exceeds the 2.5 MB limit.");
    detail.setType(ProblemType.INVALID_INPUT.uri());
    detail.setTitle("File too large");
    detail.setProperty(ProblemType.CODE_PROPERTY, ProblemType.INVALID_INPUT.code());
    response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    json.writeValue(response.getOutputStream(), detail);
  }

  private static final class BodyTooLargeException extends RuntimeException {}

  private static final class LimitedRequest extends HttpServletRequestWrapper {

    private @Nullable LimitedStream cachedStream;
    private @Nullable BufferedReader cachedReader;

    LimitedRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      LimitedStream stream = cachedStream;
      if (stream == null) {
        stream = new LimitedStream(super.getInputStream());
        cachedStream = stream;
      }
      return stream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      BufferedReader reader = cachedReader;
      if (reader == null) {
        Charset charset = resolveCharset(getCharacterEncoding());
        reader = new BufferedReader(new InputStreamReader(getInputStream(), charset));
        cachedReader = reader;
      }
      return reader;
    }

    private static Charset resolveCharset(@Nullable String encoding) {
      if (encoding == null || encoding.isBlank()) {
        return StandardCharsets.UTF_8;
      }
      try {
        return Charset.forName(encoding);
      } catch (IllegalArgumentException ignored) {
        return StandardCharsets.UTF_8;
      }
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

    @Override
    public void close() throws IOException {
      delegate.close();
    }

    private void count(int n) {
      readBytes += n;
      if (readBytes > MAX_REQUEST_BYTES) {
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
