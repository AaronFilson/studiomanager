package io.github.aaronfilson.studiomanager.importer;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Pattern;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;

import org.jspecify.annotations.Nullable;

/**
 * Reads the values people type into spreadsheets: "$1,234.50", "(12.00)", "60%", "3/4/2026".
 * Each method returns null for a blank cell and throws {@link InvalidInputException} with a
 * message fit to show beside the row.
 */
final class ImportValues {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    // Uuuu with STRICT: 2/30/2026 is an error, not 2 March
    private static final DateTimeFormatter MONTH_FIRST = DateTimeFormatter.ofPattern("M/d/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DAY_FIRST = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private ImportValues() {
    }

    static @Nullable String text(@Nullable String cell) {
        return cell == null || cell.isBlank() ? null : cell.strip();
    }

    static @Nullable String email(@Nullable String cell) {
        String value = text(cell);
        if (value == null) {
            return null;
        }
        if (value.length() > 254 || !EMAIL.matcher(value).matches()) {
            throw new InvalidInputException("'%s' isn't an email address.".formatted(value));
        }
        return value.toLowerCase(Locale.ROOT);
    }

    /**
     * A sum of money. Accepts a currency symbol or code, thousands separators, a leading minus
     * or accounting parentheses for a negative. Doesn't guess at "1.234,50": a comma is a
     * thousands separator.
     */
    static @Nullable BigDecimal money(@Nullable String cell) {
        String value = text(cell);
        if (value == null) {
            return null;
        }
        boolean negative = false;
        String digits = value;
        if (digits.startsWith("(") && digits.endsWith(")")) {
            negative = true;
            digits = digits.substring(1, digits.length() - 1);
        }
        digits = digits.replaceAll("[\\s,$£€¥]|[A-Z]{3}", "");
        if (digits.startsWith("-")) {
            negative = !negative;
            digits = digits.substring(1);
        }
        if (!digits.matches("\\d+(\\.\\d+)?|\\.\\d+")) {
            throw new InvalidInputException("'%s' isn't an amount of money.".formatted(value));
        }
        BigDecimal amount = new BigDecimal(digits);
        return negative ? amount.negate() : amount;
    }

    /** A fraction: "0.6", "60%" or "60" all mean 60%. */
    static @Nullable BigDecimal share(@Nullable String cell) {
        String value = text(cell);
        if (value == null) {
            return null;
        }
        boolean percent = value.endsWith("%");
        String digits = percent ? value.substring(0, value.length() - 1).strip() : value;
        BigDecimal number;
        try {
            number = new BigDecimal(digits);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("'%s' isn't a share. Write 60%% or 0.6.".formatted(value));
        }
        if (percent || number.compareTo(BigDecimal.ONE) > 0) {
            number = number.movePointLeft(2);
        }
        return number.stripTrailingZeros();
    }

    static @Nullable Integer wholeNumber(@Nullable String cell) {
        String value = text(cell);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "")).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new InvalidInputException("'%s' isn't a whole number.".formatted(value));
        }
    }

    /**
     * A date as ISO (2026-03-04), or with slashes read the way the studio's country writes them:
     * month first in the US, day first elsewhere.
     */
    static @Nullable LocalDate date(@Nullable String cell, String country) {
        String value = text(cell);
        if (value == null) {
            return null;
        }
        try {
            if (value.contains("/")) {
                return LocalDate.parse(value, "US".equals(country) ? MONTH_FIRST : DAY_FIRST);
            }
            return LocalDate.parse(value);
        } catch (DateTimeException e) {
            throw new InvalidInputException("'%s' isn't a date. Write %s or 2026-03-04."
                    .formatted(value, "US".equals(country) ? "3/4/2026" : "4/3/2026"));
        }
    }

}
