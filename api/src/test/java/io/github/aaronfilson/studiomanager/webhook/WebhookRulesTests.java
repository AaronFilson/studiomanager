package io.github.aaronfilson.studiomanager.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Signing, secrets and backoff: the parts with no database. */
class WebhookRulesTests {

  @Test
  void signsLikeTheStandardWebhooksTestVector() {
    // From the Standard Webhooks spec's reference libraries
    String signature =
        WebhookSigner.sign(
            "whsec_MfKQ9r8GKYqrTwjUPD8ILPZIo2LaLaSw",
            "msg_p5jXN8AQM9LWM0D4loKWxJek",
            1614265330L,
            "{\"test\": 2432232314}");
    assertThat(signature).isEqualTo("v1,g0hM9SsE+OTPJTGt/tmIKtSyZlE3uFJELVlNIOLJ1OE=");
  }

  @Test
  void duringARotationBothSecretsSign() {
    String a = WebhookSecrets.newSecret();
    String b = WebhookSecrets.newSecret();
    var headers = WebhookSigner.headers(List.of(a, b), "evt_1", 100, "{}");
    assertThat(headers)
        .containsEntry("webhook-id", "evt_1")
        .containsEntry("webhook-timestamp", "100");
    assertThat(headers.get("webhook-signature").split(" "))
        .containsExactly(
            WebhookSigner.sign(a, "evt_1", 100, "{}"), WebhookSigner.sign(b, "evt_1", 100, "{}"));
  }

  @Test
  void secretsRoundTripAndAreBoundToTheirEndpoint() {
    WebhookSecrets secrets = new WebhookSecrets(null);
    UUID endpoint = UUID.randomUUID();
    String secret = WebhookSecrets.newSecret();
    assertThat(secret).startsWith("whsec_").hasSize(6 + 44);
    byte[] sealed = secrets.encrypt(endpoint, secret);
    assertThat(new String(sealed, java.nio.charset.StandardCharsets.ISO_8859_1))
        .doesNotContain(secret);
    assertThat(secrets.decrypt(endpoint, sealed)).isEqualTo(secret);
    // Copied onto another endpoint's row, it won't decrypt
    assertThatThrownBy(() -> secrets.decrypt(UUID.randomUUID(), sealed))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void refusesAKeyThatIsntAes256() {
    assertThatThrownBy(() -> new WebhookSecrets("c2hvcnQ="))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void waitsLongerAfterEachFailureThenGivesUp() {
    Instant now = Instant.parse("2026-11-02T12:00:00Z");
    assertThat(Backoff.nextAttempt(1, now)).hasValue(now.plus(Duration.ofMinutes(1)));
    assertThat(Backoff.nextAttempt(2, now)).hasValue(now.plus(Duration.ofMinutes(5)));
    assertThat(Backoff.nextAttempt(9, now)).hasValue(now.plus(Duration.ofHours(24)));
    assertThat(Backoff.nextAttempt(10, now)).isEmpty();
    Duration total = Backoff.WAITS.stream().reduce(Duration.ZERO, Duration::plus);
    assertThat(total).isBetween(Duration.ofDays(3), Duration.ofDays(4));
  }

  @Test
  void disablesAfterThreeDaysOrFiftyFailures() {
    Instant now = Instant.parse("2026-11-05T12:00:00Z");
    assertThat(Backoff.shouldDisable(3, now.minus(Duration.ofDays(3)), now)).isTrue();
    assertThat(Backoff.shouldDisable(3, now.minus(Duration.ofDays(2)), now)).isFalse();
    assertThat(Backoff.shouldDisable(50, now.minus(Duration.ofMinutes(5)), now)).isTrue();
    assertThat(Backoff.delivered(204)).isTrue();
    assertThat(Backoff.delivered(302)).isFalse();
  }
}
