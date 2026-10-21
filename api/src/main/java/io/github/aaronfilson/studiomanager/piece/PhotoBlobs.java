package io.github.aaronfilson.studiomanager.piece;

import java.util.UUID;

/**
 * Where a photo's two images are stored. Names are built from ids, never taken from a client, so
 * a request can only reach blobs under its own studio and piece.
 */
final class PhotoBlobs {

    static final String CONTENT_TYPE = "image/jpeg";

    private PhotoBlobs() {
    }

    static String full(UUID studioId, UUID pieceId, UUID photoId) {
        return "%s/%s/%s/full.jpg".formatted(studioId, pieceId, photoId);
    }

    static String thumbnail(UUID studioId, UUID pieceId, UUID photoId) {
        return "%s/%s/%s/thumb.jpg".formatted(studioId, pieceId, photoId);
    }

}
