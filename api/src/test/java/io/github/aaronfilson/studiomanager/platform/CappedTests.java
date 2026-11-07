package io.github.aaronfilson.studiomanager.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CappedTests {

  @Test
  void asksForOneRowMoreThanTheLimit() {
    assertThat(Capped.fetchSize(500)).isEqualTo(501);
  }

  @Test
  void isNotTruncatedWhenEverythingFits() {
    Capped<String> capped = Capped.fromFetched(List.of("a", "b", "c"), 3);

    assertThat(capped.items()).containsExactly("a", "b", "c");
    assertThat(capped.truncated()).isFalse();
    assertThat(capped.limit()).isEqualTo(3);
  }

  @Test
  void dropsTheExtraRowAndSaysSo() {
    Capped<String> capped = Capped.fromFetched(List.of("a", "b", "c", "d"), 3);

    assertThat(capped.items()).containsExactly("a", "b", "c");
    assertThat(capped.truncated()).isTrue();
  }

  @Test
  void keepsTheCutWhenRowsAreTransformed() {
    Capped<String> capped = Capped.fromFetched(List.of("a", "b", "c"), 2);

    Capped<String> upper = capped.map(String::toUpperCase);

    assertThat(upper.items()).containsExactly("A", "B");
    assertThat(upper.truncated()).isTrue();
    assertThat(upper.limit()).isEqualTo(2);
  }
}
