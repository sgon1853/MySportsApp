package com.mysportsapp.integrations.strava;

import com.mysportsapp.integrations.strava.dto.StravaStreamSet;
import com.mysportsapp.integrations.strava.dto.StravaSummaryActivity;
import com.mysportsapp.provider.spi.ParsedActivity;
import com.mysportsapp.provider.spi.ParsedTrackPoint;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Strava's {@code type} -> {@link ParsedActivity#activityType()}. Normalized
 * to the same vocabulary the GPX provider produces (a run from either source
 * should look like the same activity type) where there's a clear
 * equivalent; anything else falls back to Strava's own type, uppercased -
 * this app doesn't enforce a fixed activity-type enum (see
 * {@code activities.activity_type} - a plain VARCHAR), so an unmapped type
 * still works, it just won't group with a GPX activity of the same real-world
 * kind.
 */
@Component
public class StravaActivityMapper {

    private static final Set<String> RUN_TYPES = Set.of("run", "trailrun", "virtualrun");
    private static final Set<String> RIDE_TYPES = Set.of("ride", "mountainbikeride", "gravelride", "virtualride", "ebikeride");

    public ParsedActivity toParsedActivity(StravaSummaryActivity summary, StravaStreamSet streams) {
        return new ParsedActivity(
                normalizeActivityType(summary.type()),
                summary.startDate(),
                summary.elapsedTimeSeconds(),
                summary.distance(),
                roundToInt(summary.averageHeartrate()),
                roundToInt(summary.maxHeartrate()),
                null, // calories: not fetched in Phase 1 - see StravaApiClient's javadoc
                summary.totalElevationGain(),
                toTrackPoints(summary.startDate(), streams)
        );
    }

    private String normalizeActivityType(String stravaType) {
        if (stravaType == null || stravaType.isBlank()) {
            return "OTHER";
        }
        String lower = stravaType.toLowerCase(Locale.ROOT);
        if (RUN_TYPES.contains(lower)) {
            return "RUNNING";
        }
        if (RIDE_TYPES.contains(lower)) {
            return "CYCLING";
        }
        return stravaType.toUpperCase(Locale.ROOT);
    }

    private List<ParsedTrackPoint> toTrackPoints(Instant startTime, StravaStreamSet streams) {
        List<Double> timeOffsets = dataOf(streams.time());
        if (timeOffsets.isEmpty()) {
            return List.of();
        }

        List<List<Double>> latLngs = streams.latlng() == null ? List.of() : nullToEmpty(streams.latlng().data());
        List<Double> altitudes = dataOf(streams.altitude());
        List<Double> heartrates = dataOf(streams.heartrate());

        List<ParsedTrackPoint> points = new ArrayList<>(timeOffsets.size());
        for (int i = 0; i < timeOffsets.size(); i++) {
            Instant timestamp = startTime.plusSeconds(timeOffsets.get(i).longValue());
            List<Double> latLng = i < latLngs.size() ? latLngs.get(i) : null;
            Double lat = latLng != null && latLng.size() == 2 ? latLng.get(0) : null;
            Double lon = latLng != null && latLng.size() == 2 ? latLng.get(1) : null;
            Double elevation = i < altitudes.size() ? altitudes.get(i) : null;
            Integer heartRate = i < heartrates.size() ? roundToInt(heartrates.get(i)) : null;

            points.add(new ParsedTrackPoint(timestamp, lat, lon, elevation, heartRate));
        }
        return points;
    }

    private List<Double> dataOf(StravaStreamSet.ValueStream stream) {
        return stream == null ? List.of() : nullToEmpty(stream.data());
    }

    private <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private Integer roundToInt(Double value) {
        return value == null ? null : (int) Math.round(value);
    }
}
