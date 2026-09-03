package com.mysportsapp.dedup;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Computes a stable, deterministic key used to detect "the same activity,
 * re-imported" - either because the same file was uploaded twice, or because
 * two providers/files describe the same real-world workout.
 *
 * <p>The key is a SHA-256 hex digest over
 * {@code userId + ":" + activityType + ":" + roundedStartTime + ":" + roundedDuration},
 * where the start time is rounded to the nearest minute and the duration to
 * the nearest 10 seconds - close enough that minor differences in watch
 * clock drift or GPS lock time between two exports of the same activity
 * still land on the same key.
 */
@Component
public class DedupKeyGenerator {

    private static final long MINUTE_SECONDS = 60L;
    private static final long DURATION_BUCKET_SECONDS = 10L;

    public String generate(UUID userId, String activityType, Instant startTime, long durationSeconds) {
        Instant roundedStart = roundToNearestMinute(startTime);
        long roundedDuration = roundToNearest(durationSeconds, DURATION_BUCKET_SECONDS);

        String raw = userId + ":" + activityType + ":" + roundedStart + ":" + roundedDuration;
        return sha256Hex(raw);
    }

    private Instant roundToNearestMinute(Instant instant) {
        long epochSecond = instant.getEpochSecond();
        long roundedSeconds = roundToNearest(epochSecond, MINUTE_SECONDS);
        return Instant.ofEpochSecond(roundedSeconds);
    }

    private long roundToNearest(long value, long bucket) {
        return Math.round(value / (double) bucket) * bucket;
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
