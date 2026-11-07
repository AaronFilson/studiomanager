package io.github.aaronfilson.studiomanager.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Postgres {@code numeric(p, s)} columns, as Java sees them. */
final class Numeric {

  private Numeric() {}

  /**
   * {@code value} as a {@code numeric} column of this scale stores it. Postgres rounds a tie away
   * from zero, which is {@link RoundingMode#HALF_UP}. An entity's amounts go through this, so a
   * view built from a new entity reads the same as one built from the row loaded back.
   */
  static BigDecimal ofScale(BigDecimal value, int scale) {
    return value.setScale(scale, RoundingMode.HALF_UP);
  }
}
