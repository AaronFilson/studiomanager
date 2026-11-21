package io.github.aaronfilson.studiomanager.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
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
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Caps RFC 7591 dynamic client registrations per minute on this replica (both per-IP and globally)
 * and enforces a 16 KB request body cap before JSON deserialization.
 */
final class RegisterRateLimit extends OncePerRequestFilter {

  static final String REGISTER_PATH = "/oauth2/register";
  static final int DEFAULT_PER_IP_REQUESTS_PER_MINUTE = 5;
  static final int MAX_BODY_BYTES = 16 * 1024;
  private static final int MAX_TRACKED_IPS = 4_096;

  private final Bucket globalBucket;
  private final int perIpRequestsPerMinute;
  private final Object ipBucketsLock = new Object();
  private final Map<String, Bucket> bucketsByIp =
      new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
          return size() > MAX_TRACKED_IPS;
        }
      };

  RegisterRateLimit(int requestsPerMinute) {
    this(requestsPerMinute, Math.min(requestsPerMinute, DEFAULT_PER_IP_REQUESTS_PER_MINUTE));
  }

  RegisterRateLimit(int globalRequestsPerMinute, int perIpRequestsPerMinute) {
    if (globalRequestsPerMinute < 1) {
      throw new IllegalStateException(
          "studiomanager.bridge.mcp.register-requests-per-minute must be at least 1.");
    }
    if (perIpRequestsPerMinute < 1) {
      throw new IllegalStateException(
          "studiomanager.bridge.mcp.register-requests-per-minute-per-ip must be at least 1.");
    }
    this.globalBucket = newBucket(globalRequestsPerMinute);
    this.perIpRequestsPerMinute = Math.min(globalRequestsPerMinute, perIpRequestsPerMinute);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return !REGISTER_PATH.equals(path);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    // Check per-IP bucket first so one noisy caller cannot exhaust the global ceiling
    if (!bucketForIp(clientIp(request)).tryConsume(1) || !globalBucket.tryConsume(1)) {
      response.setHeader("Retry-After", "60");
      response.sendError(
          HttpStatus.TOO_MANY_REQUESTS.value(), "Too many client registrations; try again soon.");
      return;
    }

    if (request.getContentLengthLong() > MAX_BODY_BYTES) {
      writeTooLarge(response);
      return;
    }

    try {
      chain.doFilter(new LimitedRequest(request), response);
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

  private Bucket bucketForIp(String ip) {
    synchronized (ipBucketsLock) {
      return bucketsByIp.computeIfAbsent(ip, key -> newBucket(perIpRequestsPerMinute));
    }
  }

  private static Bucket newBucket(int requestsPerMinute) {
    return Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build())
        .build();
  }

  private static String clientIp(HttpServletRequest request) {
    @Nullable String remoteAddr = request.getRemoteAddr();
    return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr.strip();
  }

  private static boolean isTooLarge(Throwable error) {
    for (@Nullable Throwable cur = error; cur != null; cur = cur.getCause()) {
      if (cur instanceof BodyTooLargeException) {
        return true;
      }
    }
    return false;
  }

  private static void writeTooLarge(HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            "{\"error\":\"invalid_client_metadata\","
                + "\"error_description\":\"Registration request body exceeds the 16 KB limit.\"}");
  }

  private static final class BodyTooLargeException extends RuntimeException {}

  private static final class LimitedRequest extends HttpServletRequestWrapper {

    private @Nullable LimitedStream stream;
    private @Nullable BufferedReader reader;

    LimitedRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (stream == null) {
        stream = new LimitedStream(super.getInputStream());
      }
      return stream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      if (reader == null) {
        Charset charset = StandardCharsets.UTF_8;
        @Nullable String encoding = getCharacterEncoding();
        if (encoding != null && !encoding.isBlank()) {
          try {
            charset = Charset.forName(encoding);
          } catch (IllegalArgumentException ignored) {
            charset = StandardCharsets.UTF_8;
          }
        }
        reader = new BufferedReader(new InputStreamReader(getInputStream(), charset));
      }
      return reader;
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
      if (readBytes > MAX_BODY_BYTES) {
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
