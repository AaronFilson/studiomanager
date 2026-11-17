package io.github.aaronfilson.studiomanager.invoice;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.jspecify.annotations.Nullable;

/**
 * The copy of an invoice photo that every reader gets (docs/planning/24, "Images"). The original
 * stays as uploaded; the copy is turned the way the camera's EXIF orientation says, made no larger
 * than Claude reads at its high resolution, and kept under the size every provider takes:
 *
 * <ul>
 *   <li>Claude reads at most 2,576 px on the long edge and 4,784 visual tokens of 28 × 28 px (about
 *       3.75 megapixels), and takes 10 MB per image once base64-encoded;
 *   <li>Azure's free F0 tier takes 4 MB per file.
 * </ul>
 *
 * <p>A photo already within all of that, with no turn to make, goes as it is. PDFs always do. If
 * the photo can't be decoded, the original goes, and the reader says what it makes of it.
 */
final class ReadingCopy {

  static final int LONG_EDGE = 2576;
  static final long MOST_PIXELS = 4784L * 28 * 28;
  static final int MOST_BYTES = 4_000_000;

  private static final float[] QUALITIES = {0.9f, 0.8f, 0.7f, 0.6f};

  private ReadingCopy() {}

  static InvoiceFile of(InvoiceFile file) {
    if (file.type() == InvoiceFileType.PDF) {
      return file;
    }
    try {
      BufferedImage image = decode(file.content());
      if (image == null) {
        return file;
      }
      int orientation = orientation(file.content());
      double scale = scale(image.getWidth(), image.getHeight());
      if (orientation == 1 && scale >= 1 && file.content().length <= MOST_BYTES) {
        return file;
      }
      // Shrunk before it's turned, so there's never a second full-size copy in memory. A turn
      // swaps the sides, which doesn't change the scale.
      BufferedImage sized = scale >= 1 ? image : resized(image, scale);
      return new InvoiceFile(jpeg(orient(sized, orientation)), InvoiceFileType.JPEG);
    } catch (IOException | RuntimeException e) {
      return file;
    }
  }

