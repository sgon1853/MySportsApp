package com.mysportsapp.activity;

import com.mysportsapp.activity.dto.ActivityDetailDto;
import com.mysportsapp.activity.dto.ActivitySummaryDto;
import com.mysportsapp.common.exception.NotFoundException;
import com.mysportsapp.security.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;

    public ActivityService(ActivityRepository activityRepository, ActivityMapper activityMapper) {
        this.activityRepository = activityRepository;
        this.activityMapper = activityMapper;
    }

    @Transactional(readOnly = true)
    public List<ActivitySummaryDto> list(String activityType, Instant from, Instant to) {
        UUID userId = CurrentUser.get().id();
        var spec = ActivitySpecifications.search(userId, activityType, from, to);
        return activityRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "startTime")).stream()
                .map(activityMapper::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ActivityDetailDto get(UUID activityId) {
        UUID userId = CurrentUser.get().id();
        // Scoped by userId so a request for another tenant's activity is
        // indistinguishable from a request for one that doesn't exist.
        Activity activity = activityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new NotFoundException("Activity not found"));
        return activityMapper.toDetailDto(activity);
    }
}
