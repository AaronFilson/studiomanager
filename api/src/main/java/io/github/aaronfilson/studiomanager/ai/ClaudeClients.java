package io.github.aaronfilson.studiomanager.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds a Claude client for one call with a studio's own key. Each studio pays for its own use
 * under its own Anthropic account, so there's no shared, platform-wide key. The base URL is
 * configurable so tests can point it at a local stub.
 */
@Component
public class ClaudeClients {

  /**
   * The model for reading invoices: the current default, and the user's choice can override it
   * later.
   */
  public static final String MODEL = "claude-opus-5-5";

  private final String baseUrl;

  ClaudeClients(
      @Value("${studiomanager.ai.anthropic-base-url:https://api.anthropic.com}") String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * A client for reading one invoice with the studio's key. The SDK retries a 408, 409, 429 or 5xx
   * itself, with exponential backoff, up to three times before the reader gives up.
   */
  public AnthropicClient forReading(AiKeys.StudioKey key) {
    return AnthropicOkHttpClient.builder()
        .apiKey(key.secret())
        .baseUrl(baseUrl)
        .timeout(Duration.ofMinutes(3))
        .maxRetries(3)
        .build();
  }

  AnthropicClient forKey(String apiKey) {
    return AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .timeout(Duration.ofMinutes(2))
        .maxRetries(1)
        .build();
  }
}
