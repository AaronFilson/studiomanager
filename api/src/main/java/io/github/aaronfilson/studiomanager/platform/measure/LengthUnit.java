package io.github.aaronfilson.studiomanager.platform.measure;

import java.math.BigDecimal;

/** Units a length can be entered or priced in. */
public enum LengthUnit {

    MILLIMETRE("mm", new BigDecimal("1")),
    CENTIMETRE("cm", new BigDecimal("10")),
    INCH("in", new BigDecimal("25.4"));

    private final String symbol;
    private final BigDecimal millimetres;

    LengthUnit(String symbol, BigDecimal millimetres) {
        this.symbol = symbol;
        this.millimetres = millimetres;
    }

    public String symbol() {
        return symbol;
    }

    /** How many millimetres one of this unit is. Exact: the inch is defined as 25.4 mm. */
    BigDecimal millimetres() {
        return millimetres;
    }

}
