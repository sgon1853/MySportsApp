package com.mysportsapp.integrations.strava;

import com.mysportsapp.integrations.strava.dto.StravaStreamSet;
import com.mysportsapp.integrations.strava.dto.StravaSummaryActivity;
import com.mysportsapp.provider.spi.ParsedActivity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test, no Spring context - same style as
 * {@code provider/suunto/SuuntoGpxProviderTest}. Fixture objects are built by
 * hand here rather than from captured real Strava JSON (unlike the GPX
 * fixtures, which are real-shaped files) - see docs/deployment.md-adjacent
 * plan notes: replacing these with real captured payloads once a Strava
 * account is actually connected is part of the "real verification" step
 * before calling this phase done, not a nice-to-have.
 */
class StravaActivityMapperTest {

    private final StravaActivityMapper mapper = new StravaActivityMapper();

    @Test
    void mapsARunWithAFullTrackToParsedActivity() {
        Instant start = Instant.parse("2026-06-01T07:00:00Z");
        StravaSummaryActivity summary = new StravaSummaryActivity(
                123L, "Morning Run", "Run", start, 1800L, 5000.0, 42.5, 138.0, 162.0);

        StravaStreamSet streams = new StravaStreamSet(
                new StravaStreamSet.LatLngStream(List.of(
                        List.of(40.7128, -74.0060),
                        List.of(40.7130, -74.0058))),
                new StravaStreamSet.ValueStream(List.of(10.0, 12.0)),
                new StravaStreamSet.ValueStream(List.of(120.0, 125.0)),
                new StravaStreamSet.ValueStream(List.of(0.0, 30.0)));

        ParsedActivity activity = mapper.toParsedActivity(summary, streams);

        assertThat(activity.activityType()).isEqualTo("RUNNING");
        assertThat(activity.startTime()).isEqualTo(start);
        assertThat(activity.durationSeconds()).isEqualTo(1800L);
        assertThat(activity.distanceMeters()).isEqualTo(5000.0);
        assertThat(activity.avgHr()).isEqualTo(138);
        assertThat(activity.maxHr()).isEqualTo(162);
        assertThat(activity.calories()).isNull(); // deliberately not fetched in Phase 1
        assertThat(activity.elevationGainMeters()).isEqualTo(42.5);

        assertThat(activity.trackPoints()).hasSize(2);
        var firstPoint = activity.trackPoints().get(0);
        assertThat(firstPoint.timestamp()).isEqualTo(start);
        assertThat(firstPoint.lat()).isEqualTo(40.7128);
        assertThat(firstPoint.lon()).isEqualTo(-74.0060);
        assertThat(firstPoint.elevationMeters()).isEqualTo(10.0);
        assertThat(firstPoint.heartRate()).isEqualTo(120);

        var secondPoint = activity.trackPoints().get(1);
        assertThat(secondPoint.timestamp()).isEqualTo(start.plusSeconds(30));
        assertThat(secondPoint.heartRate()).isEqualTo(125);
    }

    @Test
    void mapsARideTypeToCycling() {
        StravaSummaryActivity summary = new StravaSummaryActivity(
                1L, "Evening Ride", "MountainBikeRide", Instant.now(), 3600L, 20000.0, 100.0, null, null);

        ParsedActivity activity = mapper.toParsedActivity(summary, emptyStreams());

        assertThat(activity.activityType()).isEqualTo("CYCLING");
        assertThat(activity.avgHr()).isNull();
        assertThat(activity.maxHr()).isNull();
    }

    @Test
    void fallsBackToTheRawUppercasedTypeForAnUnmappedActivity() {
        StravaSummaryActivity summary = new StravaSummaryActivity(
                1L, "Pool Session", "Swim", Instant.now(), 1200L, 1000.0, null, null, null);

        ParsedActivity activity = mapper.toParsedActivity(summary, emptyStreams());

        assertThat(activity.activityType()).isEqualTo("SWIM");
    }

    @Test
    void anActivityWithNoStreamsAtAllProducesNoTrackPoints() {
        StravaSummaryActivity summary = new StravaSummaryActivity(
                1L, "Indoor Trainer", "Ride", Instant.now(), 3000L, null, null, 140.0, 155.0);

        ParsedActivity activity = mapper.toParsedActivity(summary, emptyStreams());

        assertThat(activity.trackPoints()).isEmpty();
        // Summary stats (including HR, which Strava does report for indoor
        // activities even with no GPS) are still preserved.
        assertThat(activity.avgHr()).isEqualTo(140);
    }

    @Test
    void aTrackPointWithNoLatLngStreamStillGetsElevationAndHeartRate() {
        Instant start = Instant.now();
        StravaSummaryActivity summary = new StravaSummaryActivity(
                1L, "Indoor Trainer", "Ride", start, 60L, null, null, null, null);
        // No latlng stream at all (indoor), but altitude/heartrate/time are present.
        StravaStreamSet streams = new StravaStreamSet(
                null,
                new StravaStreamSet.ValueStream(List.of(5.0)),
                new StravaStreamSet.ValueStream(List.of(130.0)),
                new StravaStreamSet.ValueStream(List.of(0.0)));

        ParsedActivity activity = mapper.toParsedActivity(summary, streams);

        assertThat(activity.trackPoints()).hasSize(1);
        var point = activity.trackPoints().get(0);
        assertThat(point.lat()).isNull();
        assertThat(point.lon()).isNull();
        assertThat(point.elevationMeters()).isEqualTo(5.0);
        assertThat(point.heartRate()).isEqualTo(130);
    }

    private StravaStreamSet emptyStreams() {
        return new StravaStreamSet(null, null, null, null);
    }
}
