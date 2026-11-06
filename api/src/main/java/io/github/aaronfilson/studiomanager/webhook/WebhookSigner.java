package io.github.aaronfilson.studiomanager.webhook;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Signs a delivery the Standard Webhooks way (https://www.standardwebhooks.com/), so receivers can
 * check it with an off-the-shelf library instead of code written for us:
 *
 * <pre>
 * webhook-id:        the event id, the same on every retry, for dropping duplicates
 * webhook-timestamp: Unix seconds, for refusing replays
 * webhook-signature: v1,base64(HMAC-SHA256(key, id + "." + timestamp + "." + body))
 * </pre>
 *
 * The key is the secret's base64 part, decoded. During a rotation each secret's signature is sent,
 * separated by spaces, so a receiver with either secret accepts it.
 */
final class WebhookSigner {

  private WebhookSigner() {}

  static String sign(String secret, String id, long timestamp, String body) {
    String encoded =
        secret.startsWith(WebhookSecrets.PREFIX)
            ? secret.substring(WebhookSecrets.PREFIX.length())
            : secret;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(Base64.getDecoder().decode(encoded), "HmacSHA256"));
      byte[] digest =
          mac.doFinal((id + "." + timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
      return "v1," + Base64.getEncoder().encodeToString(digest);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HmacSHA256 is unavailable", e);
    }
  }

  /** The three headers, signed with each secret given. */
  static Map<String, String> headers(List<String> secrets, String id, long timestamp, String body) {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("webhook-id", id);
    headers.put("webhook-timestamp", Long.toString(timestamp));
    headers.put(
        "webhook-signature",
        secrets.stream()
            .map(secret -> sign(secret, id, timestamp, body))
            .collect(Collectors.joining(" ")));
    return headers;
  }
}
