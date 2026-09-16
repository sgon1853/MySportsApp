package com.mysportsapp.integrations.strava.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * One entry from {@code GET /athlete/activities} - Strava's "summary"
 * representation. Deliberately doesn't carry track points (Strava only
 * includes an encoded polyline summary here, not the real time series - see
 * {@link StravaStreamSet} for that) or calories (only present on the
 * "detailed" single-activity representation, which Phase 1 doesn't fetch -
 * see StravaActivityMapper for why that trade-off is fine here).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StravaSummaryActivity(
        long id,
        String name,
        /** Strava's coarser, stable classification (e.g. "Run", "Ride") - used over the
         * more granular {@code sport_type} (e.g. "TrailRun") since the mapping to this
         * app's activity-type vocabulary only needs to be coarse. */
        String type,
        @JsonProperty("start_date") Instant startDate,
        @JsonProperty("elapsed_time") long elapsedTimeSeconds,
        Double distance,
        @JsonProperty("total_elevation_gain") Double totalElevationGain,
        @JsonProperty("average_heartrate") Double averageHeartrate,
        @JsonProperty("max_heartrate") Double maxHeartrate
) {
}
