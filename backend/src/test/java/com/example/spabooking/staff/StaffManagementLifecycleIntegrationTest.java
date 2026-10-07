package com.example.spabooking.staff;

import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.auth.security.JwtUtils;
import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class StaffManagementLifecycleIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private Tenant tenantA;
    private Tenant tenantB;

    private String ownerJwtA;
    private String staffJwtA1;

    private Staff staffA1; // active
    private Staff staffA2; // active
    private Staff staffA3; // active
    private Staff staffA4; // inactive/deleted
    private Staff staffA5; // inactive/deleted
    private Staff staffA6; // inactive/deleted

    private Staff staffB1; // other tenant

    private User staffUserA4;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        long ts = System.currentTimeMillis();

        tenantA = new Tenant();
        tenantA.setName("Tenant A " + ts);
        tenantA.setSlug("tenant-a-" + ts);
        tenantA.setIsActive(true);
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B " + ts);
        tenantB.setSlug("tenant-b-" + ts);
        tenantB.setIsActive(true);
        tenantB = tenantRepository.saveAndFlush(tenantB);

        // OWNER for Tenant A
        User ownerUserA = new User();
        ownerUserA.setUsername("owner_a_" + ts);
        ownerUserA.setPassword(passwordEncoder.encode("Owner123!"));
        ownerUserA.setRole(UserRole.OWNER);
        ownerUserA.setTenant(tenantA);
        ownerUserA.setIsActive(true);
        ownerUserA = userRepository.saveAndFlush(ownerUserA);

        CustomUserDetails ownerDetailsA = new CustomUserDetails(ownerUserA);
        ownerJwtA = jwtUtils.generateJwtToken(
                new UsernamePasswordAuthenticationToken(ownerDetailsA, null, ownerDetailsA.getAuthorities()));

        // Create 3 active staff for Tenant A
        staffA1 = createStaff(tenantA, "Staff A1 " + ts, true, false, true);
        staffA2 = createStaff(tenantA, "Staff A2 " + ts, true, false, true);
        staffA3 = createStaff(tenantA, "Staff A3 " + ts, true, false, true);

        // Create 3 inactive/deleted staff for Tenant A
        staffA4 = createStaff(tenantA, "Staff A4 " + ts, false, true, true);
        staffA5 = createStaff(tenantA, "Staff A5 " + ts, false, true, true);
        staffA6 = createStaff(tenantA, "Staff A6 " + ts, false, true, true);

        // Linked User for staffA1 (active)
        User staffUserA1 = new User();
        staffUserA1.setUsername("staff_a1_" + ts);
        staffUserA1.setPassword(passwordEncoder.encode("Staff123!"));
        staffUserA1.setRole(UserRole.STAFF);
        staffUserA1.setTenant(tenantA);
        staffUserA1.setStaff(staffA1);
        staffUserA1.setIsActive(true);
        staffUserA1 = userRepository.saveAndFlush(staffUserA1);

        CustomUserDetails staffDetailsA1 = new CustomUserDetails(staffUserA1);
        staffJwtA1 = jwtUtils.generateJwtToken(
                new UsernamePasswordAuthenticationToken(staffDetailsA1, null, staffDetailsA1.getAuthorities()));

        // Linked User for staffA4 (inactive/deleted)
        staffUserA4 = new User();
        staffUserA4.setUsername("staff_a4_" + ts);
        staffUserA4.setPassword(passwordEncoder.encode("StaffPass123!"));
        staffUserA4.setRole(UserRole.STAFF);
        staffUserA4.setTenant(tenantA);
        staffUserA4.setStaff(staffA4);
        staffUserA4.setIsActive(false);
        staffUserA4 = userRepository.saveAndFlush(staffUserA4);

        // Create 1 staff in Tenant B
        staffB1 = createStaff(tenantB, "Staff B1 " + ts, true, false, true);
    }

    private Staff createStaff(Tenant tenant, String name, boolean isActive, boolean isDeleted, boolean showOnWebsite) {
        Staff s = new Staff();
        s.setTenant(tenant);
        s.setName(name);
        s.setPhone("090" + (1000000 + (int)(Math.random() * 8999999)));
        s.setIsActive(isActive);
        s.setIsDeleted(isDeleted);
        s.setShowOnWebsite(showOnWebsite);
        return staffRepository.saveAndFlush(s);
    }

    @Test
    @DisplayName("1. OWNER fetches staff -> returns all 6 records (3 active, 3 inactive/deleted)")
    void testOwnerSeesAllSixStaffRecords() throws Exception {
        mockMvc.perform(get("/api/v1/staff")
                        .header("Authorization", "Bearer " + ownerJwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[?(@.id == " + staffA1.getId() + ")].isActive").value(true))
                .andExpect(jsonPath("$[?(@.id == " + staffA2.getId() + ")].isActive").value(true))
                .andExpect(jsonPath("$[?(@.id == " + staffA3.getId() + ")].isActive").value(true))
                .andExpect(jsonPath("$[?(@.id == " + staffA4.getId() + ")].isActive").value(false))
                .andExpect(jsonPath("$[?(@.id == " + staffA5.getId() + ")].isActive").value(false))
                .andExpect(jsonPath("$[?(@.id == " + staffA6.getId() + ")].isActive").value(false))
                .andExpect(jsonPath("$[?(@.id == " + staffA4.getId() + ")].isDeleted").value(true))
                .andExpect(jsonPath("$[?(@.id == " + staffA5.getId() + ")].isDeleted").value(true))
                .andExpect(jsonPath("$[?(@.id == " + staffA6.getId() + ")].isDeleted").value(true));
    }

    @Test
    @DisplayName("2 & 3. Active filter returns exactly 3 active staff records")
    void testActiveFilterReturnsThreeRecords() throws Exception {
        mockMvc.perform(get("/api/v1/staff?status=ACTIVE")
                        .header("Authorization", "Bearer " + ownerJwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].isActive", everyItem(is(true))))
                .andExpect(jsonPath("$[*].isDeleted", everyItem(is(false))));
    }

    @Test
    @DisplayName("4. Inactive filter returns exactly 3 inactive records")
    void testInactiveFilterReturnsThreeRecords() throws Exception {
        mockMvc.perform(get("/api/v1/staff?status=INACTIVE")
                        .header("Authorization", "Bearer " + ownerJwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].isActive", everyItem(is(false))));
    }

    @Test
    @DisplayName("5. STAFF calling /api/v1/staff management endpoint returns 403 Forbidden")
    void testStaffCannotAccessStaffManagement() throws Exception {
        mockMvc.perform(get("/api/v1/staff")
                        .header("Authorization", "Bearer " + staffJwtA1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Cross-tenant staff never appears in OWNER staff list")
    void testCrossTenantStaffExcluded() throws Exception {
        mockMvc.perform(get("/api/v1/staff")
                        .header("Authorization", "Bearer " + ownerJwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + staffB1.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("7. Inactive staff account cannot login (401 Unauthorized)")
    void testInactiveStaffCannotLogin() throws Exception {
        String loginJson = String.format("{\"username\":\"%s\",\"password\":\"StaffPass123!\"}", staffUserA4.getUsername());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. Inactive staff cannot be assigned to new booking")
    void testInactiveStaffCannotBeAssigned() throws Exception {
        Service svc = new Service();
        svc.setTenant(tenantA);
        svc.setName("Service A " + System.currentTimeMillis());
        svc.setPrice(BigDecimal.valueOf(200000));
        svc.setDurationMinutes(60);
        svc.setIsActive(true);
        svc = serviceRepository.saveAndFlush(svc);

        Customer customer = new Customer();
        customer.setTenant(tenantA);
        customer.setName("Customer A");
        customer.setPhone("0988776655");
        customer = customerRepository.saveAndFlush(customer);

        String bookingJson = String.format("{\"customerId\":%d,\"serviceId\":%d,\"staffId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                customer.getId(), svc.getId(), staffA4.getId(),
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(2).withHour(11).withMinute(0));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerJwtA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("9. Historical booking of inactive staff remains intact")
    void testHistoricalBookingOfInactiveStaffPreserved() {
        Service svc = new Service();
        svc.setTenant(tenantA);
        svc.setName("Historic Service " + System.currentTimeMillis());
        svc.setPrice(BigDecimal.valueOf(300000));
        svc.setDurationMinutes(60);
        svc.setIsActive(true);
        svc = serviceRepository.saveAndFlush(svc);

        Customer customer = new Customer();
        customer.setTenant(tenantA);
        customer.setName("Customer Old");
        customer.setPhone("0911223344");
        customer = customerRepository.saveAndFlush(customer);

        Booking booking = new Booking();
        booking.setTenant(tenantA);
        booking.setBookingCode("BK-HISTORIC-999");
        booking.setCustomer(customer);
        booking.setService(svc);
        booking.setStaff(staffA4); // Inactive staff
        booking.setStartTime(LocalDateTime.now().minusDays(5));
        booking.setEndTime(LocalDateTime.now().minusDays(5).plusHours(1));
        booking.setPrice(BigDecimal.valueOf(300000));
        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.saveAndFlush(booking);

        assertNotNull(booking.getId());
        Booking found = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(staffA4.getId(), found.getStaff().getId());
        assertEquals("BK-HISTORIC-999", found.getBookingCode());
    }

    @Test
    @DisplayName("10. Public staff endpoint only exposes active staff, not inactive/deleted staff")
    void testPublicEndpointDoesNotExposeInactiveStaff() throws Exception {
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.id == " + staffA4.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == " + staffA5.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == " + staffA6.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("11. Reactivating an inactive/deleted staff restores isActive=true and isDeleted=false")
    void testReactivateStaffRestoresFlags() throws Exception {
        mockMvc.perform(put("/api/v1/staff/" + staffA4.getId())
                        .header("Authorization", "Bearer " + ownerJwtA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + staffA4.getName() + "\",\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.isDeleted").value(false));

        Staff restored = staffRepository.findById(staffA4.getId()).orElseThrow();
        assertTrue(restored.getIsActive());
        assertFalse(restored.getIsDeleted());
    }
}
