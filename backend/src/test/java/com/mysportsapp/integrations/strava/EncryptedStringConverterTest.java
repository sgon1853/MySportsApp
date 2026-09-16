package com.mysportsapp.integrations.strava;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test - constructs the converter directly with a fixed test key
 * rather than going through Spring, since all it needs is the one
 * constructor argument {@code @Value} would otherwise inject.
 */
class EncryptedStringConverterTest {

    // A throwaway AES-256 key generated the same way the real ones are
    // (openssl rand -base64 32) - not used anywhere real.
    private static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final EncryptedStringConverter converter = new EncryptedStringConverter(TEST_KEY);

    @Test
    void roundTripsAValueThroughEncryptionAndDecryption() {
        String plaintext = "a-real-looking-strava-access-token-abc123";

        String stored = converter.convertToDatabaseColumn(plaintext);
        String recovered = converter.convertToEntityAttribute(stored);

        assertThat(recovered).isEqualTo(plaintext);
    }

    @Test
    void theStoredValueIsNotThePlaintext() {
        String plaintext = "a-real-looking-strava-refresh-token-xyz789";

        String stored = converter.convertToDatabaseColumn(plaintext);

        assertThat(stored).doesNotContain(plaintext);
    }

    @Test
    void encryptingTheSameValueTwiceProducesDifferentOutput() {
        // Each call uses a fresh random IV - required for GCM's security
        // guarantees, and incidentally means two encryptions of the same
        // token aren't distinguishable from each other at rest either.
        String plaintext = "same-token-both-times";

        String first = converter.convertToDatabaseColumn(plaintext);
        String second = converter.convertToDatabaseColumn(plaintext);

        assertThat(first).isNotEqualTo(second);
        assertThat(converter.convertToEntityAttribute(first)).isEqualTo(plaintext);
        assertThat(converter.convertToEntityAttribute(second)).isEqualTo(plaintext);
    }

    @Test
    void nullPassesThroughUnchanged() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
