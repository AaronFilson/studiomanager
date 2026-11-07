package io.github.aaronfilson.studiomanager.invoice;

import java.util.Arrays;

/** The kinds of file the inbox takes: a PDF from the supplier, or a photo of a paper invoice. */
public enum InvoiceFileType {
  PDF("application/pdf", new byte[] {'%', 'P', 'D', 'F', '-'}),
  JPEG("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
  PNG("image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

  private final String mediaType;
  private final byte[] magic;

  InvoiceFileType(String mediaType, byte[] magic) {
    this.mediaType = mediaType;
    this.magic = magic;
  }

  public String mediaType() {
    return mediaType;
  }

  /** How many bytes {@link #startsLike} needs to look at. */
  static int magicLength() {
    return Arrays.stream(values()).mapToInt(type -> type.magic.length).max().orElseThrow();
  }

  /** Whether {@code head}, the first bytes of a file, start the way this kind of file does. */
  boolean startsLike(byte[] head) {
    return head.length >= magic.length && Arrays.equals(Arrays.copyOf(head, magic.length), magic);
  }
}
