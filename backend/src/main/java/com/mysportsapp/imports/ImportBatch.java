package com.mysportsapp.imports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Records the outcome of a single file upload through the import pipeline.
 */
@Entity
@Table(name = "import_batches")
public class ImportBatch {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status;

    @Column(name = "records_parsed", nullable = false)
    private int recordsParsed;

    @Column(name = "records_inserted", nullable = false)
    private int recordsInserted;

    @Column(name = "records_deduped", nullable = false)
    private int recordsDeduped;

    @Column(name = "records_failed", nullable = false)
    private int recordsFailed;

    @Column(name = "error_details")
    private String errorDetails;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ImportBatch() {
        // JPA
    }

    public static ImportBatch pending(UUID id, UUID userId, String providerId, String originalFilename) {
        ImportBatch batch = new ImportBatch();
        batch.id = id;
        batch.userId = userId;
        batch.providerId = providerId;
        batch.originalFilename = originalFilename;
        batch.status = ImportStatus.PENDING;
        batch.recordsParsed = 0;
        batch.recordsInserted = 0;
        batch.recordsDeduped = 0;
        batch.recordsFailed = 0;
        batch.createdAt = Instant.now();
        return batch;
    }

    /**
     * Records the final outcome of processing this batch.
     */
    public void complete(ImportStatus status, int recordsParsed, int recordsInserted,
                          int recordsDeduped, int recordsFailed, List<String> errors) {
        this.status = status;
        this.recordsParsed = recordsParsed;
        this.recordsInserted = recordsInserted;
        this.recordsDeduped = recordsDeduped;
        this.recordsFailed = recordsFailed;
        this.errorDetails = (errors == null || errors.isEmpty()) ? null : String.join("\n", errors);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public int getRecordsParsed() {
        return recordsParsed;
    }

    public int getRecordsInserted() {
        return recordsInserted;
    }

    public int getRecordsDeduped() {
        return recordsDeduped;
    }

    public int getRecordsFailed() {
        return recordsFailed;
    }

    public List<String> getErrors() {
        return (errorDetails == null || errorDetails.isBlank())
                ? List.of()
                : List.of(errorDetails.split("\n"));
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
