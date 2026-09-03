package com.mysportsapp.activity;

import com.mysportsapp.provider.spi.ParsedTrackPoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A single tenant-owned activity (a run, a ride, ...) produced by importing
 * a file through some {@link com.mysportsapp.provider.spi.DataProvider}.
 *
 * <p>Deliberately has no JPA relationships to {@code User} or {@code
 * ImportBatch} - {@code userId} and {@code sourceImportBatchId} are plain
 * columns. Every tenant-scoped query goes through {@link ActivityRepository}
 * methods that take the acting user's id explicitly; there is no bare
 * {@code findById} used anywhere in service code (enforced by
 * {@code TenantScopingArchTest}).
 */
@Entity
@Table(name = "activities")
public class Activity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source_provider_id", nullable = false)
    private String sourceProviderId;

    @Column(name = "source_import_batch_id", nullable = false)
    private UUID sourceImportBatchId;

    @Column(name = "activity_type", nullable = false)
    private String activityType;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "avg_hr")
    private Integer avgHr;

    @Column(name = "max_hr")
    private Integer maxHr;

    @Column(name = "calories")
    private Integer calories;

    @Column(name = "elevation_gain_meters")
    private Double elevationGainMeters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "track_points", nullable = false, columnDefinition = "jsonb")
    private List<ParsedTrackPoint> trackPoints;

    @Column(name = "dedup_key", nullable = false)
    private String dedupKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Activity() {
        // JPA
    }

    public Activity(UUID id, UUID userId, String sourceProviderId, UUID sourceImportBatchId,
                     String activityType, Instant startTime, long durationSeconds, Double distanceMeters,
                     Integer avgHr, Integer maxHr, Integer calories, Double elevationGainMeters,
                     List<ParsedTrackPoint> trackPoints, String dedupKey, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.sourceProviderId = sourceProviderId;
        this.sourceImportBatchId = sourceImportBatchId;
        this.activityType = activityType;
        this.startTime = startTime;
        this.durationSeconds = durationSeconds;
        this.distanceMeters = distanceMeters;
        this.avgHr = avgHr;
        this.maxHr = maxHr;
        this.calories = calories;
        this.elevationGainMeters = elevationGainMeters;
        this.trackPoints = trackPoints;
        this.dedupKey = dedupKey;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSourceProviderId() {
        return sourceProviderId;
    }

    public UUID getSourceImportBatchId() {
        return sourceImportBatchId;
    }

    public String getActivityType() {
        return activityType;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public Integer getAvgHr() {
        return avgHr;
    }

    public Integer getMaxHr() {
        return maxHr;
    }

    public Integer getCalories() {
        return calories;
    }

    public Double getElevationGainMeters() {
        return elevationGainMeters;
    }

    public List<ParsedTrackPoint> getTrackPoints() {
        return trackPoints;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
