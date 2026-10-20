package io.github.aaronfilson.studiomanager.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class UuidsTests {

    @Test
    void areVersion7WithTheStandardVariant() {
        UUID id = Uuids.v7();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void carryTheirCreationTimeInTheFirst48Bits() {
        long before = System.currentTimeMillis();
        UUID id = Uuids.v7();
        long after = System.currentTimeMillis();

        assertThat(id.getMostSignificantBits() >>> 16).isBetween(before, after);
    }

    @Test
    void sortInCreationOrder() throws InterruptedException {
        UUID earlier = Uuids.v7();
        Thread.sleep(2);
        UUID later = Uuids.v7();

        // PostgreSQL compares uuids byte by byte, which is the same order as their hex strings
        assertThat(earlier.toString()).isLessThan(later.toString());
    }

}
