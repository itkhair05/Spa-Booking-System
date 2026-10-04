package com.example.spabooking.booking.controller;

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
import com.example.spabooking.service.repository.ServiceRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class DashboardAndBookingQueryTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private String ownerJwt;
    private String staffJwt;
    private String ownerTenantBJwt;

    private Tenant tenantA;
    private Tenant tenantB;
    
    private Customer customerA;
    private Staff staffA;
    private com.example.spabooking.service.entity.Service serviceA;

    private Booking bookingA1_pending_today;
    private Booking bookingA2_confirmed_today;
    private Booking bookingA3_completed_today;
    private Booking bookingA4_cancelled_today;
    private Booking bookingA5_pending_tomorrow;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a-dash");
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b-dash");
        tenantB = tenantRepository.saveAndFlush(tenantB);

        User ownerA = new User();
        ownerA.setUsername("ownerADash");
        ownerA.setPassword("encoded");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA = userRepository.saveAndFlush(ownerA);

        User staffUserA = new User();
        staffUserA.setUsername("staffADash");
        staffUserA.setPassword("encoded");
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setTenant(tenantA);
        staffUserA = userRepository.saveAndFlush(staffUserA);

        User ownerB = new User();
        ownerB.setUsername("ownerBDash");
        ownerB.setPassword("encoded");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB = userRepository.saveAndFlush(ownerB);

        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));
        staffJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(staffUserA), null, new CustomUserDetails(staffUserA).getAuthorities()));
        ownerTenantBJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));

        customerA = new Customer();
        customerA.setTenant(tenantA);
        customerA.setName("Alice");
        customerA.setIsActive(true);
        customerA = customerRepository.saveAndFlush(customerA);

        staffA = new Staff();
        staffA.setTenant(tenantA);
        staffA.setName("Staff A");
        staffA.setIsActive(true);
        staffA = staffRepository.saveAndFlush(staffA);

        serviceA = new com.example.spabooking.service.entity.Service();
        serviceA.setTenant(tenantA);
        serviceA.setName("Massage");
        serviceA.setPrice(new BigDecimal("300000.00"));
        serviceA.setDurationMinutes(60);
        serviceA.setIsActive(true);
        serviceA = serviceRepository.saveAndFlush(serviceA);

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();

        bookingA1_pending_today = new Booking();
        bookingA1_pending_today.setTenant(tenantA);
        bookingA1_pending_today.setCustomer(customerA);
        bookingA1_pending_today.setStaff(staffA);
        bookingA1_pending_today.setService(serviceA);
        bookingA1_pending_today.setStartTime(todayStart.plusHours(10));
        bookingA1_pending_today.setEndTime(todayStart.plusHours(11));
        bookingA1_pending_today.setStatus(BookingStatus.PENDING);
        bookingA1_pending_today.setPrice(serviceA.getPrice());
        bookingA1_pending_today = bookingRepository.saveAndFlush(bookingA1_pending_today);

        bookingA2_confirmed_today = new Booking();
        bookingA2_confirmed_today.setTenant(tenantA);
        bookingA2_confirmed_today.setCustomer(customerA);
        bookingA2_confirmed_today.setStaff(staffA);
        bookingA2_confirmed_today.setService(serviceA);
        bookingA2_confirmed_today.setStartTime(todayStart.plusHours(12));
        bookingA2_confirmed_today.setEndTime(todayStart.plusHours(13));
        bookingA2_confirmed_today.setStatus(BookingStatus.CONFIRMED);
        bookingA2_confirmed_today.setPrice(serviceA.getPrice());
        bookingA2_confirmed_today = bookingRepository.saveAndFlush(bookingA2_confirmed_today);

        bookingA3_completed_today = new Booking();
        bookingA3_completed_today.setTenant(tenantA);
        bookingA3_completed_today.setCustomer(customerA);
        bookingA3_completed_today.setStaff(staffA);
        bookingA3_completed_today.setService(serviceA);
        bookingA3_completed_today.setStartTime(todayStart.plusHours(14));
        bookingA3_completed_today.setEndTime(todayStart.plusHours(15));
        bookingA3_completed_today.setStatus(BookingStatus.COMPLETED);
        bookingA3_completed_today.setPrice(serviceA.getPrice());
        bookingA3_completed_today = bookingRepository.saveAndFlush(bookingA3_completed_today);

        bookingA4_cancelled_today = new Booking();
        bookingA4_cancelled_today.setTenant(tenantA);
        bookingA4_cancelled_today.setCustomer(customerA);
        bookingA4_cancelled_today.setStaff(staffA);
        bookingA4_cancelled_today.setService(serviceA);
        bookingA4_cancelled_today.setStartTime(todayStart.plusHours(16));
        bookingA4_cancelled_today.setEndTime(todayStart.plusHours(17));
        bookingA4_cancelled_today.setStatus(BookingStatus.CANCELLED);
        bookingA4_cancelled_today.setPrice(serviceA.getPrice());
        bookingA4_cancelled_today = bookingRepository.saveAndFlush(bookingA4_cancelled_today);

        bookingA5_pending_tomorrow = new Booking();
        bookingA5_pending_tomorrow.setTenant(tenantA);
        bookingA5_pending_tomorrow.setCustomer(customerA);
        bookingA5_pending_tomorrow.setStaff(staffA);
        bookingA5_pending_tomorrow.setService(serviceA);
        bookingA5_pending_tomorrow.setStartTime(todayStart.plusDays(1).plusHours(10));
        bookingA5_pending_tomorrow.setEndTime(todayStart.plusDays(1).plusHours(11));
        bookingA5_pending_tomorrow.setStatus(BookingStatus.PENDING);
        bookingA5_pending_tomorrow.setPrice(serviceA.getPrice());
        bookingA5_pending_tomorrow = bookingRepository.saveAndFlush(bookingA5_pending_tomorrow);
    }

    @Test
    void testGetBookingsWithoutFilters() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].customerName", is("Alice")))
                .andExpect(jsonPath("$[0].staffName", is("Staff A")))
                .andExpect(jsonPath("$[0].serviceName", is("Massage")))
                .andExpect(jsonPath("$[0].id", is(bookingA1_pending_today.getId().intValue())))
                .andExpect(jsonPath("$[1].id", is(bookingA2_confirmed_today.getId().intValue())))
                .andExpect(jsonPath("$[2].id", is(bookingA3_completed_today.getId().intValue())))
                .andExpect(jsonPath("$[3].id", is(bookingA4_cancelled_today.getId().intValue())))
                .andExpect(jsonPath("$[4].id", is(bookingA5_pending_tomorrow.getId().intValue())));
    }

    @Test
    void testGetBookingsWithDateFilters() throws Exception {
        LocalDateTime todayStart = LocalDateTime.now(VIETNAM_ZONE).toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        mockMvc.perform(get("/api/v1/bookings")
                        .param("startDate", todayStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .param("endDate", todayEnd.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void testGetBookingsWithStaffId() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .param("staffId", staffA.getId().toString())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void testGetBookingsWithStatus() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .param("status", "CONFIRMED")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("CONFIRMED")));
    }

    @Test
    void testGetBookingsWithCombinedFilters() throws Exception {
        LocalDateTime todayStart = LocalDateTime.now(VIETNAM_ZONE).toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        mockMvc.perform(get("/api/v1/bookings")
                        .param("startDate", todayStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .param("endDate", todayEnd.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testInvalidStatus400() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .param("status", "INVALID_STATUS")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidDateRange400() throws Exception {
        LocalDateTime todayStart = LocalDateTime.now(VIETNAM_ZONE).toLocalDate().atStartOfDay();
        
        mockMvc.perform(get("/api/v1/bookings")
                        .param("startDate", todayStart.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .param("endDate", todayStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testExcessiveDateRange400() throws Exception {
        LocalDateTime todayStart = LocalDateTime.now(VIETNAM_ZONE).toLocalDate().atStartOfDay();
        
        mockMvc.perform(get("/api/v1/bookings")
                        .param("startDate", todayStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .param("endDate", todayStart.plusDays(91).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStartDateOnlyReturns400() throws Exception {
        LocalDateTime todayStart = LocalDateTime.now(VIETNAM_ZONE).toLocalDate().atStartOfDay();
        
        mockMvc.perform(get("/api/v1/bookings")
                        .param("startDate", todayStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Both startDate and endDate must be provided together")));
    }

    @Test
    void testEndDateOnlyReturns400() throws Exception {
        LocalDateTime todayStart = LocalDateTime.now(VIETNAM_ZONE).toLocalDate().atStartOfDay();
        
        mockMvc.perform(get("/api/v1/bookings")
                        .param("endDate", todayStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Both startDate and endDate must be provided together")));
    }

    @Test
    void testInvalidDateFormat400() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .param("startDate", "invalid-date-format")
                        .param("endDate", "2026-10-02T10:00:00")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBookingListTenantIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + ownerTenantBJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testDashboardMetricsPopulated() throws Exception {
        // Today we have: 1 PENDING (300k), 1 CONFIRMED (300k), 1 COMPLETED (300k), 1 CANCELLED (300k)
        // Today active booking count excludes CANCELLED: 3
        // Realized revenue from COMPLETED: 300,000
        // Expected revenue from CONFIRMED: 300,000
        
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayBookingCount", is(3)))
                .andExpect(jsonPath("$.pendingBookingCount", is(2))) // 1 today, 1 tomorrow
                .andExpect(jsonPath("$.confirmedBookingCount", is(1)))
                .andExpect(jsonPath("$.todayExpectedRevenue", is(300000.0)))
                .andExpect(jsonPath("$.todayCompletedRevenue", is(300000.0)));
    }

    @Test
    void testDashboardMetricsEmptyTenant() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerTenantBJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayBookingCount", is(0)))
                .andExpect(jsonPath("$.upcomingBookingCount", is(0)))
                .andExpect(jsonPath("$.pendingBookingCount", is(0)))
                .andExpect(jsonPath("$.confirmedBookingCount", is(0)))
                .andExpect(jsonPath("$.todayExpectedRevenue", is(0.0)));
    }

    @Test
    void testStaffCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isOk());
    }

    @Test
    void testUnauthenticatedDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics"))
                .andExpect(status().isUnauthorized());
    }
}
