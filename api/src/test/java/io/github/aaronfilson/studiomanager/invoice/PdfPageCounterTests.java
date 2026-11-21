package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PdfPageCounterTests {

  @Test
  void countsPagesFromPagesTreeCount() {
    byte[] tenPages =
        "%PDF-1.7\n1 0 obj\n<< /Type /Pages /Kids [2 0 R] /Count 10 >>\nendobj\n"
            .getBytes(StandardCharsets.ISO_8859_1);
    byte[] elevenPages =
        "%PDF-1.7\n1 0 obj\n<< /Type /Pages /Count 11 /Kids [2 0 R] >>\nendobj\n"
            .getBytes(StandardCharsets.ISO_8859_1);
    byte[] countFirst =
        "%PDF-1.7\n1 0 obj\n<< /Count 15 /Type /Pages >>\nendobj\n"
            .getBytes(StandardCharsets.ISO_8859_1);

    assertThat(PdfPageCounter.countPages(tenPages)).hasValue(10);
    assertThat(PdfPageCounter.countPages(elevenPages)).hasValue(11);
    assertThat(PdfPageCounter.countPages(countFirst)).hasValue(15);
  }

  @Test
  void countsLeafPageObjectsWithoutDoubleCountingPagesParent() {
    StringBuilder pdf = new StringBuilder("%PDF-1.7\n1 0 obj << /Type /Pages >> endobj\n");
    for (int i = 0; i < 12; i++) {
      pdf.append(i + 2).append(" 0 obj << /Type /Page /Parent 1 0 R >> endobj\n");
    }
    assertThat(PdfPageCounter.countPages(pdf.toString().getBytes(StandardCharsets.ISO_8859_1)))
        .hasValue(12);
  }

  @Test
  void returnsEmptyWhenCompressedOrMinimalHeaderOnly() {
    byte[] minimal = "%PDF-1.7\n% minimal\n".getBytes(StandardCharsets.ISO_8859_1);
    assertThat(PdfPageCounter.countPages(minimal)).isEmpty();
    assertThat(PdfPageCounter.countPages(new byte[0])).isEmpty();
  }
}
