package io.github.aaronfilson.studiomanager.platform.storage;

import java.time.Duration;
import java.util.Arrays;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;

/**
 * The rules for photos that browsers and phones upload straight to storage (ADR 12): how big each
 * of the two images may be, how long the upload URLs last, and the check that what arrived is a
 * JPEG within its limit. Shared by piece photos and maintenance record photos.
 */
public final class UploadedImages {

    public static final int MAX_PHOTO_BYTES = 8 * 1024 * 1024;
    public static final int MAX_THUMBNAIL_BYTES = 512 * 1024;
    public static final Duration UPLOAD_WINDOW = Duration.ofMinutes(15);
    public static final String CONTENT_TYPE = "image/jpeg";

    /** Every JPEG starts with a start-of-image marker followed by another marker. */
    private static final byte[] JPEG_MAGIC = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

    private UploadedImages() {
    }

    /**
     * Checks that {@code blob} was uploaded, is no larger than {@code maxBytes}, and starts like a
     * JPEG. Returns its size.
     *
     * @param what "photo" or "thumbnail", for the message
     */
    public static long check(BlobStore blobs, String blob, int maxBytes, String what) {
        BlobStore.BlobInfo info = blobs.info(blob)
                .orElseThrow(() -> new InvalidInputException("The %s hasn't been uploaded yet.".formatted(what)));
        if (info.size() > maxBytes) {
            throw new InvalidInputException("The %s is larger than %d KB. Shrink it and try again."
                    .formatted(what, maxBytes / 1024));
        }
        if (info.size() < JPEG_MAGIC.length || !Arrays.equals(blobs.head(blob, JPEG_MAGIC.length), JPEG_MAGIC)) {
            throw new InvalidInputException("The %s isn't a JPEG image.".formatted(what));
        }
        return info.size();
    }

}
