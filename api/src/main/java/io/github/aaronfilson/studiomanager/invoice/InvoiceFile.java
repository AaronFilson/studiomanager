package io.github.aaronfilson.studiomanager.invoice;

/**
 * An uploaded invoice, as a reader receives it: only the file, with no member data
 * (docs/planning/16, "Data minimization").
 */
public record InvoiceFile(byte[] content, InvoiceFileType type) {}
