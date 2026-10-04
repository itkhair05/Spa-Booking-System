package com.example.spabooking.booking;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RevenueAndStaffIsolationSecurityTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private StaffRepository staffRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;

    private Tenant tenant1;
    private Tenant tenant2;

    private Staff staff1;
    private Staff staff2;

    private Service serviceCheap;
    private Service serviceExpensive;

    private Customer customer1;

    private String ownerJwt;
    private String staff1Jwt;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenant1 = new Tenant();
        tenant1.setName("Spa One");
        tenant1.setSlug("spa-one-" + UUID.randomUUID());
        tenant1 = tenantRepository.saveAndFlush(tenant1);

        tenant2 = new Tenant();
        tenant2.setName("Spa Two");
        tenant2.setSlug("spa-two-" + UUID.randomUUID());
        tenant2 = tenantRepository.saveAndFlush(tenant2);

        User owner = new User();
        owner.setUsername("ownerRev_" + UUID.randomUUID());
        owner.setPassword("encoded");
        owner.setRole(UserRole.OWNER);
        owner.setTenant(tenant1);
        owner = userRepository.saveAndFlush(owner);

        staff1 = new Staff();
        staff1.setName("Staff 1");
        staff1.setTenant(tenant1);
        staff1.setIsActive(true);
        staff1 = staffRepository.saveAndFlush(staff1);

        staff2 = new Staff();
        staff2.setName("Staff 2");
        staff2.setTenant(tenant1);
        staff2.setIsActive(true);
        staff2 = staffRepository.saveAndFlush(staff2);

        User staffUser = new User();
        staffUser.setUsername("staffUserRev_" + UUID.randomUUID());
        staffUser.setPassword("encoded");
        staffUser.setRole(UserRole.STAFF);
        staffUser.setTenant(tenant1);
        staffUser.setStaff(staff1);
        staffUser = userRepository.saveAndFlush(staffUser);

        ownerJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(owner), null, new CustomUserDetails(owner).getAuthorities()));
        staff1Jwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(staffUser), null, new CustomUserDetails(staffUser).getAuthorities()));

        customer1 = new Customer();
        customer1.setName("Customer A");
        customer1.setTenant(tenant1);
        customer1.setIsActive(true);
        customer1 = customerRepository.saveAndFlush(customer1);

        serviceCheap = new Service();
        serviceCheap.setName("Basic Massage");
        serviceCheap.setPrice(new BigDecimal("200000.00"));
        serviceCheap.setDurationMinutes(30);
        serviceCheap.setTenant(tenant1);
        serviceCheap.setIsActive(true);
        serviceCheap = serviceRepository.saveAndFlush(serviceCheap);

        serviceExpensive = new Service();
        serviceExpensive.setName("Premium Spa");
        serviceExpensive.setPrice(new BigDecimal("500000.00"));
        serviceExpensive.setDurationMinutes(90);
        serviceExpensive.setTenant(tenant1);
        serviceExpensive.setIsActive(true);
        serviceExpensive = serviceRepository.saveAndFlush(serviceExpensive);
    }

    private Booking createBooking(Tenant tenant, Staff staff, Service service, BookingStatus status,
                                  LocalDateTime startTime, BigDecimal price) {
        Booking b = new Booking();
        b.setTenant(tenant);
        b.setStaff(staff);
        b.setCustomer(customer1);
        b.setService(service);
        b.setStatus(status);
        b.setStartTime(startTime);
        b.setEndTime(startTime.plusMinutes(service.getDurationMinutes()));
        b.setPrice(price);
        b.setBookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8));
        return bookingRepository.saveAndFlush(b);
    }

    @Test
    void testPendingAndConfirmedDoNotCountAsCompletedRevenue() throws Exception {
        LocalDateTime today = LocalDate.now(VIETNAM_ZONE).atTime(10, 0);

        createBooking(tenant1, staff1, serviceCheap, BookingStatus.PENDING, today, new BigDecimal("200000.00"));
        createBooking(tenant1, staff1, serviceExpensive, BookingStatus.CONFIRMED, today.plusHours(2), new BigDecimal("500000.00"));

        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayCompletedRevenue").value(0))
                .andExpect(jsonPath("$.todayExpectedRevenue", is(500000.0)));
    }

    @Test
    void testCompletedBookingCountsExactlyOnceAndCancelledExcluded() throws Exception {
        LocalDateTime today = LocalDate.now(VIETNAM_ZONE).atTime(11, 0);

        // 1 Completed booking of 200k
        createBooking(tenant1, staff1, serviceCheap, BookingStatus.COMPLETED, today, new BigDecimal("200000.00"));
        // 1 Cancelled booking of 500k
        createBooking(tenant1, staff1, serviceExpensive, BookingStatus.CANCELLED, today.plusHours(1), new BigDecimal("500000.00"));

        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayCompletedRevenue", is(200000.0)))
                .andExpect(jsonPath("$.todayBookingCount", is(1))); // Cancelled booking is excluded from today's active count
    }

    @Test
    void testMultipleCompletedBookingsWithDifferentPricesSumCorrectly() throws Exception {
        LocalDateTime today = LocalDate.now(VIETNAM_ZONE).atTime(9, 0);

        createBooking(tenant1, staff1, serviceCheap, BookingStatus.COMPLETED, today, new BigDecimal("200000.00"));
        createBooking(tenant1, staff2, serviceExpensive, BookingStatus.COMPLETED, today.plusHours(3), new BigDecimal("500000.00"));

        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayCompletedRevenue", is(700000.0)));
    }

    @Test
    void testDateBoundariesRespectVietnamTimezone() throws Exception {
        LocalDate todayDate = LocalDate.now(VIETNAM_ZONE);
        LocalDateTime yesterday = todayDate.minusDays(1).atTime(23, 0);
        LocalDateTime tomorrow = todayDate.plusDays(1).atTime(8, 0);
        LocalDateTime today = todayDate.atTime(14, 0);

        // Yesterday completed (should not be in today's revenue)
        createBooking(tenant1, staff1, serviceCheap, BookingStatus.COMPLETED, yesterday, new BigDecimal("200000.00"));
        // Today completed
        createBooking(tenant1, staff1, serviceExpensive, BookingStatus.COMPLETED, today, new BigDecimal("500000.00"));
        // Tomorrow completed
        createBooking(tenant1, staff1, serviceCheap, BookingStatus.COMPLETED, tomorrow, new BigDecimal("200000.00"));

        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayCompletedRevenue", is(500000.0)));
    }

    @Test
    void testCrossTenantBookingsDoNotAffectMetrics() throws Exception {
        LocalDateTime today = LocalDate.now(VIETNAM_ZONE).atTime(10, 0);

        Customer cust2 = new Customer();
        cust2.setName("Customer B");
        cust2.setTenant(tenant2);
        cust2.setIsActive(true);
        cust2 = customerRepository.saveAndFlush(cust2);

        Staff staffTenant2 = new Staff();
        staffTenant2.setName("Staff T2");
        staffTenant2.setTenant(tenant2);
        staffTenant2.setIsActive(true);
        staffTenant2 = staffRepository.saveAndFlush(staffTenant2);

        Service srv2 = new Service();
        srv2.setName("Other Spa Srv");
        srv2.setPrice(new BigDecimal("999999.00"));
        srv2.setDurationMinutes(60);
        srv2.setTenant(tenant2);
        srv2.setIsActive(true);
        srv2 = serviceRepository.saveAndFlush(srv2);

        // Booking on Tenant 2
        Booking b2 = new Booking();
        b2.setTenant(tenant2);
        b2.setStaff(staffTenant2);
        b2.setCustomer(cust2);
        b2.setService(srv2);
        b2.setStatus(BookingStatus.COMPLETED);
        b2.setStartTime(today);
        b2.setEndTime(today.plusMinutes(60));
        b2.setPrice(new BigDecimal("999999.00"));
        b2.setBookingCode("BK-T2-" + UUID.randomUUID().toString().substring(0, 8));
        bookingRepository.saveAndFlush(b2);

        // Check Tenant 1 owner: must be 0
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayCompletedRevenue").value(0))
                .andExpect(jsonPath("$.todayBookingCount", is(0)));
    }

    @Test
    void testStaffScheduleCountMatchesAssignedBookingsOnly() throws Exception {
        LocalDateTime today = LocalDate.now(VIETNAM_ZONE).atTime(10, 0);

        // 2 bookings assigned to staff 1
        createBooking(tenant1, staff1, serviceCheap, BookingStatus.CONFIRMED, today, new BigDecimal("200000.00"));
        createBooking(tenant1, staff1, serviceExpensive, BookingStatus.COMPLETED, today.plusHours(2), new BigDecimal("500000.00"));

        // 2 bookings assigned to staff 2 (different staff member)
        createBooking(tenant1, staff2, serviceCheap, BookingStatus.CONFIRMED, today.plusHours(4), new BigDecimal("200000.00"));
        createBooking(tenant1, staff2, serviceCheap, BookingStatus.PENDING, today.plusHours(6), new BigDecimal("200000.00"));

        // Staff 1 checks dashboard: must see exactly 2 bookings today (their own), NOT 4!
        // Financial revenue must be 0.0 (hidden from staff)
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + staff1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayBookingCount", is(2)))
                .andExpect(jsonPath("$.todayCompletedRevenue").value(0))
                .andExpect(jsonPath("$.todayExpectedRevenue").value(0));

        // Owner checks dashboard: sees all non-cancelled bookings today: 4
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayBookingCount", is(4)));
    }
}
