package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exports a confirmed invoice as a private eval case ZIP: {@code expected.json} (in {@link
 * ClaudeInvoiceAnswer}'s shape, holding no customer PII), {@code case.json} metadata, and the
 * uploaded invoice file when present in storage (docs/planning/24, "Keep score after confirming").
 */
@Service
class InvoiceEvalExport {

  private record ExportSnapshot(InvoiceFileType fileType, String expectedJson, String caseJson) {}

  private final InvoiceRepository invoices;
  private final BlobStore blobs;
  private final TransactionOperations readOnlyTransactions;
  private final Clock clock;

  InvoiceEvalExport(
      InvoiceRepository invoices,
      BlobStore blobs,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.invoices = invoices;
    this.blobs = blobs;
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setReadOnly(true);
    this.readOnlyTransactions = template;
    this.clock = clock;
  }

  byte[] zip(UUID studioId, UUID invoiceId) {
    InvoiceInbox.requireBound(studioId);
    ExportSnapshot snapshot =
        readOnlyTransactions.execute(tx -> {
          Invoice invoice =
              invoices
                  .findWithLines(invoiceId)
                  .orElseThrow(() -> new NotFoundException("No such invoice."));
          if (invoice.status() != InvoiceStatus.CONFIRMED) {
            throw new ConflictException(
                "Confirm this invoice before exporting it as an eval case.");
          }
          InvoiceDraft draft = invoice.draft();
          String expectedJson = InvoiceJson.writeDraftPretty(draft) + "\n";
          String caseJson = InvoiceJson.writeObjectPretty(metadata(invoice, draft)) + "\n";
          return new ExportSnapshot(invoice.fileType(), expectedJson, caseJson);
        });
    if (snapshot == null) {
      throw new NotFoundException("No such invoice.");
    }

    String blobName = InvoiceBlobs.name(studioId, invoiceId, snapshot.fileType());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
      writeText(zip, "expected.json", snapshot.expectedJson());
      writeText(zip, "case.json", snapshot.caseJson());
      if (blobs.info(blobName).isPresent()) {
        writeBytes(
            zip,
            "invoice." + extension(snapshot.fileType()),
            blobs.head(blobName, InvoiceBlobs.MAX_BYTES));
      }
      writeText(zip, "README.txt", readme(invoiceId));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out.toByteArray();
  }

  private Map<String, Object> metadata(Invoice invoice, InvoiceDraft draft) {
    List<String> tags = new ArrayList<>();
    tags.add("confirmed-export");
    tags.add(invoice.fileType().name().toLowerCase(Locale.ROOT));
    if (draft.documentType() != null) {
      tags.add(draft.documentType().name().toLowerCase(Locale.ROOT));
    }
    if (!draft.payments().isEmpty()) {
      tags.add("payments");
    }
    if (!draft.crossedOut().isEmpty()) {
      tags.add("crossed-out");
    }
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("source", "studio-export");
    meta.put("license", "private");
    meta.put("split", "test");
    meta.put("tags", tags);
    meta.put("invoiceId", invoice.id().toString());
    if (invoice.reader() != null) {
      meta.put("reader", invoice.reader().name());
    }
    if (draft.documentType() != null) {
      meta.put("documentType", draft.documentType().name());
    }
    meta.put("exportedAt", clock.instant().toString());
    return meta;
  }

  private static String extension(InvoiceFileType type) {
    return switch (type) {
      case PDF -> "pdf";
      case JPEG -> "jpg";
      case PNG -> "png";
    };
  }

  private static String readme(UUID invoiceId) {
    return """
    Private invoice eval case exported from Studio Manager (invoice %s).

    Unzip this folder into api/src/test/resources/invoices/private/<case-name>/ to include it in
    LiveInvoiceEval. That folder is git-ignored so real supplier documents never leave your machine.

    expected.json holds the confirmed draft with no customer names, addresses, emails or card digits.
    """
        .formatted(invoiceId);
  }

  private static void writeText(ZipOutputStream zip, String name, String content)
      throws IOException {
    writeBytes(zip, name, content.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeBytes(ZipOutputStream zip, String name, byte[] bytes)
      throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(bytes);
    zip.closeEntry();
  }
}
