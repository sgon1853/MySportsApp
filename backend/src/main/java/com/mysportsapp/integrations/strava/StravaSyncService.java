package com.mysportsapp.integrations.strava;

import com.mysportsapp.imports.ImportBatch;
import com.mysportsapp.imports.ImportBatchRepository;
import com.mysportsapp.imports.ImportService;
import com.mysportsapp.integrations.strava.dto.StravaStreamSet;
import com.mysportsapp.integrations.strava.dto.StravaSummaryActivity;
import com.mysportsapp.provider.spi.ParsedActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates an on-demand Strava backfill: page through the athlete's full
 * activity history, fetch each activity's track data, map everything to the
 * same {@link ParsedActivity} shape a GPX file would produce, and persist it
 * all as one {@link ImportBatch} via {@link ImportService#persistParsedActivities}
 * - the same dedup/persist path file uploads use, so re-running a sync is
 * exactly as safe as re-uploading a file (already-imported activities are
 * recognized as duplicates, not re-inserted).
 */
@Service
public class StravaSyncService {

    private static final Logger log = LoggerFactory.getLogger(StravaSyncService.class);
    private static final String PROVIDER_ID = "strava";
    // A generous ceiling, not an expected real value - guards against an
    // infinite loop if Strava's pagination ever behaves unexpectedly, not a
    // real limit on how much history an account can have.
    private static final int MAX_PAGES = 50;

    private final StravaConnectionRepository connectionRepository;
    private final StravaApiClient apiClient;
    private final StravaActivityMapper mapper;
    private final ImportService importService;
    private final ImportBatchRepository importBatchRepository;

    public StravaSyncService(StravaConnectionRepository connectionRepository, StravaApiClient apiClient,
                              StravaActivityMapper mapper, ImportService importService,
                              ImportBatchRepository importBatchRepository) {
        this.connectionRepository = connectionRepository;
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.importService = importService;
        this.importBatchRepository = importBatchRepository;
    }

    @Transactional
    public ImportService.Outcome sync(UUID userId) {
        StravaConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(StravaNotConnectedException::new);

        ImportBatch batch = ImportBatch.pending(UUID.randomUUID(), userId, PROVIDER_ID, "strava-sync");
        importBatchRepository.save(batch);

        List<ParsedActivity> parsedActivities = fetchAllActivities(connection);

        return importService.persistParsedActivities(batch, userId, PROVIDER_ID, parsedActivities);
    }

    private List<ParsedActivity> fetchAllActivities(StravaConnection connection) {
        List<ParsedActivity> parsedActivities = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            List<StravaSummaryActivity> pageActivities;
            try {
                pageActivities = apiClient.listActivities(connection, page);
            } catch (HttpClientErrorException.TooManyRequests e) {
                // Stop gracefully rather than fail the whole sync: whatever
                // was fetched on earlier pages is still persisted below. A
                // later sync naturally picks up anything missed, since
                // dedup means re-fetching already-imported activities is
                // harmless.
                log.warn("Strava rate limit hit while listing activities on page {} - stopping this sync early "
                        + "with {} activities already fetched", page, parsedActivities.size());
                break;
            }

            if (pageActivities.isEmpty()) {
                break;
            }

            for (StravaSummaryActivity summary : pageActivities) {
                parsedActivities.add(mapper.toParsedActivity(summary, fetchStreamsOrEmpty(connection, summary)));
            }
        }

        return parsedActivities;
    }

    /** A single activity's streams failing (rate limit, transient error) isn't
     * worth losing the whole sync over - the activity still gets imported
     * with its summary stats, just without a GPS track/HR-over-time chart. */
    private StravaStreamSet fetchStreamsOrEmpty(StravaConnection connection, StravaSummaryActivity summary) {
        try {
            return apiClient.getStreams(connection, summary.id());
        } catch (RestClientException e) {
            log.warn("Could not fetch streams for Strava activity {} - importing its summary without a track",
                    summary.id(), e);
            return new StravaStreamSet(null, null, null, null);
        }
    }
}
