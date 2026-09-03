package com.mysportsapp.dedup;

import com.mysportsapp.activity.ActivityRepository;
import com.mysportsapp.provider.spi.ParsedActivity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Splits a batch of freshly-parsed activities into "new" (not seen before
 * for this user) and "duplicate" (already imported, or repeated within the
 * same file). Read-only - it never writes to the database; that's
 * {@code ImportService}'s job once it decides what to persist.
 */
@Component
public class DedupService {

    private final DedupKeyGenerator dedupKeyGenerator;
    private final ActivityRepository activityRepository;

    public DedupService(DedupKeyGenerator dedupKeyGenerator, ActivityRepository activityRepository) {
        this.dedupKeyGenerator = dedupKeyGenerator;
        this.activityRepository = activityRepository;
    }

    public DedupResult split(UUID userId, List<ParsedActivity> activities) {
        List<Keyed> newActivities = new ArrayList<>();
        Set<String> keysSeenInThisBatch = new HashSet<>();
        int duplicateCount = 0;

        for (ParsedActivity activity : activities) {
            String key = dedupKeyGenerator.generate(
                    userId, activity.activityType(), activity.startTime(), activity.durationSeconds());

            boolean duplicateWithinBatch = !keysSeenInThisBatch.add(key);
            boolean duplicateInDatabase = activityRepository.existsByUserIdAndDedupKey(userId, key);

            if (duplicateWithinBatch || duplicateInDatabase) {
                duplicateCount++;
                continue;
            }
            newActivities.add(new Keyed(activity, key));
        }

        return new DedupResult(List.copyOf(newActivities), duplicateCount);
    }

    public record Keyed(ParsedActivity activity, String dedupKey) {
    }

    public record DedupResult(List<Keyed> newActivities, int duplicateCount) {
    }
}
