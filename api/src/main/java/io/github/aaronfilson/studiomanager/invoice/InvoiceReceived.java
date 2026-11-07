package io.github.aaronfilson.studiomanager.invoice;

import java.util.UUID;

/** An invoice's file arrived and passed its checks, so it can be read. */
public record InvoiceReceived(UUID invoiceId, UUID studioId) {}
