package com.example.spabooking.bootstrap;

import com.example.spabooking.auth.dto.LoginRequest;
import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ProductionOwnerBootstrapSeederTest {

    // Test-only identifiers and passwords. Never a real credential.
    private static final String TEST_TENANT_NAME = "Test Production Spa";
    private static final String TEST_TENANT_SLUG = "test-prod-spa";
    private static final String TEST_TENANT_PHONE = "0909999999";
    private static final String TEST_TENANT_EMAIL = "test@prodspa.local";
    private static final String TEST_TENANT_ADDRESS = "999 Le Loi, D1, HCMC";
    private static final String TEST_TENANT_TZ = "Asia/Ho_Chi_Minh";
    private static final String TEST_OWNER_USERNAME = "prod-owner@test.local";
    private static final String TEST_OWNER_PASSWORD = "StrongSecurePassword123!";

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

        // Ensure clean test slate
        userRepository.findByUsername(TEST_OWNER_USERNAME).ifPresent(userRepository::delete);
        tenantRepository.findBySlug(TEST_TENANT_SLUG).ifPresent(tenantRepository::delete);
        userRepository.flush();
        tenantRepository.flush();
    }

    private ProductionOwnerBootstrapSeeder createSeeder(
            boolean enabled,
            String tenantName,
            String tenantSlug,
            String ownerUsername,
            String ownerPassword) {
        return new ProductionOwnerBootstrapSeeder(
                tenantRepository,
                userRepository,
                passwordEncoder,
                enabled,
                tenantName,
                tenantSlug,
                TEST_TENANT_PHONE,
                TEST_TENANT_EMAIL,
                TEST_TENANT_ADDRESS,
                TEST_TENANT_TZ,
                ownerUsername,
                ownerPassword
        );
    }

    @Test
    @DisplayName("Case 1 — Bootstrap disabled: no tenant or owner record is created")
    void bootstrapDisabled_noRecordsCreated() {
        ProductionOwnerBootstrapSeeder seeder = createSeeder(
                false,
                TEST_TENANT_NAME,
                TEST_TENANT_SLUG,
                TEST_OWNER_USERNAME,
                TEST_OWNER_PASSWORD
        );

        seeder.run();

        assertTrue(tenantRepository.findBySlug(TEST_TENANT_SLUG).isEmpty(), "Tenant should not be created when disabled");
        assertTrue(userRepository.findByUsername(TEST_OWNER_USERNAME).isEmpty(), "User should not be created when disabled");
    }

    @Test
    @DisplayName("Case 2 — Bootstrap enabled with valid config: Tenant and OWNER are created correctly")
    void bootstrapEnabledWithValidConfig_createsTenantAndOwner() {
        ProductionOwnerBootstrapSeeder seeder = createSeeder(
                true,
                TEST_TENANT_NAME,
                TEST_TENANT_SLUG,
                TEST_OWNER_USERNAME,
                TEST_OWNER_PASSWORD
        );

        seeder.run();

        Tenant tenant = tenantRepository.findBySlug(TEST_TENANT_SLUG).orElseThrow();
        assertEquals(TEST_TENANT_NAME, tenant.getName());
        assertEquals(TEST_TENANT_SLUG, tenant.getSlug());
        assertEquals(TEST_TENANT_PHONE, tenant.getPhone());
        assertEquals(TEST_TENANT_EMAIL, tenant.getEmail());
        assertEquals(TEST_TENANT_ADDRESS, tenant.getAddress());
        assertEquals(TEST_TENANT_TZ, tenant.getTimezone());
        assertEquals(Boolean.TRUE, tenant.getIsActive());

        User owner = userRepository.findByUsername(TEST_OWNER_USERNAME).orElseThrow();
        assertEquals(TEST_OWNER_USERNAME, owner.getUsername());
        assertEquals(UserRole.OWNER, owner.getRole());
        assertNotNull(owner.getTenant());
        assertEquals(tenant.getId(), owner.getTenant().getId());
        assertEquals(Boolean.TRUE, owner.getIsActive());
        assertNull(owner.getStaff(), "Owner should have no staff link by default");
    }

    @Test
    @DisplayName("Case 3 — Password is BCrypt encoded and login works via existing auth flow")
    void passwordIsBcryptEncodedAndOwnerCanLogin() throws Exception {
        ProductionOwnerBootstrapSeeder seeder = createSeeder(
                true,
                TEST_TENANT_NAME,
                TEST_TENANT_SLUG,
                TEST_OWNER_USERNAME,
                TEST_OWNER_PASSWORD
        );

        seeder.run();

        User owner = userRepository.findByUsername(TEST_OWNER_USERNAME).orElseThrow();

        // 1. Password must be hashed with BCrypt
        assertNotEquals(TEST_OWNER_PASSWORD, owner.getPassword(), "Plaintext password must not be stored");
        assertTrue(owner.getPassword().startsWith("$2"), "Password must be BCrypt hashed");
        assertTrue(passwordEncoder.matches(TEST_OWNER_PASSWORD, owner.getPassword()), "Encoded password must match raw password via PasswordEncoder");

        // 2. Login via MockMvc /api/v1/auth/login must succeed
        LoginRequest loginRequest = new LoginRequest(TEST_OWNER_USERNAME, TEST_OWNER_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.username").value(TEST_OWNER_USERNAME))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_OWNER"))
                .andReturn();

        String jwt = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        assertNotNull(jwt);
        assertFalse(jwt.isBlank());
    }

    @Test
    @DisplayName("Case 4 — Idempotency: repeated execution does not duplicate records or reset password")
    void bootstrapIsIdempotent_repeatedRunsDoNotDuplicateOrResetPassword() {
        ProductionOwnerBootstrapSeeder seeder1 = createSeeder(
                true,
                TEST_TENANT_NAME,
                TEST_TENANT_SLUG,
                TEST_OWNER_USERNAME,
                TEST_OWNER_PASSWORD
        );
        seeder1.run();

        User ownerAfterFirstRun = userRepository.findByUsername(TEST_OWNER_USERNAME).orElseThrow();
        String originalPasswordHash = ownerAfterFirstRun.getPassword();

        // Run second time with a different password in config
        String newPasswordAttempt = "DifferentNewPassword999!";
        ProductionOwnerBootstrapSeeder seeder2 = createSeeder(
                true,
                TEST_TENANT_NAME,
                TEST_TENANT_SLUG,
                TEST_OWNER_USERNAME,
                newPasswordAttempt
        );
        seeder2.run();

        // Must still have exactly 1 tenant and 1 user
        long matchingTenants = tenantRepository.findAll().stream()
                .filter(t -> TEST_TENANT_SLUG.equals(t.getSlug()))
                .count();
        long matchingOwners = userRepository.findAll().stream()
                .filter(u -> TEST_OWNER_USERNAME.equals(u.getUsername()))
                .count();

        assertEquals(1, matchingTenants);
        assertEquals(1, matchingOwners);

        // Password hash must NOT have been changed or reset
        User ownerAfterSecondRun = userRepository.findByUsername(TEST_OWNER_USERNAME).orElseThrow();
        assertEquals(originalPasswordHash, ownerAfterSecondRun.getPassword(), "Existing OWNER password must not be overwritten");
        assertTrue(passwordEncoder.matches(TEST_OWNER_PASSWORD, ownerAfterSecondRun.getPassword()));
        assertFalse(passwordEncoder.matches(newPasswordAttempt, ownerAfterSecondRun.getPassword()));
    }

    @Test
    @DisplayName("Case 5 — Missing required configuration fails fast with clear error message")
    void missingConfiguration_failsFast() {
        // Missing tenant name
        assertThrows(IllegalStateException.class, () ->
                createSeeder(true, "", TEST_TENANT_SLUG, TEST_OWNER_USERNAME, TEST_OWNER_PASSWORD).run());

        // Missing tenant slug
        assertThrows(IllegalStateException.class, () ->
                createSeeder(true, TEST_TENANT_NAME, "", TEST_OWNER_USERNAME, TEST_OWNER_PASSWORD).run());

        // Missing owner username
        assertThrows(IllegalStateException.class, () ->
                createSeeder(true, TEST_TENANT_NAME, TEST_TENANT_SLUG, "", TEST_OWNER_PASSWORD).run());

        // Missing owner password
        assertThrows(IllegalStateException.class, () ->
                createSeeder(true, TEST_TENANT_NAME, TEST_TENANT_SLUG, TEST_OWNER_USERNAME, "").run());

        // Password shorter than 8 characters
        assertThrows(IllegalStateException.class, () ->
                createSeeder(true, TEST_TENANT_NAME, TEST_TENANT_SLUG, TEST_OWNER_USERNAME, "short").run());

        // Verify nothing was persisted during failure
        assertTrue(tenantRepository.findBySlug(TEST_TENANT_SLUG).isEmpty());
        assertTrue(userRepository.findByUsername(TEST_OWNER_USERNAME).isEmpty());
    }

    @Test
    @DisplayName("Case 6 — No plaintext password is persisted in database")
    void noPlaintextPasswordPersisted() {
        ProductionOwnerBootstrapSeeder seeder = createSeeder(
                true,
                TEST_TENANT_NAME,
                TEST_TENANT_SLUG,
                TEST_OWNER_USERNAME,
                TEST_OWNER_PASSWORD
        );
        seeder.run();

        User owner = userRepository.findByUsername(TEST_OWNER_USERNAME).orElseThrow();
        assertFalse(owner.getPassword().contains(TEST_OWNER_PASSWORD));
    }

    @Test
    @DisplayName("Case 7 — Existing Tenant is not overwritten when creating missing OWNER")
    void existingTenantIsNotOverwritten() {
        // Pre-create tenant with custom description/name
        Tenant existing = new Tenant();
        existing.setName("Custom Original Name");
        existing.setSlug(TEST_TENANT_SLUG);
        existing.setPhone("0123456789");
        existing.setIsActive(true);
        tenantRepository.saveAndFlush(existing);

        // Run seeder with different tenant name
        ProductionOwnerBootstrapSeeder seeder = createSeeder(
                true,
                "Overwritten Name Attempt",
                TEST_TENANT_SLUG,
                TEST_OWNER_USERNAME,
                TEST_OWNER_PASSWORD
        );
        seeder.run();

        Tenant tenantAfter = tenantRepository.findBySlug(TEST_TENANT_SLUG).orElseThrow();
        assertEquals("Custom Original Name", tenantAfter.getName(), "Existing tenant name must not be overwritten");
        assertEquals("0123456789", tenantAfter.getPhone());

        // But OWNER is successfully created under the existing tenant
        User owner = userRepository.findByUsername(TEST_OWNER_USERNAME).orElseThrow();
        assertEquals(tenantAfter.getId(), owner.getTenant().getId());
    }

    @Test
    @DisplayName("Case 8 — Seeder bean is NOT registered in application context outside 'prod' profile")
    void seederBeanIsNotRegisteredOutsideProdProfile() {
        assertNull(context.getBeanProvider(ProductionOwnerBootstrapSeeder.class).getIfAvailable(),
                "ProductionOwnerBootstrapSeeder must not be registered outside the 'prod' profile");
    }

    @Test
    @DisplayName("Case 9 — OWNER username already exists in a different tenant: fails fast and does not create orphaned tenant")
    void ownerUsernameInDifferentTenant_failsFast() {
        // Pre-create another tenant and user with the target username
        Tenant otherTenant = new Tenant();
        otherTenant.setName("Other Tenant");
        otherTenant.setSlug("other-tenant");
        otherTenant.setIsActive(true);
        otherTenant = tenantRepository.saveAndFlush(otherTenant);

        User existingUser = new User();
        existingUser.setUsername(TEST_OWNER_USERNAME);
        existingUser.setPassword(passwordEncoder.encode("ExistingOtherPassword123!"));
        existingUser.setRole(UserRole.OWNER);
        existingUser.setTenant(otherTenant);
        existingUser.setIsActive(true);
        userRepository.saveAndFlush(existingUser);

        // Attempt bootstrap for target tenant using the same username
        ProductionOwnerBootstrapSeeder seeder = createSeeder(
                true,
                TEST_TENANT_NAME,
                TEST_TENANT_SLUG,
                TEST_OWNER_USERNAME,
                TEST_OWNER_PASSWORD
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, seeder::run);
        assertTrue(ex.getMessage().contains("belongs to a different tenant"));

        // Verify target tenant was not persisted/committed
        assertTrue(tenantRepository.findBySlug(TEST_TENANT_SLUG).isEmpty(), "Target tenant must not be created on collision");
    }

    @Test
    @DisplayName("Case 10 — Existing username in target tenant has a different role (STAFF): fails fast and does not escalate")
    void existingUserInTargetTenantWithStaffRole_failsFast() {
        // Pre-create target tenant
        Tenant tenant = new Tenant();
        tenant.setName(TEST_TENANT_NAME);
        tenant.setSlug(TEST_TENANT_SLUG);
        tenant.setIsActive(true);
        tenant = tenantRepository.saveAndFlush(tenant);

        // Pre-create user with STAFF role
        User staffUser = new User();
        staffUser.setUsername(TEST_OWNER_USERNAME);
        staffUser.setPassword(passwordEncoder.encode("StaffPassword123!"));
        staffUser.setRole(UserRole.STAFF);
        staffUser.setTenant(tenant);
        staffUser.setIsActive(true);
        userRepository.saveAndFlush(staffUser);

        // Attempt bootstrap for target tenant with this username
        ProductionOwnerBootstrapSeeder seeder = createSeeder(
                true,
                TEST_TENANT_NAME,
                TEST_TENANT_SLUG,
                TEST_OWNER_USERNAME,
                TEST_OWNER_PASSWORD
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, seeder::run);
        assertTrue(ex.getMessage().contains("with role 'STAFF' and cannot be bootstrapped as OWNER"));

        // Verify role was not escalated
        User reloadedUser = userRepository.findByUsername(TEST_OWNER_USERNAME).orElseThrow();
        assertEquals(UserRole.STAFF, reloadedUser.getRole(), "User role must not be escalated to OWNER");
    }
}

