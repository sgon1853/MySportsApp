package com.mysportsapp.config;

import com.mysportsapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code /actuator/health} must stay reachable with no credentials: Docker's
 * healthcheck, Cloud Run's readiness probe, and deploy-time smoke tests all
 * call it unauthenticated. This regression-tests a real bug where it was
 * accidentally caught by the default "authenticate everything" rule.
 */
class HealthEndpointIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
