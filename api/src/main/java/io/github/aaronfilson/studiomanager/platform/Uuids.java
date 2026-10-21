package io.github.aaronfilson.studiomanager.platform;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Time-ordered UUIDs (RFC 9562 version 7). As unguessable as random v4 IDs, but they sort by
 * creation time, so new rows land at the right edge of the primary-key index instead of
 * scattering across it.
 */
public final class Uuids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Uuids() {
    }

    public static UUID v7() {
        // 48 bits of Unix milliseconds, 4 version bits, 12 random bits
        long mostSignificant = (System.currentTimeMillis() << 16) | 0x7000L | (RANDOM.nextInt() & 0x0FFFL);
        // 2 variant bits (10), 62 random bits
        long leastSignificant = (RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(mostSignificant, leastSignificant);
    }

}
