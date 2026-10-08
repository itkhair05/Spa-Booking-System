package com.example.spabooking.observability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class HealthAndObservabilityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("1. Actuator health endpoint is publicly accessible without authentication and reports UP")
    void testActuatorHealthEndpointIsPublicAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("2. Actuator liveness probe endpoint is publicly accessible and reports UP")
    void testActuatorLivenessProbeIsPublicAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("3. Actuator readiness probe endpoint is publicly accessible and reports UP with database connectivity")
    void testActuatorReadinessProbeIsPublicAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("4. Actuator health response does NOT expose sensitive details or database credentials")
    void testActuatorHealthDoesNotExposeSensitiveDetails() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(jsonPath("$.db").doesNotExist())
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("secret"))))
                .andExpect(content().string(not(containsString("jdbc"))));
    }

    @Test
    @DisplayName("5. Sensitive Actuator endpoints (/env, /beans, /configprops, /heapdump, etc.) are NOT publicly exposed")
    void testSensitiveActuatorEndpointsAreNotExposed() throws Exception {
        String[] sensitiveEndpoints = {
                "/actuator/env",
                "/actuator/beans",
                "/actuator/configprops",
                "/actuator/heapdump",
                "/actuator/threaddump",
                "/actuator/loggers",
                "/actuator/mappings"
        };

        for (String endpoint : sensitiveEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        org.junit.jupiter.api.Assertions.assertTrue(
                                status == 401 || status == 403 || status == 404,
                                "Sensitive actuator endpoint " + endpoint + " must not return 200, but returned: " + status
                        );
                    });
        }
    }

    @Test
    @DisplayName("6. Root /actuator endpoint is protected and requires authentication")
    void testRootActuatorEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status == 401 || status == 403 || status == 404,
                            "Root /actuator endpoint must not be publicly accessible, but returned: " + status
                    );
                });
    }

    @Test
    @DisplayName("7. Existing protected application endpoints still require authentication")
    void testExistingProtectedEndpointsStillRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/owner/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. Existing public payment and booking routes remain intact")
    void testExistingPublicRoutesRemainIntact() throws Exception {
        // VNPay IPN endpoint remains public and accessible
        mockMvc.perform(get("/api/v1/payments/vnpay-ipn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").exists());
    }
}
