package io.github.aaronfilson.studiomanager.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class McpOAuthPropertiesTests {

  private static final UUID STUDIO_A = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID STUDIO_B = UUID.fromString("22222222-2222-4222-8222-222222222222");

  @Test
  void blankIssuerDisablesMcpOAuth() {
    assertThat(new McpOAuthProperties(null, null, null).enabled()).isFalse();
    assertThat(new McpOAuthProperties("   ", null, null).enabled()).isFalse();
  }

  @Test
  void enabledIssuerRequiresValidAbsoluteHttpAudience() {
    assertThatThrownBy(() -> new McpOAuthProperties("https://auth.example", null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("audience");
    assertThatThrownBy(() -> new McpOAuthProperties("https://auth.example", null, "   "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("audience");
    assertThatThrownBy(() -> new McpOAuthProperties("https://auth.example", null, "/mcp"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new McpOAuthProperties("https://auth.example", null, "urn:mcp:api"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> new McpOAuthProperties("https://auth.example", null, "https://api.example/mcp?x=1"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new McpOAuthProperties("https://auth.example", null, "https://api.example/mcp#frag"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void derivesDefaultJwkSetUriAndStudioUrlsStrictlyFromConfiguredAudience() {
    McpOAuthProperties props =
        new McpOAuthProperties(
            "https://auth.studiomanager.example/", "", "https://api.studiomanager.example/mcp");

    assertThat(props.enabled()).isTrue();
    assertThat(props.effectiveJwkSetUri())
        .isEqualTo("https://auth.studiomanager.example/oauth2/jwks");
    assertThat(props.rootResource()).isEqualTo("https://api.studiomanager.example/mcp");
    assertThat(props.resourceForStudio(STUDIO_A))
        .isEqualTo("https://api.studiomanager.example/api/v1/studios/" + STUDIO_A + "/mcp");
    assertThat(props.metadataUrlForStudio(STUDIO_A))
        .isEqualTo(
            "https://api.studiomanager.example/.well-known/oauth-protected-resource/api/v1/studios/"
                + STUDIO_A
                + "/mcp");
  }

  @Test
  void acceptsConfiguredAudienceOrMatchingStudioResourceOnly() {
    McpOAuthProperties props =
        new McpOAuthProperties(
            "https://auth.studiomanager.example",
            "https://auth.studiomanager.example/oauth2/jwks",
            "https://api.studiomanager.example/mcp");

    assertThat(props.acceptsAudience(List.of("https://api.studiomanager.example/mcp"), STUDIO_A))
        .isTrue();
    assertThat(
            props.acceptsAudience(
                List.of("https://api.studiomanager.example/api/v1/studios/" + STUDIO_A + "/mcp"),
                STUDIO_A))
        .isTrue();
    assertThat(
            props.acceptsAudience(
                List.of("https://api.studiomanager.example/api/v1/studios/" + STUDIO_B + "/mcp"),
                STUDIO_A))
        .isFalse();
    assertThat(props.acceptsAudience(List.of("https://evil.example/mcp"), STUDIO_A)).isFalse();
    assertThat(props.acceptsAudience(List.of(), STUDIO_A)).isFalse();
    assertThat(props.acceptsAudience(null, STUDIO_A)).isFalse();
  }
}
