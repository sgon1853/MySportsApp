package com.mysportsapp.provider.suunto;

import com.mysportsapp.provider.spi.DataProvider;
import com.mysportsapp.provider.spi.ImportContext;
import com.mysportsapp.provider.spi.ParseResult;
import com.mysportsapp.provider.spi.ParsedActivity;
import com.mysportsapp.provider.spi.ParsedTrackPoint;
import com.mysportsapp.provider.spi.ProviderParseException;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Length;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.WayPoint;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * {@link DataProvider} for GPX files exported from Suunto watches (also
 * handles generic GPX 1.1 track data, since Suunto's export is a plain GPX
 * file with an optional Garmin TrackPointExtension for heart rate).
 */
@Component
public class SuuntoGpxProvider implements DataProvider {

    private static final String PROVIDER_ID = "suunto-gpx";
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayName() {
        return "Suunto (GPX)";
    }

    @Override
    public Set<String> getSupportedFileExtensions() {
        return Set.of("gpx");
    }

    @Override
    public boolean canParse(String filename, byte[] content) {
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".gpx")) {
            return false;
        }
        if (content == null || content.length == 0) {
            return false;
        }
        int len = Math.min(content.length, 512);
        String head = new String(content, 0, len, StandardCharsets.UTF_8);
        return head.contains("<gpx");
    }

    @Override
    public ParseResult parse(InputStream input, ImportContext context) throws ProviderParseException {
        GPX gpx;
        try {
            gpx = GPX.Reader.DEFAULT.read(input);
        } catch (IOException | RuntimeException e) {
            throw new ProviderParseException("Unable to parse GPX file: " + e.getMessage(), e);
        }

        List<ParsedActivity> activities = new ArrayList<>();
        for (Track track : gpx.tracks().toList()) {
            activities.add(toParsedActivity(track));
        }

        if (activities.isEmpty()) {
            throw new ProviderParseException("GPX file contains no <trk> tracks");
        }

        return new ParseResult(activities);
    }

    private ParsedActivity toParsedActivity(Track track) {
        List<WayPoint> wayPoints = new ArrayList<>();
        track.getSegments().forEach(segment -> wayPoints.addAll(segment.getPoints()));

        if (wayPoints.isEmpty()) {
            throw new ProviderParseException("GPX track contains no track points");
        }

        List<ParsedTrackPoint> trackPoints = new ArrayList<>(wayPoints.size());
        for (WayPoint wp : wayPoints) {
            trackPoints.add(new ParsedTrackPoint(
                    wp.getTime().orElse(null),
                    wp.getLatitude() != null ? wp.getLatitude().doubleValue() : null,
                    wp.getLongitude() != null ? wp.getLongitude().doubleValue() : null,
                    wp.getElevation().map(e -> e.to(Length.Unit.METER)).orElse(null),
                    extractHeartRate(wp)
            ));
        }

        String activityType = track.getType()
                .map(t -> t.trim().toUpperCase(Locale.ROOT))
                .filter(t -> !t.isBlank())
                .orElse("OTHER");

        Instant startTime = trackPoints.stream()
                .map(ParsedTrackPoint::timestamp)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new ProviderParseException("GPX track points have no timestamps"));

        Instant endTime = trackPoints.stream()
                .map(ParsedTrackPoint::timestamp)
                .filter(java.util.Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(startTime);

        long durationSeconds = Math.max(0, endTime.getEpochSecond() - startTime.getEpochSecond());

        double distanceMeters = computeDistanceMeters(trackPoints);
        double elevationGainMeters = computeElevationGainMeters(trackPoints);

        List<Integer> heartRates = trackPoints.stream()
                .map(ParsedTrackPoint::heartRate)
                .filter(java.util.Objects::nonNull)
                .toList();
        Integer avgHr = heartRates.isEmpty() ? null
                : (int) Math.round(heartRates.stream().mapToInt(Integer::intValue).average().orElse(0));
        Integer maxHr = heartRates.isEmpty() ? null
                : heartRates.stream().mapToInt(Integer::intValue).max().orElseThrow();

        return new ParsedActivity(
                activityType,
                startTime,
                durationSeconds,
                distanceMeters,
                avgHr,
                maxHr,
                null,
                elevationGainMeters,
                trackPoints
        );
    }

    private double computeDistanceMeters(List<ParsedTrackPoint> points) {
        double total = 0.0;
        ParsedTrackPoint previous = null;
        for (ParsedTrackPoint point : points) {
            if (previous != null && previous.lat() != null && previous.lon() != null
                    && point.lat() != null && point.lon() != null) {
                total += haversineMeters(previous.lat(), previous.lon(), point.lat(), point.lon());
            }
            previous = point;
        }
        return total;
    }

    private double computeElevationGainMeters(List<ParsedTrackPoint> points) {
        double gain = 0.0;
        Double previous = null;
        for (ParsedTrackPoint point : points) {
            Double elevation = point.elevationMeters();
            if (previous != null && elevation != null) {
                double delta = elevation - previous;
                if (delta > 0) {
                    gain += delta;
                }
            }
            if (elevation != null) {
                previous = elevation;
            }
        }
        return gain;
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Heart rate typically lives in the Garmin TrackPointExtension namespace
     * ({@code gpxtpx:hr}) inside {@code <trkpt><extensions>}. Its absence is
     * normal (older/simpler GPX exports) and must not fail parsing.
     */
    private Integer extractHeartRate(WayPoint wayPoint) {
        return wayPoint.getExtensions()
                .map(this::findHrValue)
                .orElse(null);
    }

    private Integer findHrValue(Document extensions) {
        NodeList matches = extensions.getElementsByTagNameNS("*", "hr");
        if (matches.getLength() == 0) {
            return null;
        }
        Node node = matches.item(0);
        String text = node.getTextContent();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
