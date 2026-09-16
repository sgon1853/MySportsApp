package com.mysportsapp.integrations.strava.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response shape of {@code GET /activities/{id}/streams?...&key_by_type=true}
 * - a map keyed by stream type rather than an array (that's what
 * {@code key_by_type=true} buys us: fields we can bind directly instead of
 * scanning an array for the type we want). Every field is nullable - Strava
 * only includes a stream if that activity actually has that data (an indoor
 * trainer ride has no {@code latlng}; an activity recorded without a heart
 * rate strap has no {@code heartrate}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StravaStreamSet(
        LatLngStream latlng,
        ValueStream altitude,
        ValueStream heartrate,
        ValueStream time
) {

    /** Each element is a {@code [lat, lng]} pair. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LatLngStream(List<List<Double>> data) {
    }

    /** {@code time}'s values are whole seconds elapsed since the activity's
     * start; {@code altitude}/{@code heartrate}'s are the corresponding
     * measurement at that same offset - same length, same index alignment. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValueStream(List<Double> data) {
    }
}
