package io.github.aaronfilson.studiomanager.platform.storage;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/**
 * The container that holds uploaded files. Browsers and phones move the bytes themselves, with
 * short-lived signed URLs; the API only hands those out and checks what arrived.
 */
public interface BlobStore {

    /**
     * A URL that can create {@code name} once, by an HTTP PUT with {@code x-ms-blob-type: BlockBlob},
     * until {@code expiresAt}. It can't overwrite the blob once it exists, or read anything.
     */
    URI uploadUrl(String name, Instant expiresAt);

    /**
     * A URL that can read {@code name} for at least an hour. It stays the same within each hour, so
     * browsers can cache the image instead of fetching it again on every page.
     */
    URI readUrl(String name, String contentType);

    /** The blob's size and content type, or empty if nothing was uploaded under that name. */
    Optional<BlobInfo> info(String name);

    /** Up to the first {@code count} bytes of the blob. */
    byte[] head(String name, int count);

    /** Deletes the blob if it exists. */
    void delete(String name);

    record BlobInfo(long size, String contentType) {
    }

}
