package io.github.aaronfilson.studiomanager.platform.measure;

import java.math.BigDecimal;

/** Units a weight can be entered or priced in. */
public enum WeightUnit {

    GRAM("g", new BigDecimal("1")),
    KILOGRAM("kg", new BigDecimal("1000")),
    POUND("lb", new BigDecimal("453.59237")),
    OUNCE("oz", new BigDecimal("28.349523125"));

    private final String symbol;
    private final BigDecimal grams;

    WeightUnit(String symbol, BigDecimal grams) {
        this.symbol = symbol;
        this.grams = grams;
    }

    public String symbol() {
        return symbol;
    }

    /** How many grams one of this unit is. Exact: the pound is defined as 453.59237 g. */
    BigDecimal grams() {
        return grams;
    }

}
