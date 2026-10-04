package com.example.spabooking.staff;

import com.example.spabooking.auth.dto.LoginRequest;
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
import com.example.spabooking.staff.service.StaffService;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class InactiveStaffSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private StaffService staffService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Tenant tenant;
    private Staff staff;
    private User staffUser;
    private String staffPassword = "testPassword123";

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenant = new Tenant();
        tenant.setName("TIKEY SPA Test");
        tenant.setSlug("tikey-spa-inactive-" + System.currentTimeMillis());
        tenant = tenantRepository.saveAndFlush(tenant);

        staff = new Staff();
        staff.setTenant(tenant);
        staff.setName("Staff Inactive Test");
        staff.setIsActive(true);
        staff = staffRepository.saveAndFlush(staff);

        staffUser = new User();
        staffUser.setUsername("inactive_staff_" + System.currentTimeMillis());
        staffUser.setPassword(passwordEncoder.encode(staffPassword));
        staffUser.setRole(UserRole.STAFF);
        staffUser.setTenant(tenant);
        staffUser.setStaff(staff);
        staffUser.setIsActive(true);
        staffUser = userRepository.saveAndFlush(staffUser);
    }

    @Test
    void activeStaffCanAuthenticate() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(staffUser.getUsername());
        req.setPassword(staffPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void inactiveStaffUserCannotAuthenticate() throws Exception {
        staffUser.setIsActive(false);
        userRepository.saveAndFlush(staffUser);

        LoginRequest req = new LoginRequest();
        req.setUsername(staffUser.getUsername());
        req.setPassword(staffPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffWithInactiveStaffProfileCannotAuthenticate() throws Exception {
        // Staff domain record is set to inactive
        staff.setIsActive(false);
        staffRepository.saveAndFlush(staff);

        LoginRequest req = new LoginRequest();
        req.setUsername(staffUser.getUsername());
        req.setPassword(staffPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffDeactivatedMidSessionCannotAccessProtectedEndpoints() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(staffUser);
        String token = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // Active token works initially
        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Deactivate staff
        staff.setIsActive(false);
        staffRepository.saveAndFlush(staff);

        // Mid-session token is now rejected because staff is inactive
        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void historicalBookingsRemainIntactAfterDeactivation() {
        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setName("Khách cũ");
        customer.setIsActive(true);
        customer = customerRepository.saveAndFlush(customer);

        Service service = new Service();
        service.setTenant(tenant);
        service.setName("Dịch vụ cũ");
        service.setDurationMinutes(30);
        service.setPrice(new BigDecimal("200000.00"));
        service.setIsActive(true);
        service = serviceRepository.saveAndFlush(service);

        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setCustomer(customer);
        booking.setStaff(staff);
        booking.setService(service);
        booking.setStartTime(LocalDateTime.now().minusDays(2));
        booking.setEndTime(LocalDateTime.now().minusDays(2).plusMinutes(30));
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setPrice(service.getPrice());
        booking = bookingRepository.saveAndFlush(booking);

        // Deactivate staff
        staff.setIsActive(false);
        staffRepository.saveAndFlush(staff);

        // Historical booking still exists and references the inactive staff
        Booking history = bookingRepository.findById(booking.getId()).orElseThrow();
        assertNotNull(history);
        assertEquals(staff.getId(), history.getStaff().getId());
        assertEquals(BookingStatus.COMPLETED, history.getStatus());
    }

    @Test
    void ownerCanReactivateInactiveStaff() throws Exception {
        // Setup: create an OWNER user
        User ownerUser = new User();
        ownerUser.setUsername("owner_reactive_" + System.currentTimeMillis());
        ownerUser.setPassword(passwordEncoder.encode("ownerPass123"));
        ownerUser.setRole(UserRole.OWNER);
        ownerUser.setTenant(tenant);
        ownerUser.setIsActive(true);
        ownerUser = userRepository.saveAndFlush(ownerUser);

        com.example.spabooking.auth.security.CustomUserDetails ownerDetails =
                new com.example.spabooking.auth.security.CustomUserDetails(ownerUser);
        String ownerJwt = jwtUtils.generateJwtToken(
                new UsernamePasswordAuthenticationToken(ownerDetails, null, ownerDetails.getAuthorities()));

        // Deactivate staff via DELETE endpoint (soft delete = isActive=false)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNoContent());

        // Staff DB record should still exist but be inactive
        Staff afterDeactivate = staffRepository.findById(staff.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(afterDeactivate.getIsActive(),
                "Staff record should be inactive after soft delete");

        // Staff user should be inactive
        User linkedUser = userRepository.findByStaffId(staff.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(linkedUser.getIsActive(),
                "User account should be disabled when staff is deactivated");

        // Verify inactive staff cannot login
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername(staffUser.getUsername());
        loginReq.setPassword(staffPassword);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());

        // Reactivate staff via PUT with isActive=true
        String reactivateJson = String.format(
                "{\"name\":\"%s\",\"isActive\":true}", staff.getName());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reactivateJson))
                .andExpect(status().isOk());

        // Staff record should be active again
        Staff afterReactivate = staffRepository.findById(staff.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(afterReactivate.getIsActive(),
                "Staff record should be active after reactivation");

        // User account should also be active again
        User reactivatedUser = userRepository.findByStaffId(staff.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(reactivatedUser.getIsActive(),
                "User account should be enabled after staff reactivation");

        // Reactivated staff can now login
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk());
    }

    @Test
    void ownerCanSeeInactiveStaffInStaffList() throws Exception {
        User ownerUser = new User();
        ownerUser.setUsername("owner_view_inactive_" + System.currentTimeMillis());
        ownerUser.setPassword(passwordEncoder.encode("ownerPass123"));
        ownerUser.setRole(UserRole.OWNER);
        ownerUser.setTenant(tenant);
        ownerUser.setIsActive(true);
        ownerUser = userRepository.saveAndFlush(ownerUser);

        CustomUserDetails ownerDetails = new CustomUserDetails(ownerUser);
        String ownerJwt = jwtUtils.generateJwtToken(
                new UsernamePasswordAuthenticationToken(ownerDetails, null, ownerDetails.getAuthorities()));

        // Deactivate staff
        staff.setIsActive(false);
        staffRepository.saveAndFlush(staff);
        staffUser.setIsActive(false);
        userRepository.saveAndFlush(staffUser);

        // OWNER calls GET /api/v1/staff
        mockMvc.perform(get("/api/v1/staff")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[?(@.id == " + staff.getId() + ")].isActive")
                        .value(false))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[?(@.id == " + staff.getId() + ")].accountEnabled")
                        .value(false));
    }

    @Test
    void staffCannotPerformDeactivation() throws Exception {
        CustomUserDetails staffDetails = new CustomUserDetails(staffUser);
        String staffJwt = jwtUtils.generateJwtToken(
                new UsernamePasswordAuthenticationToken(staffDetails, null, staffDetails.getAuthorities()));

        // STAFF tries to DELETE another or their own staff record
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isForbidden());

        // STAFF tries to PUT staff
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + staffJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\",\"isActive\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateReactivatePreservesSingleStaffAndUserRecord() throws Exception {
        User ownerUser = new User();
        ownerUser.setUsername("owner_single_rec_" + System.currentTimeMillis());
        ownerUser.setPassword(passwordEncoder.encode("ownerPass123"));
        ownerUser.setRole(UserRole.OWNER);
        ownerUser.setTenant(tenant);
        ownerUser.setIsActive(true);
        ownerUser = userRepository.saveAndFlush(ownerUser);

        CustomUserDetails ownerDetails = new CustomUserDetails(ownerUser);
        String ownerJwt = jwtUtils.generateJwtToken(
                new UsernamePasswordAuthenticationToken(ownerDetails, null, ownerDetails.getAuthorities()));

        // 1. Deactivate
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNoContent());

        // 2. Reactivate
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"name\":\"%s\",\"isActive\":true}", staff.getName())))
                .andExpect(status().isOk());

        // 3. Assert exactly 1 Staff record exists for this staff ID
        assertEquals(1, staffRepository.findAllByTenantId(tenant.getId()).stream()
                .filter(s -> s.getId().equals(staff.getId()))
                .count(), "Exactly one Staff entity must exist");

        // 4. Assert exactly 1 linked User account exists for this staff ID
        assertEquals(1, userRepository.findAll().stream()
                .filter(u -> u.getStaff() != null && u.getStaff().getId().equals(staff.getId()))
                .count(), "Exactly one linked User account must exist");
    }
}
