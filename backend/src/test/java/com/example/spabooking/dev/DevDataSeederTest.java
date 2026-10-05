package com.example.spabooking.dev;

import com.example.spabooking.auth.dto.LoginRequest;
import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.customer.repository.CustomerRepository;
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

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private com.example.spabooking.feedback.repository.FeedbackRepository feedbackRepository;

    @Autowired
    private com.example.spabooking.service.repository.ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private com.example.spabooking.article.repository.ArticleRepository articleRepository;

    @Autowired
    private com.example.spabooking.review.repository.ReviewRepository reviewRepository;

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
        tenantRepository.findBySlug(DevDataSeeder.DEMO_TENANT_SLUG).ifPresent(t -> {
            bookingRepository.findAllByTenantId(t.getId()).forEach(bookingRepository::delete);
            bookingRepository.flush();

            customerRepository.findAllByTenantId(t.getId()).forEach(customerRepository::delete);
            customerRepository.flush();

            feedbackRepository.findAllByTenantId(t.getId()).forEach(feedbackRepository::delete);
            feedbackRepository.flush();

            serviceRepository.findAllByTenantId(t.getId()).forEach(serviceRepository::delete);
            serviceRepository.flush();

            serviceCategoryRepository.findAllByTenantId(t.getId()).forEach(serviceCategoryRepository::delete);
            serviceCategoryRepository.flush();

            articleRepository.findAllByTenantId(t.getId()).forEach(articleRepository::delete);
            articleRepository.flush();

            reviewRepository.findAllByTenantId(t.getId()).forEach(reviewRepository::delete);
            reviewRepository.flush();

            // Remove any login accounts linked to this tenant's staff before deleting
            // the staff rows (users.staff_id FK), including accounts created via the UI.
            java.util.List<Long> staffIds = staffRepository.findAllByTenantId(t.getId()).stream()
                    .map(Staff::getId)
                    .collect(java.util.stream.Collectors.toList());
            if (!staffIds.isEmpty()) {
                userRepository.findAllByStaffIdIn(staffIds).forEach(userRepository::delete);
                userRepository.flush();
            }

            staffRepository.findAllByTenantId(t.getId()).forEach(staffRepository::delete);
            staffRepository.flush();
            tenantRepository.delete(t);
        });
        userRepository.flush();
        tenantRepository.flush();
        serviceRepository.flush();
        staffRepository.flush();
    }

    private DevDataSeeder seeder(String ownerPassword, String staffPassword) {
        return new DevDataSeeder(tenantRepository, userRepository, passwordEncoder, serviceRepository, staffRepository, ownerPassword, staffPassword);
    }

    @Test
    void createsDemoTenantAndOwnerAndStaffWhenAbsent() {
        seeder(DEMO_PASSWORD, DEMO_STAFF_PASSWORD).run();

        Tenant tenant = tenantRepository.findBySlug(DevDataSeeder.DEMO_TENANT_SLUG).orElseThrow();
        User owner = userRepository.findByUsername(DevDataSeeder.DEMO_OWNER_USERNAME).orElseThrow();
        User staff = userRepository.findByUsername(DevDataSeeder.DEMO_STAFF_USERNAME).orElseThrow();

        assertEquals("TIKEY SPA", tenant.getName());

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
    void renamesLegacyDemoSpaTenantInPlaceWithoutCreatingDuplicate() {
        Tenant legacy = new Tenant();
        legacy.setName("Demo Spa");
        legacy.setSlug(DevDataSeeder.LEGACY_TENANT_SLUG);
        legacy.setIsActive(true);
        legacy = tenantRepository.saveAndFlush(legacy);
        Long legacyId = legacy.getId();

        seeder(DEMO_PASSWORD, DEMO_STAFF_PASSWORD).run();

        Tenant renamed = tenantRepository.findBySlug(DevDataSeeder.DEMO_TENANT_SLUG).orElseThrow();
        assertEquals(legacyId, renamed.getId());
        assertEquals("TIKEY SPA", renamed.getName());
        assertTrue(tenantRepository.findBySlug(DevDataSeeder.LEGACY_TENANT_SLUG).isEmpty());

        long tikeyTenants = tenantRepository.findAll().stream()
                .filter(t -> DevDataSeeder.DEMO_TENANT_SLUG.equals(t.getSlug()))
                .count();
        assertEquals(1, tikeyTenants);
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

        Tenant tenant = tenantRepository.findBySlug(DevDataSeeder.DEMO_TENANT_SLUG).orElseThrow();
        long servicesCount = serviceRepository.findAllByTenantId(tenant.getId()).size();
        long staffRecordsCount = staffRepository.findAllByTenantId(tenant.getId()).size();

        assertEquals(1, tenants);
        assertEquals(1, owners);
        assertEquals(1, staffs);
        assertEquals(4, servicesCount);
        assertEquals(3, staffRecordsCount);
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
    void partialSeedRecoveryRestoresMissingFixtures() {
        // 1. Initial seed
        DevDataSeeder seeder = seeder(DEMO_PASSWORD, DEMO_STAFF_PASSWORD);
        seeder.run();

        Tenant tenant = tenantRepository.findBySlug(DevDataSeeder.DEMO_TENANT_SLUG).orElseThrow();

        // 2. Delete one Service and one Staff
        var services = serviceRepository.findAllByTenantId(tenant.getId());
        Service serviceToDelete = services.stream().filter(s -> s.getName().equals("Massage thư giãn cơ bản")).findFirst().orElseThrow();
        serviceRepository.delete(serviceToDelete);

        var staffs = staffRepository.findAllByTenantId(tenant.getId());
        Staff staffToDelete = staffs.stream().filter(s -> "0912345678".equals(s.getPhone())).findFirst().orElseThrow();
        staffRepository.delete(staffToDelete);

        // Ensure deletion is flushed
        serviceRepository.flush();
        staffRepository.flush();

        // 3. Seed again
        seeder.run();

        // 4. Verify recovery
        var servicesAfter = serviceRepository.findAllByTenantId(tenant.getId());
        var staffsAfter = staffRepository.findAllByTenantId(tenant.getId());

        assertEquals(4, servicesAfter.size(), "Should have exactly 4 services after recovery");
        assertTrue(servicesAfter.stream().anyMatch(s -> s.getName().equals("Massage thư giãn cơ bản")), "Missing service should be recovered");

        assertEquals(3, staffsAfter.size(), "Should have exactly 3 staff records after recovery");
        assertTrue(staffsAfter.stream().anyMatch(s -> "0912345678".equals(s.getPhone())), "Missing staff should be recovered");
    }

    @Test
    void seededServicesAndStaffAreActiveAndBelongToTenant() {
        seeder(DEMO_PASSWORD, DEMO_STAFF_PASSWORD).run();

        Tenant tenant = tenantRepository.findBySlug(DevDataSeeder.DEMO_TENANT_SLUG).orElseThrow();
        var services = serviceRepository.findAllByTenantId(tenant.getId());
        var staffs = staffRepository.findAllByTenantId(tenant.getId());

        assertEquals(4, services.size());
        for (Service s : services) {
            assertTrue(s.getIsActive());
            assertEquals(tenant.getId(), s.getTenant().getId());
            assertNotNull(s.getPrice());
            assertNotNull(s.getDurationMinutes());
        }

        assertEquals(3, staffs.size());
        for (Staff st : staffs) {
            assertTrue(st.getIsActive());
            assertEquals(tenant.getId(), st.getTenant().getId());
            assertNotNull(st.getName());
        }
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
