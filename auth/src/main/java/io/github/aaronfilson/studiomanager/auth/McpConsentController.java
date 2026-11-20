package io.github.aaronfilson.studiomanager.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * Explicit OAuth 2.1 consent screen for MCP clients ({@code GET /oauth2/consent}). Displays the
 * requesting assistant, the target studio, and each requested scope, rendered with strict CSP and
 * full HTML escaping (ADR 26).
 */
@RestController
class McpConsentController {

  static final String CONSENT_PATH = "/oauth2/consent";
  static final String CONSENT_CSS_PATH = "/oauth2/consent.css";
  private static final OAuth2TokenType STATE_TOKEN =
      new OAuth2TokenType(OAuth2ParameterNames.STATE);

  private static final Map<String, String> SCOPE_DESCRIPTIONS =
      Map.of(
          "read",
          "Read studio data (today's schedule, kilns, open firings, low supplies, maintenance, problem reports, glaze book)",
          "propose",
          "Propose changes for staff approval (purchase requests and maintenance tasks — never applied automatically)");

  private static final String STYLESHEET =
      """
      body { font-family: system-ui, -apple-system, sans-serif; background: #f8f6f2; color: #221d1a; margin: 0; padding: 2rem 1rem; }
      main { max-width: 34rem; margin: 0 auto; background: #fff; border: 1px solid #e2dcd5; border-radius: 10px; padding: 1.75rem; }
      h1 { font-size: 1.35rem; margin: 0 0 0.75rem; }
      p { line-height: 1.5; margin: 0.5rem 0; }
      dl { background: #f8f6f2; border-radius: 8px; padding: 0.75rem 1rem; margin: 1rem 0; font-size: 0.92rem; }
      dt { font-weight: 600; color: #5b5048; margin-top: 0.4rem; }
      dd { margin: 0.1rem 0 0.4rem; word-break: break-all; font-family: ui-monospace, monospace; font-size: 0.85rem; }
      fieldset { border: 1px solid #e2dcd5; border-radius: 8px; padding: 0.75rem 1rem; margin: 1rem 0; }
      legend { font-weight: 600; padding: 0 0.35rem; }
      label { display: block; margin: 0.65rem 0; cursor: pointer; }
      .scope-name { font-weight: 600; font-family: ui-monospace, monospace; }
      .scope-desc { display: block; margin-left: 1.5rem; color: #5b5048; font-size: 0.9rem; }
      .actions { display: flex; gap: 0.75rem; margin-top: 1.25rem; }
      button { border-radius: 6px; padding: 0.6rem 1.15rem; font-size: 0.95rem; font-weight: 600; cursor: pointer; }
      .approve { background: #8c3b2b; color: #fff; border: 1px solid #8c3b2b; }
      .deny { background: #fff; color: #221d1a; border: 1px solid #c9bfb5; }
      """;

  private final BridgeProperties properties;
  private final RegisteredClientRepository clients;
  private final OAuth2AuthorizationService authorizations;

  McpConsentController(
      BridgeProperties properties,
      RegisteredClientRepository clients,
      OAuth2AuthorizationService authorizations) {
    this.properties = properties;
    this.clients = clients;
    this.authorizations = authorizations;
  }

