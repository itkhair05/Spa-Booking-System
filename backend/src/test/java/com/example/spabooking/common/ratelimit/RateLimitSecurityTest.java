package com.example.spabooking.common.ratelimit;

import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "security.rate-limit.enabled=true")
public class RateLimitSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    private MockMvc mockMvc;
    private String slug;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Tenant tenant = new Tenant();
        tenant.setName("Rate Limit Spa");
        tenant.setSlug("rate-limit-spa-" + System.currentTimeMillis());
        tenant.setIsActive(true);
        tenant = tenantRepository.saveAndFlush(tenant);
        slug = tenant.getSlug();
    }

    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    // Fixed windows align to epoch minutes; ensure at least 5s of headroom so an
    // exhausted window cannot roll over mid-assertion.
    private static void awaitFreshWindow() throws InterruptedException {
        long intoWindow = System.currentTimeMillis() % 60_000;
        if (intoWindow > 55_000) {
            Thread.sleep(60_000 - intoWindow);
        }
    }

    private static String loginBody() {
        return "{\"username\":\"nobody\",\"password\":\"wrong-password\"}";
    }

    @Test
    void loginBruteForceIsThrottledAfterTenAttempts() throws Exception {
        awaitFreshWindow();
        String ip = "203.0.113.10";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login").with(fromIp(ip))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody()))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login").with(fromIp(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    void rateLimitIsScopedPerClientIp() throws Exception {
        awaitFreshWindow();
        String exhaustedIp = "203.0.113.20";
        String otherIp = "203.0.113.21";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login").with(fromIp(exhaustedIp))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody()))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login").with(fromIp(exhaustedIp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(post("/api/v1/auth/login").with(fromIp(otherIp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exhaustedLoginDoesNotAffectUnrelatedEndpoints() throws Exception {
        awaitFreshWindow();
        String ip = "203.0.113.30";

        for (int i = 0; i < 11; i++) {
            mockMvc.perform(post("/api/v1/auth/login").with(fromIp(ip))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody()));
        }

        mockMvc.perform(get("/api/v1/public/spas/" + slug + "/services").with(fromIp(ip)))
                .andExpect(status().isOk());
    }

    @Test
    void passwordChangeIsThrottled() throws Exception {
        awaitFreshWindow();
        String ip = "203.0.113.40";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/change-password").with(fromIp(ip))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"x\",\"newPassword\":\"y\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/change-password").with(fromIp(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"x\",\"newPassword\":\"y\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void publicBookingLookupIsThrottled() throws Exception {
        awaitFreshWindow();
        String ip = "203.0.113.50";

        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/api/v1/public/spas/" + slug + "/bookings/BKUNKNOWN" + i).with(fromIp(ip)))
                    .andExpect(status().isNotFound());
        }

        mockMvc.perform(get("/api/v1/public/spas/" + slug + "/bookings/BKUNKNOWN99").with(fromIp(ip)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void publicBookingCreationIsThrottled() throws Exception {
        awaitFreshWindow();
        String ip = "203.0.113.60";

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/public/spas/" + slug + "/bookings").with(fromIp(ip))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/v1/public/spas/" + slug + "/bookings").with(fromIp(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void refundInitiationIsThrottled() throws Exception {
        awaitFreshWindow();
        String ip = "203.0.113.70";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/bookings/999999/refund").with(fromIp(ip)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/bookings/999999/refund").with(fromIp(ip)))
                .andExpect(status().isTooManyRequests());
    }
}
