package com.mysportsapp.activity;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Maps a persisted record's "kind" to the visualization the frontend should
 * render for it. Today there is only one kind of tenant-owned record
 * (GPS-track activities), but this is written as a lookup - not a single
 * hardcoded return - so that adding a dive computer or a smart scale
 * provider later is a one-line addition here (e.g. {@code DIVE_LOG ->
 * "DIVE_PROFILE"}), not a rewrite.
 */
@Component
public class VisualizationTypeResolver {

    public enum RecordKind {
        ACTIVITY
    }

    private static final String UNKNOWN_VISUALIZATION = "UNKNOWN";

    private final Map<RecordKind, String> visualizationTypesByKind;

    public VisualizationTypeResolver() {
        this.visualizationTypesByKind = new EnumMap<>(RecordKind.class);
        this.visualizationTypesByKind.put(RecordKind.ACTIVITY, "GPS_TRACK");
    }

    public String resolve(RecordKind kind) {
        return visualizationTypesByKind.getOrDefault(kind, UNKNOWN_VISUALIZATION);
    }
}
