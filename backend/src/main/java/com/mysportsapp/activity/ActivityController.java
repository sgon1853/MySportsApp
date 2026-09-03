package com.mysportsapp.activity;

import com.mysportsapp.activity.dto.ActivityDetailDto;
import com.mysportsapp.activity.dto.ActivitySummaryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ResponseEntity<List<ActivitySummaryDto>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant fromInstant = parseInstant(from, "from");
        Instant toInstant = parseInstant(to, "to");
        return ResponseEntity.ok(activityService.list(type, fromInstant, toInstant));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityDetailDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(activityService.get(id));
    }

    private Instant parseInstant(String value, String paramName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid ISO-8601 timestamp for '" + paramName + "': " + value);
        }
    }
}
