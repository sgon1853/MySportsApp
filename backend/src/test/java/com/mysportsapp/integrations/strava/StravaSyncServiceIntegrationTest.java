package com.mysportsapp.integrations.strava;

import com.mysportsapp.activity.Activity;
import com.mysportsapp.activity.ActivityRepository;
import com.mysportsapp.imports.ImportService;
import com.mysportsapp.integrations.strava.dto.StravaStreamSet;
import com.mysportsapp.integrations.strava.dto.StravaSummaryActivity;
import com.mysportsapp.support.AbstractIntegrationTest;
import com.mysportsapp.user.User;
import com.mysportsapp.user.UserRepository;
import com.mysportsapp.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link StravaApiClient} is mocked - this test is about proving the sync
 * orchestration and dedup/persist path work, not about hitting the real
 * Strava API (that's the "real verification" step against a real connected
 * account, done separately, not something an automated test can do at all).
 */
class StravaSyncServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StravaSyncService syncService;

    @Autowired
    private StravaConnectionRepository connectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @MockBean
    private StravaApiClient apiClient;

    private User testUser;

    @BeforeEach
    void setUpUserAndConnection() {
        testUser = new User(UUID.randomUUID(), "strava-sync+" + UUID.randomUUID() + "@example.com",
                "irrelevant-hash", UserRole.USER, true, null, null, null, Instant.now());
        userRepository.save(testUser);

        // A random athlete id per test run, not a fixed constant - this class'
        // two @Test methods share one Testcontainers Postgres instance across
        // the whole run (see AbstractIntegrationTest), so a fixed value would
        // collide with strava_athlete_id's unique constraint on the second
        // test's @BeforeEach.
        long randomAthleteId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        StravaConnection connection = new StravaConnection(
                UUID.randomUUID(), testUser.getId(), randomAthleteId,
                "fake-access-token", "fake-refresh-token", Instant.now().plusSeconds(3600), Instant.now());
        connectionRepository.save(connection);
    }

    @Test
    void syncFetchesAllPagesAndPersistsThroughTheSameDedupPathAsFileUploads() {
        StravaSummaryActivity page1Activity = new StravaSummaryActivity(
                111L, "Morning Run", "Run", Instant.parse("2026-01-01T07:00:00Z"),
                1800L, 5000.0, 10.0, 130.0, 150.0);

        when(apiClient.listActivities(any(), eq(1))).thenReturn(List.of(page1Activity));
        when(apiClient.listActivities(any(), eq(2))).thenReturn(List.of());
        when(apiClient.getStreams(any(), anyLong())).thenReturn(new StravaStreamSet(null, null, null, null));

        ImportService.Outcome outcome = syncService.sync(testUser.getId());

        assertThat(outcome.result().status()).isEqualTo("SUCCESS");
        assertThat(outcome.result().recordsParsed()).isEqualTo(1);
        assertThat(outcome.result().recordsInserted()).isEqualTo(1);

        List<Activity> saved = activityRepository.findAll().stream()
                .filter(a -> a.getUserId().equals(testUser.getId()))
                .toList();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getSourceProviderId()).isEqualTo("strava");
        assertThat(saved.get(0).getActivityType()).isEqualTo("RUNNING");

        // Re-syncing must not duplicate: same activities come back from the
        // (still mocked) Strava API, and the existing dedup key catches them.
        ImportService.Outcome secondOutcome = syncService.sync(testUser.getId());
        assertThat(secondOutcome.result().status()).isEqualTo("SUCCESS");
        assertThat(secondOutcome.result().recordsInserted()).isEqualTo(0);
        assertThat(secondOutcome.result().recordsDeduped()).isEqualTo(1);

        List<Activity> afterResync = activityRepository.findAll().stream()
                .filter(a -> a.getUserId().equals(testUser.getId()))
                .toList();
        assertThat(afterResync).hasSize(1);
    }

    @Test
    void syncWithoutAConnectionFailsClearly() {
        User unconnectedUser = new User(UUID.randomUUID(), "no-strava+" + UUID.randomUUID() + "@example.com",
                "irrelevant-hash", UserRole.USER, true, null, null, null, Instant.now());
        userRepository.save(unconnectedUser);

        assertThatThrownBy(() -> syncService.sync(unconnectedUser.getId()))
                .isInstanceOf(StravaNotConnectedException.class);
    }
}
