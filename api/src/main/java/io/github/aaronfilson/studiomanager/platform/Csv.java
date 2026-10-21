package io.github.aaronfilson.studiomanager.platform;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Writes RFC 4180 CSV, safe to open in a spreadsheet.
 *
 * <p>Text that members typed can start with {@code =}, {@code +}, {@code -} or {@code @}, which
 * Excel and Google Sheets run as a formula. Such text cells get a leading apostrophe, as OWASP
 * recommends for CSV exports. Numbers and dates are written as they are, so {@code -5} stays a
 * number.
 */
public final class Csv {

    private final StringBuilder out = new StringBuilder();

    /** Appends one row. Each cell is text (guarded) unless it is a {@link Number}; null is empty. */
    public Csv row(List<?> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            Object cell = cells.get(i);
            out.append(cell instanceof Number number ? number.toString() : text(cell == null ? null : cell.toString()));
        }
        out.append("\r\n");
        return this;
    }

    @Override
    public String toString() {
        return out.toString();
    }

    /** One text cell: guarded against formulas, then quoted if it needs to be. */
    public static String text(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String safe = "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return '"' + safe.replace("\"", "\"\"") + '"';
        }
        return safe;
    }

}
