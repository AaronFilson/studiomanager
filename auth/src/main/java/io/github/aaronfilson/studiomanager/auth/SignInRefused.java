package io.github.aaronfilson.studiomanager.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Ends a GitHub sign-in that failed, with a fixed message: a bad or missing state, a GitHub error,
 * or an account without a verified email. Nothing from the request is echoed back or logged, and
 * nothing goes back to Entra, so no code is issued.
 */
final class SignInRefused implements AuthenticationFailureHandler {

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.TEXT_PLAIN_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write(message(exception));
  }

  private static String message(AuthenticationException exception) {
    if (exception instanceof OAuth2AuthenticationException oauth
        && GitHubUserService.UNVERIFIED_EMAIL.equals(oauth.getError().getErrorCode())) {
      return "GitHub sign-in needs a verified primary email address on your GitHub account."
          + " Verify one at https://github.com/settings/emails, then sign in again.";
    }
    return "GitHub sign-in didn't complete. Go back to Studio Manager and sign in again.";
  }
}
