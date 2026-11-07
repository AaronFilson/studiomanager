package io.github.aaronfilson.studiomanager.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Turns a GitHub sign-in into the bridge's user: reads {@code GET /user} and {@code GET
 * /user/emails} with GitHub's access token, then forgets the token. Refuses anyone without a
 * verified primary email, because invites are matched by email (plan 18).
 *
 * <p>The subject is {@code github:<numeric id>}, never the login name, which people can change.
 */
@Component
public class GitHubUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  /** The error code the sign-in failure page reports for a missing verified email. */
  public static final String UNVERIFIED_EMAIL = "unverified_email";

  public static final String INVALID_USER = "invalid_github_user";

  private final OAuth2UserService<OAuth2UserRequest, OAuth2User> profileReader;
  private final RestClient github;
  private final BridgeProperties properties;

  public GitHubUserService(BridgeProperties properties) {
    this.profileReader = new DefaultOAuth2UserService();
    this.github = RestClient.create();
    this.properties = properties;
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest request) {
    OAuth2User profile = profileReader.loadUser(request);
    if (profile == null || !(profile.getAttributes().get("id") instanceof Number numericId)) {
      throw refusal(INVALID_USER, "GitHub returned a user without a numeric id.");
    }
    String email =
        verifiedPrimaryEmail(request.getAccessToken().getTokenValue())
            .orElseThrow(() ->
                refusal(
                    UNVERIFIED_EMAIL, "The GitHub account has no verified primary email address."));

    Map<String, Object> claims = new HashMap<>();
    claims.put(StandardClaims.SUBJECT, "github:" + numericId.longValue());
    claims.put(StandardClaims.EMAIL, email);
    claims.put(StandardClaims.NAME, displayName(profile.getAttributes()));
    if (profile.getAttributes().get("avatar_url") instanceof String avatar) {
      claims.put(StandardClaims.PICTURE, avatar);
    }
    return new DefaultOAuth2User(
        AuthorityUtils.createAuthorityList("ROLE_USER"), claims, StandardClaims.SUBJECT);
  }

  private Optional<String> verifiedPrimaryEmail(String accessToken) {
    List<GitHubEmail> emails;
    try {
      emails =
          github
              .get()
              .uri(properties.githubEmailsUri())
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
              .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
              .retrieve()
              .body(new ParameterizedTypeReference<List<GitHubEmail>>() {});
    } catch (RestClientException exception) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(INVALID_USER, "GitHub's email list couldn't be read.", null), exception);
    }
    if (emails == null) {
      return Optional.empty();
    }
    return emails.stream()
        .filter(candidate -> candidate.primary() && candidate.verified())
        .map(GitHubEmail::email)
        .filter(address -> address != null && !address.isBlank())
        .findFirst();
  }

  private static String displayName(Map<String, Object> attributes) {
    if (attributes.get("name") instanceof String name && !name.isBlank()) {
      return name;
    }
    if (attributes.get("login") instanceof String login) {
      return login;
    }
    return "";
  }

  private static OAuth2AuthenticationException refusal(String code, String description) {
    return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
  }

  /** One entry of GitHub's {@code /user/emails}. */
  record GitHubEmail(@Nullable String email, boolean primary, boolean verified) {}
}