  /**
   * The image, decoded. A photo far larger than the copy (a 100-megapixel phone camera's) is
   * decoded at every second, third, ... pixel, still at least twice the copy's size, so the
   * full-size image is never held in memory. Null when no reader knows the format.
   */
  private static @Nullable BufferedImage decode(byte[] content) throws IOException {
    try (ImageInputStream input =
        ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
      if (input == null) {
        return null;
      }
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        return null;
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        int step = (int) Math.floor(0.5 / scale(reader.getWidth(0), reader.getHeight(0)));
        ImageReadParam param = reader.getDefaultReadParam();
        if (step > 1) {
          param.setSourceSubsampling(step, step, 0, 0);
        }
        return reader.read(0, param);
      } finally {
        reader.dispose();
      }
    }
  }

  /** How much to shrink a photo of this size: 1 when it's within Claude's limits already. */
  static double scale(int width, int height) {
    double byEdge = (double) LONG_EDGE / Math.max(width, height);
    double byPixels = Math.sqrt((double) MOST_PIXELS / ((double) width * height));
    return Math.min(1, Math.min(byEdge, byPixels));
  }

  /** The EXIF orientation, 1 to 8; 1 (as shot) when there's none or it can't be read. */
  static int orientation(byte[] content) {
    try {
      Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(content));
      ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
      if (exif == null || !exif.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
        return 1;
      }
      int orientation = exif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
      return orientation >= 1 && orientation <= 8 ? orientation : 1;
    } catch (ImageProcessingException | MetadataException | IOException e) {
      return 1;
    }
  }

  /**
   * The image as it should be seen, for each of EXIF's eight orientations. Each transform maps a
   * stored pixel (x, y) of a W × H image to where it's shown, as {@code AffineTransform(m00, m10,
   * m01, m11, m02, m12)}: x' = m00·x + m01·y + m02, y' = m10·x + m11·y + m12.
   */
  static BufferedImage orient(BufferedImage image, int orientation) {
    double width = image.getWidth();
    double height = image.getHeight();
    AffineTransform turn =
        switch (orientation) {
          // Mirrored left to right: (W − x, y)
          case 2 -> new AffineTransform(-1, 0, 0, 1, width, 0);
          // Upside down: (W − x, H − y)
          case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
          // Mirrored top to bottom: (x, H − y)
          case 4 -> new AffineTransform(1, 0, 0, -1, 0, height);
          // Transposed: (y, x)
          case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
          // A quarter turn clockwise, the usual phone held upright: (H − y, x)
          case 6 -> new AffineTransform(0, 1, -1, 0, height, 0);
          // Transversed: (H − y, W − x)
          case 7 -> new AffineTransform(0, -1, -1, 0, height, width);
          // A quarter turn anticlockwise: (y, W − x)
          case 8 -> new AffineTransform(0, -1, 1, 0, 0, width);
          default -> null;
        };
    if (turn == null) {
      return opaque(image);
    }
    boolean sideways = orientation >= 5;
    BufferedImage turned =
        new BufferedImage(
            sideways ? image.getHeight() : image.getWidth(),
            sideways ? image.getWidth() : image.getHeight(),
            BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = turned.createGraphics();
    try {
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, turned.getWidth(), turned.getHeight());
      graphics.drawImage(image, turn, null);
    } finally {
      graphics.dispose();
    }
    return turned;
  }

  /** Shrinks in halves, then once more to size, which keeps small print readable. */
  static BufferedImage resized(BufferedImage image, double scale) {
    int targetWidth = Math.max(1, (int) Math.floor(image.getWidth() * scale));
    int targetHeight = Math.max(1, (int) Math.floor(image.getHeight() * scale));
    BufferedImage current = image;
    while (current.getWidth() / 2 >= targetWidth && current.getHeight() / 2 >= targetHeight) {
      current = drawn(current, current.getWidth() / 2, current.getHeight() / 2);
    }
    return current.getWidth() == targetWidth && current.getHeight() == targetHeight
        ? current
        : drawn(current, targetWidth, targetHeight);
  }

  private static BufferedImage drawn(BufferedImage image, int width, int height) {
    BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = target.createGraphics();
    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, width, height);
      graphics.drawImage(image, 0, 0, width, height, null);
    } finally {
      graphics.dispose();
    }
    return target;
  }

  /** A PNG's transparency becomes white paper, since JPEG has none. */
  private static BufferedImage opaque(BufferedImage image) {
    return image.getType() == BufferedImage.TYPE_INT_RGB
        ? image
        : drawn(image, image.getWidth(), image.getHeight());
  }

  /** JPEG at the best quality that fits in {@link #MOST_BYTES}. */
  static byte[] jpeg(BufferedImage image) throws IOException {
    byte[] encoded = null;
    for (float quality : QUALITIES) {
      encoded = encode(image, quality);
      if (encoded.length <= MOST_BYTES) {
        return encoded;
      }
    }
    return encoded == null ? encode(image, 0.6f) : encoded;
  }

  private static byte[] encode(BufferedImage image, float quality) throws IOException {
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
      writer.setOutput(output);
      ImageWriteParam param = writer.getDefaultWriteParam();
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionQuality(quality);
      writer.write(null, new IIOImage(opaque(image), null, null), param);
    } finally {
      writer.dispose();
    }
    return bytes.toByteArray();
  }

  /** For a log line: what the copy changed, or null when it's the original. */
  static @Nullable String change(InvoiceFile original, InvoiceFile copy) {
    return original.type() == copy.type() && Arrays.equals(original.content(), copy.content())
        ? null
        : "%s of %d bytes became a JPEG of %d bytes"
            .formatted(original.type(), original.content().length, copy.content().length);
  }
}
