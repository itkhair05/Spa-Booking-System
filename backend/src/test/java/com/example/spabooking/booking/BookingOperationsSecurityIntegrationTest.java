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
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.enums.PaymentMethod;
import com.example.spabooking.payment.enums.PaymentProvider;
import com.example.spabooking.payment.enums.PaymentStatus;
import com.example.spabooking.payment.repository.PaymentRepository;
import com.example.spabooking.service.entity.Service;
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
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class BookingOperationsSecurityIntegrationTest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Tenant tenantA;
    private Tenant tenantB;

    private Staff staffA1;
    private Staff staffA2;
    private Staff staffB1;

    private User ownerA;
    private User staffUserA1;
    private User staffUserA2;
    private User ownerB;

    private String ownerAJwt;
    private String staffA1Jwt;
    private String staffA2Jwt;
    private String ownerBJwt;

    private Service serviceA;
    private Customer customerA;
    private Booking bookingA1;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long ts = System.currentTimeMillis();

        // 1. Tenants
        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a-" + ts);
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b-" + ts);
        tenantB = tenantRepository.saveAndFlush(tenantB);

        // 2. Staff
        staffA1 = new Staff();
        staffA1.setName("Staff A1");
        staffA1.setTenant(tenantA);
        staffA1.setIsActive(true);
        staffA1 = staffRepository.saveAndFlush(staffA1);

        staffA2 = new Staff();
        staffA2.setName("Staff A2");
        staffA2.setTenant(tenantA);
        staffA2.setIsActive(true);
        staffA2 = staffRepository.saveAndFlush(staffA2);

        staffB1 = new Staff();
        staffB1.setName("Staff B1");
        staffB1.setTenant(tenantB);
        staffB1.setIsActive(true);
        staffB1 = staffRepository.saveAndFlush(staffB1);

        // 3. Users & JWTs
        ownerA = new User();
        ownerA.setUsername("ownerA_" + ts);
        ownerA.setPassword("hash");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA.setIsActive(true);
        ownerA = userRepository.saveAndFlush(ownerA);
        CustomUserDetails userDetailsOwnerA = new CustomUserDetails(ownerA);
        ownerAJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(userDetailsOwnerA, null, userDetailsOwnerA.getAuthorities()));

        staffUserA1 = new User();
        staffUserA1.setUsername("staffA1_" + ts);
        staffUserA1.setPassword("hash");
        staffUserA1.setRole(UserRole.STAFF);
        staffUserA1.setTenant(tenantA);
        staffUserA1.setStaff(staffA1);
        staffUserA1.setIsActive(true);
        staffUserA1 = userRepository.saveAndFlush(staffUserA1);
        CustomUserDetails userDetailsStaffA1 = new CustomUserDetails(staffUserA1);
        staffA1Jwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(userDetailsStaffA1, null, userDetailsStaffA1.getAuthorities()));

        staffUserA2 = new User();
        staffUserA2.setUsername("staffA2_" + ts);
        staffUserA2.setPassword("hash");
        staffUserA2.setRole(UserRole.STAFF);
        staffUserA2.setTenant(tenantA);
        staffUserA2.setStaff(staffA2);
        staffUserA2.setIsActive(true);
        staffUserA2 = userRepository.saveAndFlush(staffUserA2);
        CustomUserDetails userDetailsStaffA2 = new CustomUserDetails(staffUserA2);
        staffA2Jwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(userDetailsStaffA2, null, userDetailsStaffA2.getAuthorities()));

        ownerB = new User();
        ownerB.setUsername("ownerB_" + ts);
        ownerB.setPassword("hash");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB.setIsActive(true);
        ownerB = userRepository.saveAndFlush(ownerB);
        CustomUserDetails userDetailsOwnerB = new CustomUserDetails(ownerB);
        ownerBJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(userDetailsOwnerB, null, userDetailsOwnerB.getAuthorities()));

        // 4. Service & Customer
        serviceA = new Service();
        serviceA.setName("Aromatherapy Massage");
        serviceA.setDurationMinutes(60);
        serviceA.setPrice(new BigDecimal("350000.00"));
        serviceA.setIsActive(true);
        serviceA.setTenant(tenantA);
        serviceA = serviceRepository.saveAndFlush(serviceA);

        customerA = new Customer();
        customerA.setName("Customer Alpha");
        customerA.setPhone("0987654321");
        customerA.setTenant(tenantA);
        customerA.setIsActive(true);
        customerA = customerRepository.saveAndFlush(customerA);

        // 5. Booking assigned to staffA1
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        bookingA1 = new Booking();
        bookingA1.setTenant(tenantA);
        bookingA1.setStaff(staffA1);
        bookingA1.setCustomer(customerA);
        bookingA1.setService(serviceA);
        bookingA1.setStartTime(startTime);
        bookingA1.setEndTime(startTime.plusMinutes(60));
        bookingA1.setPrice(serviceA.getPrice());
        bookingA1.setStatus(BookingStatus.PENDING);
        bookingA1 = bookingRepository.saveAndFlush(bookingA1);
    }

    @Test
    void testOwnerFullLifecycleSuccess() throws Exception {
        // 1. Confirm: PENDING -> CONFIRMED
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/confirm")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.confirmedAt", notNullValue()));

        // 2. Check-in: CONFIRMED -> CHECKED_IN
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/check-in")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CHECKED_IN")))
                .andExpect(jsonPath("$.checkedInAt", notNullValue()));

        // 3. Start: CHECKED_IN -> IN_PROGRESS
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/start")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.startedAt", notNullValue()));

        // 4. Complete: IN_PROGRESS -> COMPLETED
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/complete")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.completedAt", notNullValue()));

        // Verify customer lastVisit was updated
        Customer updatedCustomer = customerRepository.findById(customerA.getId()).orElseThrow();
        assertNotNull(updatedCustomer.getLastVisit(), "Customer lastVisit must be updated upon completion");

        // Verify revenue inclusion
        BigDecimal completedRev = bookingRepository.sumTotalCompletedRevenue(tenantA.getId());
        assertEquals(0, serviceA.getPrice().compareTo(completedRev));
    }

    @Test
    void testStaffCanOperateOwnAssignedBooking() throws Exception {
        // Staff A1 operates booking assigned to themselves
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/confirm")
                        .header("Authorization", "Bearer " + staffA1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/check-in")
                        .header("Authorization", "Bearer " + staffA1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CHECKED_IN")));

        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/start")
                        .header("Authorization", "Bearer " + staffA1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/complete")
                        .header("Authorization", "Bearer " + staffA1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void testStaffCannotOperateOtherStaffBooking() throws Exception {
        // Staff A2 attempts to confirm booking assigned to Staff A1
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/confirm")
                        .header("Authorization", "Bearer " + staffA2Jwt))
                .andExpect(status().isForbidden());

        // Staff A2 attempts to cancel
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/cancel")
                        .header("Authorization", "Bearer " + staffA2Jwt))
                .andExpect(status().isForbidden());

        // Staff A2 attempts to reschedule
        String rescheduleJson = """
                {
                    "startTime": "%s"
                }
                """.formatted(bookingA1.getStartTime().plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/reschedule")
                        .header("Authorization", "Bearer " + staffA2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rescheduleJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testTenantIsolationCrossTenantRejected() throws Exception {
        // Owner B attempts to operate on Tenant A's booking
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/confirm")
                        .header("Authorization", "Bearer " + ownerBJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testInvalidTransitionsRejected() throws Exception {
        // PENDING -> COMPLETED directly is rejected
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/complete")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isBadRequest());

        // PENDING -> CHECKED_IN directly is rejected
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/check-in")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isBadRequest());

        // Confirm it
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/confirm")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk());

        // CONFIRMED -> COMPLETED directly (without checkin and start) is rejected
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/complete")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isBadRequest());

        // Cancel it from CONFIRMED
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/cancel")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Customer requested cancellation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.cancelledAt", notNullValue()))
                .andExpect(jsonPath("$.cancellationReason", is("Customer requested cancellation")));

        // CANCELLED -> COMPLETED is rejected
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/complete")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isBadRequest());

        // CANCELLED -> CONFIRMED is rejected
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/confirm")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testNoShowFlowAndExclusionFromRevenue() throws Exception {
        // Confirm booking first
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/confirm")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk());

        // Mark no-show
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/no-show")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NO_SHOW")))
                .andExpect(jsonPath("$.noShowAt", notNullValue()));

        // Customer lastVisit must NOT be updated
        Customer freshCustomer = customerRepository.findById(customerA.getId()).orElseThrow();
        assertNull(freshCustomer.getLastVisit(), "No-show must NOT update customer lastVisit");

        // NO_SHOW must NOT contribute to completed revenue
        BigDecimal totalRev = bookingRepository.sumTotalCompletedRevenue(tenantA.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(totalRev));

        // NO_SHOW -> COMPLETED is rejected
        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/complete")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRescheduleFlow() throws Exception {
        // Attach payment to bookingA1
        Payment payment = new Payment();
        payment.setTenant(tenantA);
        payment.setBooking(bookingA1);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setStatus(PaymentStatus.PAID);
        payment.setAmount(bookingA1.getPrice());
        payment.setTxnRef("VNP-TEST-" + System.currentTimeMillis());
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.saveAndFlush(payment);

        LocalDateTime newTime = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
        String rescheduleJson = """
                {
                    "startTime": "%s"
                }
                """.formatted(newTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/reschedule")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rescheduleJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime", is(newTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
                .andExpect(jsonPath("$.endTime", is(newTime.plusMinutes(60).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
                .andExpect(jsonPath("$.bookingCode", is(bookingA1.getBookingCode())));

        // Payment record must remain intact and attached
        Payment freshPayment = paymentRepository.findByBookingIdAndTenantId(bookingA1.getId(), tenantA.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, freshPayment.getStatus());
        assertEquals(payment.getTxnRef(), freshPayment.getTxnRef());
    }

    @Test
    void testRescheduleConflictRejected() throws Exception {
        // Create second booking for Staff A1
        LocalDateTime startTime2 = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Booking bookingA2 = new Booking();
        bookingA2.setTenant(tenantA);
        bookingA2.setStaff(staffA1);
        bookingA2.setCustomer(customerA);
        bookingA2.setService(serviceA);
        bookingA2.setStartTime(startTime2);
        bookingA2.setEndTime(startTime2.plusMinutes(60));
        bookingA2.setPrice(serviceA.getPrice());
        bookingA2.setStatus(BookingStatus.CONFIRMED);
        bookingA2 = bookingRepository.saveAndFlush(bookingA2);

        // Attempt to reschedule bookingA1 into bookingA2's slot -> 409 Conflict
        String rescheduleJson = """
                {
                    "startTime": "%s"
                }
                """.formatted(startTime2.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/reschedule")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rescheduleJson))
                .andExpect(status().isConflict());
    }

    @Test
    void testCannotRescheduleCompletedOrCancelledBooking() throws Exception {
        // Mark as completed
        bookingA1.setStatus(BookingStatus.COMPLETED);
        bookingRepository.saveAndFlush(bookingA1);

        String rescheduleJson = """
                {
                    "startTime": "%s"
                }
                """.formatted(LocalDateTime.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/api/v1/bookings/" + bookingA1.getId() + "/reschedule")
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rescheduleJson))
                .andExpect(status().isBadRequest());
    }
}
