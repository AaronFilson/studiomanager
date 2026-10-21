package io.github.aaronfilson.studiomanager.platform.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AzureBlobStoreTests {

    @Test
    void readUrlsExpireOnTheHourAtLeastAnHourAhead() {
        Instant expiry = AzureBlobStore.readExpiry(Instant.parse("2026-10-20T14:00:00Z"));

        assertThat(expiry).isEqualTo(Instant.parse("2026-10-20T16:00:00Z"));
        assertThat(AzureBlobStore.readExpiry(Instant.parse("2026-10-20T14:59:59Z"))).isEqualTo(expiry);
    }

}
