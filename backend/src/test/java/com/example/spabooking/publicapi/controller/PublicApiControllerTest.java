package com.example.spabooking.publicapi.controller;

import com.example.spabooking.publicapi.dto.CreatePublicBookingRequest;
import com.example.spabooking.publicapi.service.PublicBookingService;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PublicApiControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        TenantContext.clear();
        ensureDemoTenantExists();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // These tests must not depend on a dev-seeded database; the tenant is
    // created inside the test transaction and rolled back afterwards.
    private void ensureDemoTenantExists() {
        if (tenantRepository.findBySlug("tikey-spa").isEmpty()) {
            Tenant tenant = new Tenant();
            tenant.setName("TIKEY SPA");
            tenant.setSlug("tikey-spa");
            tenant.setIsActive(true);
            tenantRepository.saveAndFlush(tenant);
        }
    }

    @Test
    public void testGetSpaInfo_Success() throws Exception {
        // "tikey-spa" is ensured by the test setup
        mockMvc.perform(get("/api/v1/public/spas/tikey-spa"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetSpaInfo_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/public/spas/unknown-spa-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetServices_Success() throws Exception {
        mockMvc.perform(get("/api/v1/public/spas/tikey-spa/services"))
                .andExpect(status().isOk());
    }
    
    @Test
    public void testGetStaff_Success() throws Exception {
        mockMvc.perform(get("/api/v1/public/spas/tikey-spa/staff"))
                .andExpect(status().isOk());
    }
    
    @Test
    public void testGetAvailability_NotFoundWhenServiceMissing() throws Exception {
        mockMvc.perform(get("/api/v1/public/spas/tikey-spa/availability")
                .param("serviceId", "1")
                .param("date", "2026-10-10"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    public void testCreateBooking_ValidationFailed() throws Exception {
        String invalidJson = "{}"; // Missing required fields
        
        mockMvc.perform(post("/api/v1/public/spas/tikey-spa/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    public void testManagementSecurityRegression() throws Exception {
        // Accessing authenticated management endpoint should return 401 Unauthorized for anonymous user
        mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testTenantIsolation_CrossTenantServiceRejected() throws Exception {
        String crossTenantJson = "{"
                + "\"serviceId\": 999,"
                + "\"staffId\": 1,"
                + "\"startTime\": \"2026-12-12T10:00:00\","
                + "\"customerName\": \"Test Customer\","
                + "\"customerPhone\": \"0912345678\""
                + "}";

        // The service should not be found within the resolved tenant context, returning 404
        mockMvc.perform(post("/api/v1/public/spas/tikey-spa/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(crossTenantJson))
                .andExpect(status().isNotFound());
    }
}
