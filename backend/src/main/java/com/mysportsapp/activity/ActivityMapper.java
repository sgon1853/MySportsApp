package com.mysportsapp.activity;

import com.mysportsapp.activity.dto.ActivityDetailDto;
import com.mysportsapp.activity.dto.ActivitySummaryDto;
import com.mysportsapp.activity.dto.TrackPointDto;
import com.mysportsapp.provider.spi.ParsedTrackPoint;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ActivityMapper {

    private final VisualizationTypeResolver visualizationTypeResolver;

    public ActivityMapper(VisualizationTypeResolver visualizationTypeResolver) {
        this.visualizationTypeResolver = visualizationTypeResolver;
    }

    public ActivitySummaryDto toSummaryDto(Activity activity) {
        return new ActivitySummaryDto(
                activity.getId(),
                activity.getActivityType(),
                visualizationType(),
                activity.getStartTime(),
                activity.getDurationSeconds(),
                activity.getDistanceMeters(),
                activity.getAvgHr(),
                activity.getMaxHr(),
                activity.getCalories(),
                activity.getElevationGainMeters(),
                activity.getSourceProviderId()
        );
    }

    public ActivityDetailDto toDetailDto(Activity activity) {
        return new ActivityDetailDto(
                activity.getId(),
                activity.getActivityType(),
                visualizationType(),
                activity.getStartTime(),
                activity.getDurationSeconds(),
                activity.getDistanceMeters(),
                activity.getAvgHr(),
                activity.getMaxHr(),
                activity.getCalories(),
                activity.getElevationGainMeters(),
                activity.getSourceProviderId(),
                toTrackPointDtos(activity.getTrackPoints())
        );
    }

    private String visualizationType() {
        return visualizationTypeResolver.resolve(VisualizationTypeResolver.RecordKind.ACTIVITY);
    }

    private List<TrackPointDto> toTrackPointDtos(List<ParsedTrackPoint> trackPoints) {
        return trackPoints.stream()
                .map(tp -> new TrackPointDto(tp.timestamp(), tp.lat(), tp.lon(), tp.elevationMeters(), tp.heartRate()))
                .toList();
    }
}
