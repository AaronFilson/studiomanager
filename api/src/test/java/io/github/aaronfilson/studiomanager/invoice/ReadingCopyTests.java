package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** The upright, size-limited copy of an invoice photo that the readers get. */
class ReadingCopyTests {

  private static final int RED = 0xFF0000;
  private static final int BLUE = 0x0000FF;

  @Test
  void aLargePhoneShotBecomesAJpegWithinClaudesAndAzuresLimits() throws IOException {
    InvoiceFile photo = new InvoiceFile(encode(noisy(4000, 3000), "png"), InvoiceFileType.PNG);
    assertThat(photo.content().length).isGreaterThan(ReadingCopy.MOST_BYTES);

    InvoiceFile copy = ReadingCopy.of(photo);

    assertThat(copy.type()).isEqualTo(InvoiceFileType.JPEG);
    assertThat(copy.content().length).isLessThanOrEqualTo(ReadingCopy.MOST_BYTES);
    BufferedImage read = ImageIO.read(new ByteArrayInputStream(copy.content()));
    assertThat(Math.max(read.getWidth(), read.getHeight())).isLessThanOrEqualTo(2576);
    assertThat((long) read.getWidth() * read.getHeight()).isLessThanOrEqualTo(4784L * 28 * 28);
    // Still the same shape of page
    assertThat((double) read.getWidth() / read.getHeight()).isCloseTo(4.0 / 3, within(0.01));
  }

  @Test
  void aPhotoWithinTheLimitsGoesAsItIs() throws IOException {
    InvoiceFile photo = new InvoiceFile(encode(noisy(800, 600), "jpeg"), InvoiceFileType.JPEG);

    assertThat(ReadingCopy.of(photo)).isSameAs(photo);
    assertThat(ReadingCopy.change(photo, ReadingCopy.of(photo))).isNull();
  }

  @Test
  void pdfsAndFilesThatAreNotImagesGoAsTheyAre() {
    InvoiceFile pdf =
        new InvoiceFile("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII), InvoiceFileType.PDF);
    InvoiceFile broken = new InvoiceFile(new byte[] {1, 2, 3}, InvoiceFileType.JPEG);

    assertThat(ReadingCopy.of(pdf)).isSameAs(pdf);
    assertThat(ReadingCopy.of(broken)).isSameAs(broken);
  }

  @Test
  void exifOrientationsTurnThePhotoUpright() {
    // Red on the left, blue on the right, as the camera stored it
    BufferedImage stored = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
    stored.setRGB(0, 0, RED);
    stored.setRGB(1, 0, BLUE);

    // 6: shot with the phone upright, so a quarter turn clockwise: red ends on top
    BufferedImage six = ReadingCopy.orient(stored, 6);
    assertThat(six.getWidth()).isEqualTo(1);
    assertThat(six.getHeight()).isEqualTo(2);
    assertThat(six.getRGB(0, 0) & 0xFFFFFF).isEqualTo(RED);
    assertThat(six.getRGB(0, 1) & 0xFFFFFF).isEqualTo(BLUE);

    // 8: a quarter turn anticlockwise: blue on top
    BufferedImage eight = ReadingCopy.orient(stored, 8);
    assertThat(eight.getRGB(0, 0) & 0xFFFFFF).isEqualTo(BLUE);

    // 3: upside down: blue on the left
    BufferedImage three = ReadingCopy.orient(stored, 3);
    assertThat(three.getRGB(0, 0) & 0xFFFFFF).isEqualTo(BLUE);
  }

  @Test
  void aPhotoWithNoExifIsAsShot() throws IOException {
    assertThat(ReadingCopy.orientation(encode(noisy(10, 10), "png"))).isEqualTo(1);
  }

  private static org.assertj.core.data.Offset<Double> within(double offset) {
    return org.assertj.core.data.Offset.offset(offset);
  }

  /** Noise doesn't compress, so the file is as large as a real photo of that size. */
  private static BufferedImage noisy(int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Random random = new Random(7);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        image.setRGB(x, y, random.nextInt(0x1000000));
      }
    }
    return image;
  }

  private static byte[] encode(BufferedImage image, String format) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(image, format, bytes);
    return bytes.toByteArray();
  }
}
