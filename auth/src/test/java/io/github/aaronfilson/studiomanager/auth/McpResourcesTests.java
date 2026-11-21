package io.github.aaronfilson.studiomanager.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

class McpResourcesTests {

  private static final UUID STUDIO_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID OTHER_STUDIO_ID =
      UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final List<URI> ALLOWED =
      List.of(URI.create("https://api.studiomanager.test/mcp"));

  @Test
  void acceptsHttpsAndLoopbackHttpRedirectUris() {
    assertThat(McpResources.isValidRedirectUri("https://claude.ai/api/mcp/auth_callback")).isTrue();
    assertThat(McpResources.isValidRedirectUri("http://localhost:4321/callback")).isTrue();
    assertThat(McpResources.isValidRedirectUri("http://127.0.0.1:8765/cb")).isTrue();
    assertThat(McpResources.isValidRedirectUri("http://[::1]:5000/cb")).isTrue();
  }

  @Test
  void rejectsUnsafeRedirectUris() {
    assertThat(McpResources.isValidRedirectUri("http://evil.example/cb")).isFalse();
    assertThat(McpResources.isValidRedirectUri("javascript:alert(1)")).isFalse();
    assertThat(McpResources.isValidRedirectUri("https://claude.ai/cb#fragment")).isFalse();
    assertThat(McpResources.isValidRedirectUri("https://user@claude.ai/cb")).isFalse();
    assertThat(McpResources.isValidRedirectUri("/relative/path")).isFalse();
    assertThat(McpResources.isValidRedirectUri("")).isFalse();
    assertThat(McpResources.isValidRedirectUri(null)).isFalse();
  }

  @Test
  void extractsStudioIdFromPerStudioMcpResourceUrl() {
    String url = "https://api.studiomanager.test/api/v1/studios/" + STUDIO_ID + "/mcp";

    McpResources.Target target = McpResources.validateAndResolve(url, null, ALLOWED);

    assertThat(target.resource()).hasToString(url);
    assertThat(target.studioId()).isEqualTo(STUDIO_ID);
  }

  @Test
  void acceptsBaseResourceWithExplicitStudioId() {
    McpResources.Target target =
        McpResources.validateAndResolve(
            "https://api.studiomanager.test/mcp", STUDIO_ID.toString(), ALLOWED);

    assertThat(target.resource()).hasToString("https://api.studiomanager.test/mcp");
    assertThat(target.studioId()).isEqualTo(STUDIO_ID);
  }

  @Test
  void rejectsDisallowedOrMismatchedResourceTargets() {
    assertThatThrownBy(() ->
            McpResources.validateAndResolve(
                "https://evil.example/api/v1/studios/" + STUDIO_ID + "/mcp", null, ALLOWED))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
        .isEqualTo(McpResources.INVALID_TARGET);

    assertThatThrownBy(() ->
            McpResources.validateAndResolve(
                "https://api.studiomanager.test/api/v1/studios/" + STUDIO_ID + "/mcp",
                OTHER_STUDIO_ID.toString(),
                ALLOWED))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
        .isEqualTo(McpResources.INVALID_TARGET);

    assertThatThrownBy(() ->
            McpResources.validateAndResolve("https://api.studiomanager.test/mcp", null, ALLOWED))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
        .isEqualTo(McpResources.INVALID_TARGET);

    assertThatThrownBy(() ->
            McpResources.validateAndResolve(
                "https://api.studiomanager.test/mcp?x=1", STUDIO_ID.toString(), ALLOWED))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
        .isEqualTo(McpResources.INVALID_TARGET);

    // Empty allowed-resources list fails closed
    assertThatThrownBy(() ->
            McpResources.validateAndResolve(
                "https://api.studiomanager.test/api/v1/studios/" + STUDIO_ID + "/mcp",
                null,
                List.of()))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
        .isEqualTo(McpResources.INVALID_TARGET);
  }

  @Test
  void mcpPropertiesFailsFastWhenEnabledWithoutAllowedResources() {
    assertThatThrownBy(
            () -> new BridgeProperties.McpProperties(true, List.of(), null, 20, 5, null, null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("allowed-resources");
    assertThatThrownBy(
            () -> new BridgeProperties.McpProperties(true, null, null, 20, 5, null, null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("allowed-resources");
    assertThatThrownBy(
            () ->
                new BridgeProperties.McpProperties(
                    true, List.of(URI.create("/relative")), null, 20, 5, null, null, null))
        .isInstanceOf(IllegalStateException.class);

    BridgeProperties.McpProperties disabled =
        new BridgeProperties.McpProperties(false, List.of(), null, 20, 5, null, null, null);
    assertThat(disabled.enabled()).isFalse();
  }
}
