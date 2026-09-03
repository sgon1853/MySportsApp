package com.mysportsapp.provider.suunto;

import com.mysportsapp.provider.spi.ImportContext;
import com.mysportsapp.provider.spi.ParseResult;
import com.mysportsapp.provider.spi.ParsedActivity;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain JUnit5, no Spring context - providers must be pure and testable in
 * isolation.
 */
class SuuntoGpxProviderTest {

    private final SuuntoGpxProvider provider = new SuuntoGpxProvider();

    @Test
    void parsesRunningTrackWithHeartRate() {
        ParseResult result = parseFixture("sample-run.gpx");

        assertThat(result.activities()).hasSize(1);
        ParsedActivity activity = result.activities().get(0);

        assertThat(activity.activityType()).isEqualTo("RUNNING");
        assertThat(activity.trackPoints()).hasSize(12);
        assertThat(activity.durationSeconds()).isEqualTo(330L); // 07:00:00 -> 07:05:30
        assertThat(activity.distanceMeters()).isNotNull().isGreaterThan(0.0);
        assertThat(activity.elevationGainMeters()).isNotNull().isGreaterThan(0.0);
        assertThat(activity.avgHr()).isNotNull();
        assertThat(activity.maxHr()).isNotNull();
        assertThat(activity.maxHr()).isEqualTo(150);
        assertThat(activity.calories()).isNull();
        assertThat(activity.trackPoints().get(0).heartRate()).isEqualTo(118);
    }

    @Test
    void parsesMultiSegmentRideAsSingleActivity() {
        ParseResult result = parseFixture("sample-ride-multilap.gpx");

        assertThat(result.activities()).hasSize(1);
        ParsedActivity activity = result.activities().get(0);

        assertThat(activity.activityType()).isEqualTo("CYCLING");
        // 6 points in segment 1 + 7 points in segment 2
        assertThat(activity.trackPoints()).hasSize(13);
        assertThat(activity.durationSeconds()).isEqualTo(780L); // 18:00:00 -> 18:13:00
        assertThat(activity.distanceMeters()).isGreaterThan(0.0);
    }

    @Test
    void handlesMissingHeartRateGracefully() {
        ParseResult result = parseFixture("edge-case-no-hr.gpx");

        assertThat(result.activities()).hasSize(1);
        ParsedActivity activity = result.activities().get(0);

        assertThat(activity.activityType()).isEqualTo("OTHER");
        assertThat(activity.trackPoints()).hasSize(10);
        assertThat(activity.avgHr()).isNull();
        assertThat(activity.maxHr()).isNull();
        activity.trackPoints().forEach(tp -> assertThat(tp.heartRate()).isNull());
    }

    private ParseResult parseFixture(String name) {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/gpx/" + name)) {
            assertThat(in).as("fixture " + name + " must exist on classpath").isNotNull();
            ImportContext context = new ImportContext(UUID.randomUUID(), name, UUID.randomUUID());
            return provider.parse(in, context);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
