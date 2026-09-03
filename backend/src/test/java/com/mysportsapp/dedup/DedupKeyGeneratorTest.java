package com.mysportsapp.dedup;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DedupKeyGeneratorTest {

    private final DedupKeyGenerator generator = new DedupKeyGenerator();
    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final Instant base = Instant.parse("2026-06-01T07:00:00Z");

    @Test
    void identicalInputsProduceTheSameKey() {
        String key1 = generator.generate(userId, "RUNNING", base, 300L);
        String key2 = generator.generate(userId, "RUNNING", base, 300L);

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void startTimesAFewSecondsApartRoundToTheSameKey() {
        // both within 30s of `base`, so both round to the same minute bucket
        String key1 = generator.generate(userId, "RUNNING", base, 300L);
        String key2 = generator.generate(userId, "RUNNING", base.plusSeconds(5), 300L);

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void startTimesAcrossTheRoundingBoundaryProduceDifferentKeys() {
        // 25s rounds down to `base`; 35s rounds up to `base + 60s`
        String key1 = generator.generate(userId, "RUNNING", base.plusSeconds(25), 300L);
        String key2 = generator.generate(userId, "RUNNING", base.plusSeconds(35), 300L);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void durationsAFewSecondsApartRoundToTheSameKey() {
        // both within 5s of 300, so both round to the same 10s bucket
        String key1 = generator.generate(userId, "RUNNING", base, 302L);
        String key2 = generator.generate(userId, "RUNNING", base, 297L);

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void durationsAcrossTheRoundingBoundaryProduceDifferentKeys() {
        String key1 = generator.generate(userId, "RUNNING", base, 124L); // rounds to 120
        String key2 = generator.generate(userId, "RUNNING", base, 126L); // rounds to 130

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void differentActivityTypeProducesDifferentKey() {
        String key1 = generator.generate(userId, "RUNNING", base, 300L);
        String key2 = generator.generate(userId, "CYCLING", base, 300L);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void differentUserProducesDifferentKey() {
        String key1 = generator.generate(userId, "RUNNING", base, 300L);
        String key2 = generator.generate(UUID.randomUUID(), "RUNNING", base, 300L);

        assertThat(key1).isNotEqualTo(key2);
    }
}
