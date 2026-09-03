package com.mysportsapp.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysportsapp.security.JwtService;
import com.mysportsapp.support.AbstractIntegrationTest;
import com.mysportsapp.user.User;
import com.mysportsapp.user.UserRepository;
import com.mysportsapp.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginSucceedsWithCorrectCredentials() throws Exception {
        String password = "correct-horse-battery-staple";
        User user = new User(UUID.randomUUID(), "login-ok+" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode(password), UserRole.USER, true, null, null, null, Instant.now());
        userRepository.save(user);

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("token").asText()).isNotBlank();
        assertThat(json.get("user").get("email").asText()).isEqualTo(user.getEmail());
        assertThat(json.get("user").get("role").asText()).isEqualTo("USER");
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        User user = new User(UUID.randomUUID(), "login-bad+" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("the-real-password"), UserRole.USER, true, null, null, null, Instant.now());
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginFailsForUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminInviteAcceptInviteThenLoginFlow() throws Exception {
        User admin = new User(UUID.randomUUID(), "admin+" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("admin-password"), UserRole.ADMIN, true, null, null, null, Instant.now());
        userRepository.save(admin);
        String adminToken = jwtService.issueToken(admin.getId(), admin.getEmail(), admin.getRole());

        String invitedEmail = "invitee+" + UUID.randomUUID() + "@example.com";

        String inviteBody = mockMvc.perform(post("/api/v1/admin/invite")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"email\":\"" + invitedEmail + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode inviteJson = objectMapper.readTree(inviteBody);
        assertThat(inviteJson.get("email").asText()).isEqualTo(invitedEmail);
        String inviteToken = inviteJson.get("inviteToken").asText();
        assertThat(inviteToken).isNotBlank();

        String newPassword = "a-brand-new-password";
        String acceptBody = mockMvc.perform(post("/api/v1/auth/accept-invite")
                        .contentType("application/json")
                        .content("{\"inviteToken\":\"" + inviteToken + "\",\"password\":\"" + newPassword + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode acceptJson = objectMapper.readTree(acceptBody);
        assertThat(acceptJson.get("user").get("email").asText()).isEqualTo(invitedEmail);
        String invitedUserId = acceptJson.get("user").get("id").asText();

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + invitedEmail + "\",\"password\":\"" + newPassword + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginBody);
        assertThat(loginJson.get("user").get("id").asText()).isEqualTo(invitedUserId);
    }

    @Test
    void acceptInviteFailsForUnknownToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/accept-invite")
                        .contentType("application/json")
                        .content("{\"inviteToken\":\"not-a-real-token\",\"password\":\"whatever123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminCannotInvite() throws Exception {
        User regularUser = new User(UUID.randomUUID(), "regular+" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("password123"), UserRole.USER, true, null, null, null, Instant.now());
        userRepository.save(regularUser);
        String token = jwtService.issueToken(regularUser.getId(), regularUser.getEmail(), regularUser.getRole());

        mockMvc.perform(post("/api/v1/admin/invite")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"email\":\"someone@example.com\"}"))
                .andExpect(status().isForbidden());
    }
}
