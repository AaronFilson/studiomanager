package io.github.aaronfilson.studiomanager.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivityControllerTests {

  @Test
  void withStreamingOffItReturnsNothingEvenIfOldTotalsRemain() {
    // The projection is never asked: old totals would be stale
    var controller = new ActivityController(null, false);
    var body = controller.getWeeklyActivity(UUID.randomUUID(), 12).getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStreaming()).isFalse();
    assertThat(body.getWeeks()).isEmpty();
  }
}
