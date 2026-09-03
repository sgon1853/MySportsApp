package com.mysportsapp.config;

import com.mysportsapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Browsers send an {@code Origin} header on POST/PUT/DELETE requests even
 * when the request is same-origin (not just on genuinely cross-origin
 * calls) - including requests proxied same-origin through nginx, as in
 * docker-compose/e2e runs. Spring's CORS filter inspects that header on
 * every request regardless of whether it's "really" cross-origin, so the
 * default allowed-origin-patterns must cover the local ports docker-compose
 * publishes the frontend on, or the filter rejects the request with 403
 * before it ever reaches the controller. This regression-tests exactly the
 * failure mode that broke the e2e suite: a same-origin-proxied login POST
 * carrying an Origin header matching the default local ports.
 */
class CorsConfigurationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginSucceedsWithOriginHeaderMatchingDockerComposePublishedPort() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "http://localhost:8081")
                        .contentType("application/json")
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"whatever\"}"))
                // Wrong credentials -> 401 from our own auth logic, proving the
                // request reached the controller instead of being rejected by
                // the CORS filter first (which would be a 403 with no JSON body).
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSucceedsWithOriginHeaderMatchingViteDevServerPort() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .contentType("application/json")
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized());
    }
}
