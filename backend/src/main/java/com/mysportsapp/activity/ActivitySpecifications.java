package com.mysportsapp.activity;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

/**
 * Builds the filter predicate for {@code GET /api/v1/activities} one clause
 * at a time, adding a predicate only for filters that are actually present.
 * See {@link ActivityRepository} for why this replaced a hand-written JPQL
 * query with optional {@code (:param IS NULL OR ...)} clauses.
 */
final class ActivitySpecifications {

    private ActivitySpecifications() {
    }

    static Specification<Activity> search(UUID userId, String activityType, Instant from, Instant to) {
        return (root, query, cb) -> {
            var predicate = cb.equal(root.get("userId"), userId);
            if (activityType != null) {
                predicate = cb.and(predicate, cb.equal(root.get("activityType"), activityType));
            }
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("startTime"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("startTime"), to));
            }
            return predicate;
        };
    }
}
