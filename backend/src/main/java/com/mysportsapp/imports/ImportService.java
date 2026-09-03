package com.mysportsapp.imports;

import com.mysportsapp.activity.Activity;
import com.mysportsapp.activity.ActivityRepository;
import com.mysportsapp.common.exception.UnknownProviderException;
import com.mysportsapp.dedup.DedupService;
import com.mysportsapp.imports.dto.ImportBatchResultDto;
import com.mysportsapp.provider.DataProviderRegistry;
import com.mysportsapp.provider.spi.DataProvider;
import com.mysportsapp.provider.spi.ImportContext;
import com.mysportsapp.provider.spi.ParseResult;
import com.mysportsapp.provider.spi.ParsedActivity;
import com.mysportsapp.provider.spi.ProviderParseException;
import com.mysportsapp.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates a single file upload end to end: resolve the provider, parse
 * the file, dedupe against what this user has already imported, persist
 * what's new, and record the outcome on an {@link ImportBatch}.
 */
@Service
public class ImportService {

    private final DataProviderRegistry providerRegistry;
    private final DedupService dedupService;
    private final ActivityRepository activityRepository;
    private final ImportBatchRepository importBatchRepository;

    public ImportService(DataProviderRegistry providerRegistry, DedupService dedupService,
                          ActivityRepository activityRepository, ImportBatchRepository importBatchRepository) {
        this.providerRegistry = providerRegistry;
        this.dedupService = dedupService;
        this.activityRepository = activityRepository;
        this.importBatchRepository = importBatchRepository;
    }

    /**
     * @return the outcome of the import. {@link Outcome#hardParseFailure()} is
     * true only when the file could not be parsed at all (the caller should
     * respond 422); any other outcome, including a batch where every record
     * turned out to be a duplicate, is a normal 200 with status SUCCESS.
     */
    @Transactional
    public Outcome importFile(String providerId, String originalFilename, byte[] content) {
        UUID userId = CurrentUser.get().id();

        DataProvider provider = providerRegistry.findById(providerId)
                .orElseThrow(() -> new UnknownProviderException(providerId));

        ImportBatch batch = ImportBatch.pending(UUID.randomUUID(), userId, providerId, originalFilename);
        importBatchRepository.save(batch);

        ParseResult parseResult;
        try {
            ImportContext context = new ImportContext(userId, originalFilename, batch.getId());
            parseResult = provider.parse(new ByteArrayInputStream(content), context);
        } catch (ProviderParseException e) {
            batch.complete(ImportStatus.FAILED, 0, 0, 0, 0, List.of(e.getMessage()));
            importBatchRepository.save(batch);
            return new Outcome(toDto(batch), true);
        }

        List<ParsedActivity> parsedActivities = parseResult.activities();
        int recordsParsed = parsedActivities.size();

        DedupService.DedupResult dedupResult = dedupService.split(userId, parsedActivities);

        Instant now = Instant.now();
        List<Activity> toInsert = dedupResult.newActivities().stream()
                .map(keyed -> toEntity(keyed, userId, providerId, batch.getId(), now))
                .toList();
        activityRepository.saveAll(toInsert);

        int recordsInserted = toInsert.size();
        int recordsDeduped = dedupResult.duplicateCount();
        int recordsFailed = 0;

        ImportStatus status = resolveStatus(recordsFailed, recordsParsed);
        batch.complete(status, recordsParsed, recordsInserted, recordsDeduped, recordsFailed, List.of());
        importBatchRepository.save(batch);

        return new Outcome(toDto(batch), false);
    }

    /**
     * FAILED is reserved for "the file itself couldn't be parsed" (handled
     * separately, in the catch block above) and for a batch where every
     * parsed record individually failed to map/validate. A batch where
     * every record was successfully accounted for - whether newly inserted
     * or skipped as a duplicate of something already stored - is a normal
     * SUCCESS: re-uploading a file you already imported is expected
     * behavior, not a failure.
     */
    private ImportStatus resolveStatus(int recordsFailed, int recordsParsed) {
        if (recordsFailed == 0) {
            return ImportStatus.SUCCESS;
        }
        if (recordsFailed == recordsParsed) {
            return ImportStatus.FAILED;
        }
        return ImportStatus.PARTIAL;
    }

    private Activity toEntity(DedupService.Keyed keyed, UUID userId, String providerId, UUID batchId, Instant now) {
        ParsedActivity activity = keyed.activity();
        return new Activity(
                UUID.randomUUID(),
                userId,
                providerId,
                batchId,
                activity.activityType(),
                activity.startTime(),
                activity.durationSeconds(),
                activity.distanceMeters(),
                activity.avgHr(),
                activity.maxHr(),
                activity.calories(),
                activity.elevationGainMeters(),
                activity.trackPoints(),
                keyed.dedupKey(),
                now
        );
    }

    private ImportBatchResultDto toDto(ImportBatch batch) {
        return new ImportBatchResultDto(
                batch.getId().toString(),
                batch.getProviderId(),
                batch.getStatus().name(),
                batch.getRecordsParsed(),
                batch.getRecordsInserted(),
                batch.getRecordsDeduped(),
                batch.getRecordsFailed(),
                batch.getErrors()
        );
    }

    public record Outcome(ImportBatchResultDto result, boolean hardParseFailure) {
    }
}
