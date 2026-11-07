package io.github.aaronfilson.studiomanager.invoice;

/**
 * An uploaded invoice, as a reader receives it: only the file, with no member data
 * (docs/planning/16, "Data minimization").
 *
 * <p>A carrier, never compared or used as a key, so the array's reference equality doesn't matter.
 */
@SuppressWarnings("ArrayRecordComponent")
public record InvoiceFile(byte[] content, InvoiceFileType type) {}
