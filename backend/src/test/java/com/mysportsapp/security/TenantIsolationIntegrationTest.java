package com.mysportsapp.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysportsapp.support.AbstractIntegrationTest;
import com.mysportsapp.user.User;
import com.mysportsapp.user.UserRepository;
import com.mysportsapp.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end proof that one tenant can never read another tenant's data,
 * exercised through the real HTTP stack (JwtAuthenticationFilter,
 * SecurityConfig, controllers) rather than by calling services directly.
 */
class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userBCannotReadUserAsActivity() throws Exception {
        User userA = createActiveUser();
        User userB = createActiveUser();

        String tokenA = jwtService.issueToken(userA.getId(), userA.getEmail(), userA.getRole());
        String tokenB = jwtService.issueToken(userB.getId(), userB.getEmail(), userB.getRole());

        // Import an activity as user A over the real HTTP stack.
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample-run.gpx", "application/gpx+xml", readFixture("sample-run.gpx"));

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("providerId", "suunto-gpx")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Fetch the id of the activity we just created, as user A.
        String listBody = mockMvc.perform(get("/api/v1/activities")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode activities = objectMapper.readTree(listBody);
        assertThat(activities).hasSize(1);
        String activityId = activities.get(0).get("id").asText();

        // User A can read their own activity.
        mockMvc.perform(get("/api/v1/activities/" + activityId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // User B must NOT be able to read user A's activity - 404, not 403,
        // so the response never confirms the row exists at all.
        mockMvc.perform(get("/api/v1/activities/" + activityId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/activities"))
                .andExpect(status().isUnauthorized());
    }

    private User createActiveUser() {
        User user = new User(
                UUID.randomUUID(), "tenant+" + UUID.randomUUID() + "@example.com",
                "irrelevant-hash", UserRole.USER, true, null, null, null, Instant.now());
        return userRepository.save(user);
    }

    private byte[] readFixture(String name) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/gpx/" + name)) {
            assertThat(in).isNotNull();
            return in.readAllBytes();
        }
    }
}
