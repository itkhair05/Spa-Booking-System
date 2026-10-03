package com.example.spabooking.dev;

import com.example.spabooking.auth.dto.LoginRequest;
import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class DevDataSeederTest {

    // Test-only value. Never a real credential.
    private static final String DEMO_PASSWORD = "test-only-dev-password";
    private static final String DEMO_STAFF_PASSWORD = "test-only-staff-password";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clean slate for the fixed demo identifiers; rolled back by @Transactional.
        userRepository.findByUsername(DevDataSeeder.DEMO_OWNER_USERNAME).ifPresent(userRepository::delete);
        userRepository.findByUsername(DevDataSeeder.DEMO_STAFF_USERNAME).ifPresent(userRepository::delete);
        tenantRepository.findBySlug(DevDataSeeder.DEMO_TENANT_SLUG).ifPresent(tenantRepository::delete);
        userRepository.flush();
        tenantRepository.flush();
    }

    private DevDataSeeder seeder(String ownerPassword, String staffPassword) {
        return new DevDataSeeder(tenantRepository, userRepository, passwordEncoder, ownerPassword, staffPassword);
    }

    @Test
    void createsDemoTenantAndOwnerAndStaffWhenAbsent() {
        seeder(DEMO_PASSWORD, DEMO_STAFF_PASSWORD).run();

        Tenant tenant = tenantRepository.findBySlug(DevDataSeeder.DEMO_TENANT_SLUG).orElseThrow();
        User owner = userRepository.findByUsername(DevDataSeeder.DEMO_OWNER_USERNAME).orElseThrow();
        User staff = userRepository.findByUsername(DevDataSeeder.DEMO_STAFF_USERNAME).orElseThrow();

        assertEquals("Demo Spa", tenant.getName());
        
        assertEquals(UserRole.OWNER, owner.getRole());
        assertNotNull(owner.getTenant());
        assertEquals(tenant.getId(), owner.getTenant().getId());
        assertEquals(Boolean.TRUE, owner.getIsActive());
        
        assertEquals(UserRole.STAFF, staff.getRole());
        assertNotNull(staff.getTenant());
        assertEquals(tenant.getId(), staff.getTenant().getId());
        assertEquals(Boolean.TRUE, staff.getIsActive());
    }

    @Test
    void storesPasswordsHashedNotPlaintext() {
        seeder(DEMO_PASSWORD, DEMO_STAFF_PASSWORD).run();

        User owner = userRepository.findByUsername(DevDataSeeder.DEMO_OWNER_USERNAME).orElseThrow();
        User staff = userRepository.findByUsername(DevDataSeeder.DEMO_STAFF_USERNAME).orElseThrow();

        assertNotEquals(DEMO_PASSWORD, owner.getPassword());
        assertTrue(owner.getPassword().startsWith("$2"));
        assertTrue(passwordEncoder.matches(DEMO_PASSWORD, owner.getPassword()));
        
        assertNotEquals(DEMO_STAFF_PASSWORD, staff.getPassword());
        assertTrue(staff.getPassword().startsWith("$2"));
        assertTrue(passwordEncoder.matches(DEMO_STAFF_PASSWORD, staff.getPassword()));
    }

    @Test
    void runningTwiceDoesNotCreateDuplicates() {
        DevDataSeeder seeder = seeder(DEMO_PASSWORD, DEMO_STAFF_PASSWORD);
        seeder.run();
        seeder.run();

        long tenants = tenantRepository.findAll().stream()
                .filter(t -> DevDataSeeder.DEMO_TENANT_SLUG.equals(t.getSlug()))
                .count();
        long owners = userRepository.findAll().stream()
                .filter(u -> DevDataSeeder.DEMO_OWNER_USERNAME.equals(u.getUsername()))
                .count();
        long staffs = userRepository.findAll().stream()
                .filter(u -> DevDataSeeder.DEMO_STAFF_USERNAME.equals(u.getUsername()))
                .count();

        assertEquals(1, tenants);
        assertEquals(1, owners);
        assertEquals(1, staffs);
    }

    @Test
    void refusesToRunWithoutPassword() {
        assertThrows(IllegalStateException.class, () -> seeder("   ", DEMO_STAFF_PASSWORD).run());
        assertThrows(IllegalStateException.class, () -> seeder(null, DEMO_STAFF_PASSWORD).run());
        assertThrows(IllegalStateException.class, () -> seeder(DEMO_PASSWORD, "   ").run());
        assertThrows(IllegalStateException.class, () -> seeder(DEMO_PASSWORD, null).run());

        assertTrue(tenantRepository.findBySlug(DevDataSeeder.DEMO_TENANT_SLUG).isEmpty());
        assertTrue(userRepository.findByUsername(DevDataSeeder.DEMO_OWNER_USERNAME).isEmpty());
        assertTrue(userRepository.findByUsername(DevDataSeeder.DEMO_STAFF_USERNAME).isEmpty());
    }

    @Test
    void seededOwnerCanLoginAndAccessProtectedEndpoint() throws Exception {
        seeder(DEMO_PASSWORD, DEMO_STAFF_PASSWORD).run();

        LoginRequest request = new LoginRequest();
        request.setUsername(DevDataSeeder.DEMO_OWNER_USERNAME);
        request.setPassword(DEMO_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.username").value(DevDataSeeder.DEMO_OWNER_USERNAME))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_OWNER"))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void seederBeanIsNotRegisteredOutsideDevProfile() {
        assertNull(context.getBeanProvider(DevDataSeeder.class).getIfAvailable());
    }
}
