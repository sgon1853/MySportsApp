package com.mysportsapp.imports;

import com.mysportsapp.activity.Activity;
import com.mysportsapp.activity.ActivityRepository;
import com.mysportsapp.security.AuthenticatedUser;
import com.mysportsapp.support.AbstractIntegrationTest;
import com.mysportsapp.user.User;
import com.mysportsapp.user.UserRepository;
import com.mysportsapp.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ImportServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ImportService importService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    private User testUser;

    @BeforeEach
    void setUpUser() {
        testUser = new User(
                UUID.randomUUID(), "runner+" + UUID.randomUUID() + "@example.com",
                "irrelevant-hash", UserRole.USER, true, null, null, null, Instant.now());
        userRepository.save(testUser);
        authenticateAs(testUser);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void firstImportInsertsActivityAndSecondImportOfSameFileIsFullyDeduped() throws IOException {
        byte[] content = readFixture("sample-run.gpx");

        ImportService.Outcome first = importService.importFile("suunto-gpx", "sample-run.gpx", content);

        assertThat(first.hardParseFailure()).isFalse();
        assertThat(first.result().status()).isEqualTo("SUCCESS");
        assertThat(first.result().recordsParsed()).isEqualTo(1);
        assertThat(first.result().recordsInserted()).isEqualTo(1);
        assertThat(first.result().recordsDeduped()).isEqualTo(0);
        assertThat(first.result().recordsFailed()).isEqualTo(0);

        List<Activity> activitiesAfterFirstImport = activityRepository.findAll().stream()
                .filter(a -> a.getUserId().equals(testUser.getId()))
                .toList();
        assertThat(activitiesAfterFirstImport).hasSize(1);

        Activity saved = activitiesAfterFirstImport.get(0);
        assertThat(saved.getActivityType()).isEqualTo("RUNNING");
        assertThat(saved.getSourceProviderId()).isEqualTo("suunto-gpx");
        assertThat(saved.getSourceImportBatchId().toString()).isEqualTo(first.result().batchId());
        assertThat(saved.getDurationSeconds()).isEqualTo(330L);
        assertThat(saved.getTrackPoints()).hasSize(12);

        // Re-uploading the exact same file must be fully deduped: nothing new inserted.
        ImportService.Outcome second = importService.importFile("suunto-gpx", "sample-run.gpx", content);

        assertThat(second.hardParseFailure()).isFalse();
        // A fully-deduped re-upload is expected behavior, not a failure.
        assertThat(second.result().status()).isEqualTo("SUCCESS");
        assertThat(second.result().recordsParsed()).isEqualTo(1);
        assertThat(second.result().recordsInserted()).isEqualTo(0);
        assertThat(second.result().recordsDeduped()).isEqualTo(1);

        List<Activity> activitiesAfterSecondImport = activityRepository.findAll().stream()
                .filter(a -> a.getUserId().equals(testUser.getId()))
                .toList();
        assertThat(activitiesAfterSecondImport).hasSize(1);
        assertThat(activitiesAfterSecondImport.get(0).getId()).isEqualTo(saved.getId());
    }

    @Test
    void unparsableFileFailsTheBatchWithoutThrowing() {
        byte[] garbage = "this is not a gpx file".getBytes();

        ImportService.Outcome outcome = importService.importFile("suunto-gpx", "garbage.gpx", garbage);

        assertThat(outcome.hardParseFailure()).isTrue();
        assertThat(outcome.result().status()).isEqualTo("FAILED");
        assertThat(outcome.result().recordsParsed()).isEqualTo(0);
        assertThat(outcome.result().errors()).isNotEmpty();
    }

    private byte[] readFixture(String name) throws IOException {
        try (var in = getClass().getResourceAsStream("/fixtures/gpx/" + name)) {
            assertThat(in).isNotNull();
            return in.readAllBytes();
        }
    }

    private void authenticateAs(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