  @GetMapping(path = CONSENT_CSS_PATH, produces = "text/css;charset=UTF-8")
  ResponseEntity<String> stylesheet() {
    if (!properties.mcpSettings().enabled()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "css", StandardCharsets.UTF_8))
        .body(STYLESHEET);
  }

  @GetMapping(path = CONSENT_PATH, produces = MediaType.TEXT_HTML_VALUE)
  ResponseEntity<String> consent(
      @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
      @RequestParam(OAuth2ParameterNames.STATE) String state,
      @RequestParam(name = OAuth2ParameterNames.SCOPE, required = false) @Nullable String scope,
      Authentication authentication,
      HttpServletRequest request) {
    BridgeProperties.McpProperties mcp = properties.mcpSettings();
    if (!mcp.enabled()) {
      return ResponseEntity.notFound().build();
    }
    RegisteredClient client = clients.findByClientId(clientId);
    OAuth2Authorization pending = authorizations.findByToken(state, STATE_TOKEN);
    if (client == null
        || pending == null
        || !pending.getRegisteredClientId().equals(client.getId())
        || !pending.getPrincipalName().equals(authentication.getName())) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .contentType(MediaType.TEXT_PLAIN)
          .body(
              "This authorization request is invalid or has expired. Start again from your assistant.");
    }
    OAuth2AuthorizationRequest authRequest =
        pending.getAttribute(OAuth2AuthorizationRequest.class.getName());
    if (authRequest == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .contentType(MediaType.TEXT_PLAIN)
          .body("This authorization request is incomplete. Start again from your assistant.");
    }
    McpResources.Target target;
    try {
      target =
          McpResources.validateAndResolve(
              stringParameter(
                  authRequest.getAdditionalParameters(), McpResources.RESOURCE_PARAMETER),
              stringParameter(
                  authRequest.getAdditionalParameters(), McpResources.STUDIO_ID_PARAMETER),
              mcp.effectiveAllowedResources());
    } catch (OAuth2AuthenticationException invalid) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .contentType(MediaType.TEXT_PLAIN)
          .body("This authorization request targets an invalid studio or resource.");
    }
    Set<String> requestedScopes = new LinkedHashSet<>();
    for (String requested : authRequest.getScopes()) {
      if (client.getScopes().contains(requested)) {
        requestedScopes.add(requested);
      }
    }
    if (requestedScopes.isEmpty() && scope != null && !scope.isBlank()) {
      for (String part : scope.strip().split("\\s+", -1)) {
        if (!part.isEmpty() && client.getScopes().contains(part)) {
          requestedScopes.add(part);
        }
      }
    }
    if (requestedScopes.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .contentType(MediaType.TEXT_PLAIN)
          .body("No valid scopes were requested.");
    }

    String csrfInput = csrfHiddenInput(request);
    String html =
        renderPage(
            client.getClientName(),
            displayUser(authentication),
            target,
            clientId,
            state,
            requestedScopes,
            csrfInput);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
        .body(html);
  }

  private static String renderPage(
      String clientName,
      String userLabel,
      McpResources.Target target,
      String clientId,
      String state,
      Set<String> scopes,
      String csrfInput) {
    String escapedClient = escape(clientName);
    String escapedUser = escape(userLabel);
    String escapedStudio = escape(target.studioId().toString());
    String escapedResource = escape(target.resource().toString());
    String escapedClientId = escape(clientId);
    String escapedState = escape(state);

    StringBuilder scopeInputs = new StringBuilder();
    for (String scope : scopes) {
      String escapedScope = escape(scope);
      String description = escape(SCOPE_DESCRIPTIONS.getOrDefault(scope, "Access scope: " + scope));
      scopeInputs
          .append("<label>")
          .append("<input type=\"checkbox\" name=\"scope\" value=\"")
          .append(escapedScope)
          .append("\" checked> ")
          .append("<span class=\"scope-name\">")
          .append(escapedScope)
          .append("</span>")
          .append("<span class=\"scope-desc\">")
          .append(description)
          .append("</span>")
          .append("</label>");
    }

    return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <title>Authorize MCP access — Studio Manager</title>
      <link rel="stylesheet" href="/oauth2/consent.css">
    </head>
    <body>
      <main>
        <h1>Authorize MCP access</h1>
        <p><strong>%s</strong> wants to connect to Studio Manager on your behalf. \
    The assistant can never do more than your live role in this studio allows, and money, \
    exports, and member administration are never reachable.</p>
        <dl>
          <dt>Signed in as</dt>
          <dd>%s</dd>
          <dt>Studio</dt>
          <dd>%s</dd>
          <dt>Resource</dt>
          <dd>%s</dd>
        </dl>
        <form method="post" action="/oauth2/authorize">
          <input type="hidden" name="client_id" value="%s">
          <input type="hidden" name="state" value="%s">
          %s
          <fieldset>
            <legend>Requested permissions</legend>
            %s
          </fieldset>
          <div class="actions">
            <button type="submit" class="approve">Approve access</button>
          </div>
        </form>
        <form method="post" action="/oauth2/authorize">
          <input type="hidden" name="client_id" value="%s">
          <input type="hidden" name="state" value="%s">
          %s
          <div class="actions">
            <button type="submit" class="deny">Deny</button>
          </div>
        </form>
      </main>
    </body>
    </html>
    """
        .formatted(
            escapedClient,
            escapedUser,
            escapedStudio,
            escapedResource,
            escapedClientId,
            escapedState,
            csrfInput,
            scopeInputs,
            escapedClientId,
            escapedState,
            csrfInput);
  }

  private static String csrfHiddenInput(HttpServletRequest request) {
    if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
      return "<input type=\"hidden\" name=\""
          + escape(token.getParameterName())
          + "\" value=\""
          + escape(token.getToken())
          + "\">";
    }
    return "";
  }

  private static String displayUser(Authentication authentication) {
    if (authentication.getPrincipal() instanceof OAuth2User user) {
      Object name = user.getAttributes().get(StandardClaims.NAME);
      Object email = user.getAttributes().get(StandardClaims.EMAIL);
      if (name instanceof String n && !n.isBlank() && email instanceof String e && !e.isBlank()) {
        return n + " (" + e + ")";
      }
      if (name instanceof String n && !n.isBlank()) {
        return n;
      }
    }
    return authentication.getName();
  }

  static @Nullable String stringParameter(Map<String, Object> parameters, String name) {
    return parameters.get(name) instanceof String value ? value : null;
  }

  private static String escape(String value) {
    return HtmlUtils.htmlEscape(value);
  }
}
