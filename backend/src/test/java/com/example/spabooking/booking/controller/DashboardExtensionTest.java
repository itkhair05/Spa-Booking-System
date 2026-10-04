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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class DashboardExtensionTest {

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

    private String ownerJwt;
    private String staffJwt;
    private String ownerTenantBJwt;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Tenant tenantA = new Tenant();
        tenantA.setName("Tenant A Ext");
        tenantA.setSlug("tenant-a-ext-" + System.currentTimeMillis());
        tenantA = tenantRepository.saveAndFlush(tenantA);

        Tenant tenantB = new Tenant();
        tenantB.setName("Tenant B Ext");
        tenantB.setSlug("tenant-b-ext-" + System.currentTimeMillis());
        tenantB = tenantRepository.saveAndFlush(tenantB);

        User ownerA = new User();
        ownerA.setUsername("ownerAExt" + System.currentTimeMillis());
        ownerA.setPassword("encoded");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA = userRepository.saveAndFlush(ownerA);

        Customer customerA = new Customer();
        customerA.setTenant(tenantA);
        customerA.setName("Alice");
        customerA.setIsActive(true);
        customerA = customerRepository.saveAndFlush(customerA);

        Staff staffA = new Staff();
        staffA.setTenant(tenantA);
        staffA.setName("Staff A");
        staffA.setIsActive(true);
        staffA = staffRepository.saveAndFlush(staffA);

        User staffUserA = new User();
        staffUserA.setUsername("staffAExt" + System.currentTimeMillis());
        staffUserA.setPassword("encoded");
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setTenant(tenantA);
        staffUserA.setStaff(staffA);
        staffUserA = userRepository.saveAndFlush(staffUserA);

        User ownerB = new User();
        ownerB.setUsername("ownerBExt" + System.currentTimeMillis());
        ownerB.setPassword("encoded");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB = userRepository.saveAndFlush(ownerB);

        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));
        staffJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(staffUserA), null, new CustomUserDetails(staffUserA).getAuthorities()));
        ownerTenantBJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));

        com.example.spabooking.service.entity.Service massage = new com.example.spabooking.service.entity.Service();
        massage.setTenant(tenantA);
        massage.setName("Massage");
        massage.setPrice(new BigDecimal("300000.00"));
        massage.setDurationMinutes(60);
        massage.setIsActive(true);
        massage = serviceRepository.saveAndFlush(massage);

        com.example.spabooking.service.entity.Service facial = new com.example.spabooking.service.entity.Service();
        facial.setTenant(tenantA);
        facial.setName("Facial");
        facial.setPrice(new BigDecimal("200000.00"));
        facial.setDurationMinutes(45);
        facial.setIsActive(true);
        facial = serviceRepository.saveAndFlush(facial);

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();

        saveBooking(tenantA, customerA, staffA, massage, todayStart.plusHours(10), BookingStatus.PENDING, new BigDecimal("300000.00"));
        saveBooking(tenantA, customerA, staffA, massage, todayStart.plusHours(14), BookingStatus.COMPLETED, new BigDecimal("300000.00"));
        saveBooking(tenantA, customerA, staffA, massage, todayStart.plusHours(16), BookingStatus.CANCELLED, new BigDecimal("300000.00"));
        saveBooking(tenantA, customerA, staffA, massage, todayStart.minusDays(3).plusHours(10), BookingStatus.COMPLETED, new BigDecimal("300000.00"));
        saveBooking(tenantA, customerA, staffA, facial, todayStart.minusDays(10).plusHours(10), BookingStatus.COMPLETED, new BigDecimal("200000.00"));

        // Tenant B has one completed booking that must never leak into tenant A metrics
        Customer customerB = new Customer();
        customerB.setTenant(tenantB);
        customerB.setName("Bob");
        customerB.setIsActive(true);
        customerB = customerRepository.saveAndFlush(customerB);

        Staff staffB = new Staff();
        staffB.setTenant(tenantB);
        staffB.setName("Staff B");
        staffB.setIsActive(true);
        staffB = staffRepository.saveAndFlush(staffB);

        com.example.spabooking.service.entity.Service serviceB = new com.example.spabooking.service.entity.Service();
        serviceB.setTenant(tenantB);
        serviceB.setName("Tenant B Service");
        serviceB.setPrice(new BigDecimal("999999.00"));
        serviceB.setDurationMinutes(30);
        serviceB.setIsActive(true);
        serviceB = serviceRepository.saveAndFlush(serviceB);

        saveBooking(tenantB, customerB, staffB, serviceB, todayStart.plusHours(9), BookingStatus.COMPLETED, new BigDecimal("999999.00"));
    }

    private void saveBooking(Tenant tenant, Customer customer, Staff staff,
                             com.example.spabooking.service.entity.Service service,
                             LocalDateTime startTime, BookingStatus status, BigDecimal price) {
        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setCustomer(customer);
        booking.setStaff(staff);
        booking.setService(service);
        booking.setStartTime(startTime);
        booking.setEndTime(startTime.plusHours(1));
        booking.setStatus(status);
        booking.setPrice(price);
        bookingRepository.saveAndFlush(booking);
    }

    @Test
    void ownerSeesTotalCompletedRevenueAcrossAllDates() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                // All-time COMPLETED only: 300k + 300k + 200k = 800k; CANCELLED/PENDING excluded, today-only would be 300k
                .andExpect(jsonPath("$.totalCompletedRevenue", is(800000.0)))
                .andExpect(jsonPath("$.todayCompletedRevenue", is(300000.0)));
    }

    @Test
    void ownerSeesSevenDayBookingTrendZeroFilled() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingTrend", hasSize(7)))
                // index 3 = today minus 3 days: 1 completed booking
                .andExpect(jsonPath("$.bookingTrend[3].count", is(1)))
                // index 6 = today: pending + completed (cancelled excluded) = 2
                .andExpect(jsonPath("$.bookingTrend[6].count", is(2)))
                .andExpect(jsonPath("$.bookingTrend[0].count", is(0)))
                .andExpect(jsonPath("$.bookingTrend[5].count", is(0)));
    }

    @Test
    void ownerSeesStatusDistributionForExistingStatusesOnly() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatusDistribution", hasSize(3)))
                .andExpect(jsonPath("$.bookingStatusDistribution[?(@.status == 'PENDING')].count", contains(1)))
                .andExpect(jsonPath("$.bookingStatusDistribution[?(@.status == 'COMPLETED')].count", contains(3)))
                .andExpect(jsonPath("$.bookingStatusDistribution[?(@.status == 'CANCELLED')].count", contains(1)))
                .andExpect(jsonPath("$.bookingStatusDistribution[?(@.status == 'CONFIRMED')]").isEmpty());
    }

    @Test
    void ownerSeesTopBookedServicesOrderedByCount() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popularServices", hasSize(2)))
                .andExpect(jsonPath("$.popularServices[0].serviceName", is("Massage")))
                .andExpect(jsonPath("$.popularServices[0].bookingCount", is(3)))
                .andExpect(jsonPath("$.popularServices[1].serviceName", is("Facial")))
                .andExpect(jsonPath("$.popularServices[1].bookingCount", is(1)));
    }

    @Test
    void staffDashboardHasNoRevenueOrCharts() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + staffJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedRevenue", is(0.0)))
                .andExpect(jsonPath("$.todayCompletedRevenue", is(0.0)))
                .andExpect(jsonPath("$.todayExpectedRevenue", is(0.0)))
                .andExpect(jsonPath("$.bookingTrend", hasSize(0)))
                .andExpect(jsonPath("$.bookingStatusDistribution", hasSize(0)))
                .andExpect(jsonPath("$.popularServices", hasSize(0)));
    }

    @Test
    void dashboardExtensionRespectsTenantIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerTenantBJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedRevenue", is(999999.0)))
                .andExpect(jsonPath("$.bookingTrend", hasSize(7)))
                .andExpect(jsonPath("$.bookingTrend[6].count", is(1)))
                .andExpect(jsonPath("$.bookingStatusDistribution[?(@.status == 'COMPLETED')].count", contains(1)))
                .andExpect(jsonPath("$.popularServices[0].serviceName", is("Tenant B Service")));
    }
}
