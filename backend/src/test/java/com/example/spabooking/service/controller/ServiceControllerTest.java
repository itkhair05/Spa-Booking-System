package com.example.spabooking.service.controller;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class ServiceControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private String ownerJwt;
    private String staffJwt;
    private String ownerTenantBJwt;
    
    private Tenant tenantA;
    private Tenant tenantB;
    private Service serviceA;
    private Service serviceB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a");
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b");
        tenantB = tenantRepository.saveAndFlush(tenantB);

        User ownerA = new User();
        ownerA.setUsername("ownerA");
        ownerA.setPassword("encoded");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA = userRepository.saveAndFlush(ownerA);

        User staffA = new User();
        staffA.setUsername("staffA");
        staffA.setPassword("encoded");
        staffA.setRole(UserRole.STAFF);
        staffA.setTenant(tenantA);
        staffA = userRepository.saveAndFlush(staffA);

        User ownerB = new User();
        ownerB.setUsername("ownerB");
        ownerB.setPassword("encoded");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB = userRepository.saveAndFlush(ownerB);

        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));
        staffJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(staffA), null, new CustomUserDetails(staffA).getAuthorities()));
        ownerTenantBJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));

        serviceA = new Service();
        serviceA.setTenant(tenantA);
        serviceA.setName("Facial A");
        serviceA.setDurationMinutes(60);
        serviceA.setPrice(new BigDecimal("100.00"));
        serviceA = serviceRepository.saveAndFlush(serviceA);

        serviceB = new Service();
        serviceB.setTenant(tenantB);
        serviceB.setName("Massage B");
        serviceB.setDurationMinutes(90);
        serviceB.setPrice(new BigDecimal("150.00"));
        serviceB = serviceRepository.saveAndFlush(serviceB);
    }

    @Test
    void testUnauthenticatedGet() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testOwnerGetServices() throws Exception {
        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Facial A")));
    }

    @Test
    void testStaffGetServices() throws Exception {
        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Facial A")));
    }

    @Test
    void testCrossTenantGet() throws Exception {
        // Owner A tries to access Service B
        mockMvc.perform(get("/api/v1/services/" + serviceB.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testOwnerCreateService() throws Exception {
        String requestJson = """
                {
                    "name": "New Service",
                    "durationMinutes": 45,
                    "price": 50.00,
                    "isActive": true
                }
                """;

        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Service")));
    }

    @Test
    void testStaffCannotCreateService() throws Exception {
        String requestJson = """
                {
                    "name": "New Service",
                    "durationMinutes": 45,
                    "price": 50.00
                }
                """;

        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testOwnerUpdateService() throws Exception {
        String requestJson = """
                {
                    "name": "Updated Facial A",
                    "durationMinutes": 120,
                    "price": 200.00
                }
                """;

        mockMvc.perform(put("/api/v1/services/" + serviceA.getId())
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Facial A")));
    }

    @Test
    void testCrossTenantUpdate() throws Exception {
        String requestJson = """
                {
                    "name": "Hacked",
                    "durationMinutes": 120,
                    "price": 200.00
                }
                """;

        // Owner A tries to update Service B
        mockMvc.perform(put("/api/v1/services/" + serviceB.getId())
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void testOwnerDeleteService() throws Exception {
        mockMvc.perform(delete("/api/v1/services/" + serviceA.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/api/v1/services/" + serviceA.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCrossTenantDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/services/" + serviceB.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testValidationFailure() throws Exception {
        String requestJson = """
                {
                    "name": "",
                    "durationMinutes": -10,
                    "price": -50.00
                }
                """;

        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.durationMinutes").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }
}
