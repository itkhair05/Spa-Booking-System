package com.example.spabooking.staff.controller;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class StaffControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private String ownerJwt;
    private String staffJwt;
    private String ownerTenantBJwt;
    
    private Tenant tenantA;
    private Tenant tenantB;
    private Staff staffA;
    private Staff staffB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a-staff");
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b-staff");
        tenantB = tenantRepository.saveAndFlush(tenantB);

        User ownerA = new User();
        ownerA.setUsername("ownerAStaff");
        ownerA.setPassword("encoded");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA = userRepository.saveAndFlush(ownerA);

        User staffUserA = new User();
        staffUserA.setUsername("staffAUser");
        staffUserA.setPassword("encoded");
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setTenant(tenantA);
        staffUserA = userRepository.saveAndFlush(staffUserA);

        User ownerB = new User();
        ownerB.setUsername("ownerBStaff");
        ownerB.setPassword("encoded");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB = userRepository.saveAndFlush(ownerB);

        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));
        staffJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(staffUserA), null, new CustomUserDetails(staffUserA).getAuthorities()));
        ownerTenantBJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));

        staffA = new Staff();
        staffA.setTenant(tenantA);
        staffA.setName("Alice");
        staffA.setPhone("123456789");
        staffA.setEmail("alice@example.com");
        staffA = staffRepository.saveAndFlush(staffA);

        staffB = new Staff();
        staffB.setTenant(tenantB);
        staffB.setName("Bob");
        staffB.setPhone("987654321");
        staffB.setEmail("bob@example.com");
        staffB = staffRepository.saveAndFlush(staffB);
    }

    @Test
    void testUnauthenticatedGet() throws Exception {
        mockMvc.perform(get("/api/v1/staff"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testOwnerGetStaff() throws Exception {
        mockMvc.perform(get("/api/v1/staff")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Alice")));
    }

    @Test
    void testStaffGetStaff() throws Exception {
        mockMvc.perform(get("/api/v1/staff")
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Alice")));
    }

    @Test
    void testCrossTenantGet() throws Exception {
        mockMvc.perform(get("/api/v1/staff/" + staffB.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testOwnerCreateStaff() throws Exception {
        String requestJson = """
                {
                    "name": "Charlie",
                    "email": "charlie@example.com",
                    "phone": "555-0100",
                    "isActive": true
                }
                """;

        mockMvc.perform(post("/api/v1/staff")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Charlie")));
    }

    @Test
    void testStaffCannotCreateStaff() throws Exception {
        String requestJson = """
                {
                    "name": "Charlie",
                    "email": "charlie@example.com",
                    "phone": "555-0100"
                }
                """;

        mockMvc.perform(post("/api/v1/staff")
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testOwnerUpdateStaff() throws Exception {
        String requestJson = """
                {
                    "name": "Alice Updated",
                    "email": "alice.updated@example.com",
                    "phone": "123456789"
                }
                """;

        mockMvc.perform(put("/api/v1/staff/" + staffA.getId())
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice Updated")));
    }

    @Test
    void testCrossTenantUpdate() throws Exception {
        String requestJson = """
                {
                    "name": "Hacked Bob",
                    "email": "hacked@example.com"
                }
                """;

        mockMvc.perform(put("/api/v1/staff/" + staffB.getId())
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void testOwnerDeleteStaff() throws Exception {
        mockMvc.perform(delete("/api/v1/staff/" + staffA.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/staff/" + staffA.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCrossTenantDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/staff/" + staffB.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testValidationFailure() throws Exception {
        String requestJson = """
                {
                    "name": "",
                    "email": "invalid-email"
                }
                """;

        mockMvc.perform(post("/api/v1/staff")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }
}
