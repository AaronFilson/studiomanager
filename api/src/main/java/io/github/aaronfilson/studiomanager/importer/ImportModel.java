package io.github.aaronfilson.studiomanager.importer;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The importer's types. Rows arrive as the text that was in each mapped column, so the server
 * is the one place that decides what "$1,234.50" or "3/4/2026" means.
 */
public final class ImportModel {

    private ImportModel() {
    }

    /** A person to invite, and optionally what they owed (or were owed) in the old records. */
    public record MemberRow(int line, @Nullable String email, @Nullable String balance, @Nullable String balanceAsOf) {
    }

    /** Work an artist has on the shelf. The artist is matched by email, else by name, else created. */
    public record ConsignmentRow(int line, @Nullable String artist, @Nullable String artistEmail,
            @Nullable String description, @Nullable String retailPrice, @Nullable String quantity,
            @Nullable String artistShare, @Nullable String receivedOn) {
    }

    public enum Outcome {
        /** Something new: an invite sent, an artist or item added. */
        CREATED,
        /** Something already here was added to: an opening balance on an existing member. */
        UPDATED,
        /** Nothing to do for this row; the message says why. */
        SKIPPED,
        /** The row can't be imported as it is; the message says what to fix. */
        ERROR
    }

    public record RowResult(int line, Outcome outcome, String message) {
    }

    /**
     * {@code committed} is false for a dry run, and whenever any row has an error: an import is
     * all or nothing, so fixing the file and running it again never leaves half of it in.
     */
    public record ImportResult(boolean committed, List<RowResult> rows) {

        public long count(Outcome outcome) {
            return rows.stream().filter(row -> row.outcome() == outcome).count();
        }
    }

}
