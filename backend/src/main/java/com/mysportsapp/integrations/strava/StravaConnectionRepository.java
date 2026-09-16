package com.mysportsapp.integrations.strava;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Tenant-scoped the same way every other repository in this app is: no bare
 * {@code findById} is used from service code, only lookups keyed by the
 * acting user's id (or, for the future webhook phase, the Strava athlete id
 * a webhook event carries - {@code findByStravaAthleteId} exists now even
 * though nothing calls it yet, since the column already exists).
 */
public interface StravaConnectionRepository extends JpaRepository<StravaConnection, UUID> {

    Optional<StravaConnection> findByUserId(UUID userId);

    Optional<StravaConnection> findByStravaAthleteId(long stravaAthleteId);
}
