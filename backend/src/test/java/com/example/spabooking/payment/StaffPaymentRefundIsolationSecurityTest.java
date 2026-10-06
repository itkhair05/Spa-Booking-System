package com.example.spabooking.payment;

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
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class StaffPaymentRefundIsolationSecurityTest {

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

    private Tenant tenant;
    private Tenant otherTenant;

    private Staff staffA;
    private Staff staffB;

    private User staffUserA;
    private User staffUserB;
    private User unlinkedStaffUser;
    private User ownerUser;
    private User otherOwnerUser;

    private String staffAJwt;
    private String staffBJwt;
    private String unlinkedStaffJwt;
    private String ownerJwt;
    private String otherOwnerJwt;

    private Booking bookingA;
    private Booking bookingB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenant = new Tenant();
        tenant.setName("TIKEY SPA Payment Isolation");
        tenant.setSlug("tikey-pay-iso-" + System.currentTimeMillis());
        tenant.setIsActive(true);
        tenant = tenantRepository.saveAndFlush(tenant);

        otherTenant = new Tenant();
        otherTenant.setName("Other Spa");
        otherTenant.setSlug("other-spa-" + System.currentTimeMillis());
        otherTenant.setIsActive(true);
        otherTenant = tenantRepository.saveAndFlush(otherTenant);

        staffA = new Staff();
        staffA.setTenant(tenant);
        staffA.setName("Payment Staff A");
        staffA.setIsActive(true);
        staffA = staffRepository.saveAndFlush(staffA);

        staffB = new Staff();
        staffB.setTenant(tenant);
        staffB.setName("Payment Staff B");
        staffB.setIsActive(true);
        staffB = staffRepository.saveAndFlush(staffB);

        staffUserA = createUser("payStaffA_", UserRole.STAFF, tenant, staffA);
        staffUserB = createUser("payStaffB_", UserRole.STAFF, tenant, staffB);
        unlinkedStaffUser = createUser("payStaffUnlinked_", UserRole.STAFF, tenant, null);
        ownerUser = createUser("payOwner_", UserRole.OWNER, tenant, null);
        otherOwnerUser = createUser("payOwnerOther_", UserRole.OWNER, otherTenant, null);

        staffAJwt = jwtFor(staffUserA);
        staffBJwt = jwtFor(staffUserB);
        unlinkedStaffJwt = jwtFor(unlinkedStaffUser);
        ownerJwt = jwtFor(ownerUser);
        otherOwnerJwt = jwtFor(otherOwnerUser);

        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setName("Payment Customer");
        customer.setPhone("0912345678");
        customer.setIsActive(true);
        customer = customerRepository.saveAndFlush(customer);

        Service service = new Service();
        service.setTenant(tenant);
        service.setName("Payment Isolation Service");
        service.setDurationMinutes(60);
        service.setPrice(new BigDecimal("500000.00"));
        service.setIsActive(true);
        service = serviceRepository.saveAndFlush(service);

        LocalDateTime now = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);

        bookingA = createBooking(tenant, customer, staffA, service, now, BookingStatus.CONFIRMED);
        bookingB = createBooking(tenant, customer, staffB, service, now.plusHours(2), BookingStatus.CONFIRMED);

        createPaidVNPayPayment(bookingA, service.getPrice());
        createPaidVNPayPayment(bookingB, service.getPrice());
    }

    private User createUser(String prefix, UserRole role, Tenant userTenant, Staff linkedStaff) {
        User user = new User();
        user.setUsername(prefix + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4));
        user.setPassword("hash");
        user.setRole(role);
        user.setTenant(userTenant);
        user.setStaff(linkedStaff);
        user.setIsActive(true);
        return userRepository.saveAndFlush(user);
    }

    private String jwtFor(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return jwtUtils.generateJwtToken(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private Booking createBooking(Tenant bookingTenant, Customer customer, Staff staff, Service service,
                                  LocalDateTime startTime, BookingStatus status) {
        Booking booking = new Booking();
        booking.setTenant(bookingTenant);
        booking.setCustomer(customer);
        booking.setStaff(staff);
        booking.setService(service);
        booking.setStartTime(startTime);
        booking.setEndTime(startTime.plusMinutes(service.getDurationMinutes()));
        booking.setStatus(status);
        booking.setPrice(service.getPrice());
        booking.setBookingCode("BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return bookingRepository.saveAndFlush(booking);
    }

    private Payment createPaidVNPayPayment(Booking booking, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setTenant(booking.getTenant());
        payment.setBooking(booking);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PAID);
        payment.setTxnRef("TEST-REF-" + booking.getBookingCode() + "-" + UUID.randomUUID().toString().substring(0, 6));
        payment.setTransactionNo("14567890");
        payment.setPaidAt(LocalDateTime.now().minusHours(2));
        return paymentRepository.saveAndFlush(payment);
    }

    // =========================================================================
    // PAYMENT DETAIL ISOLATION
    // =========================================================================

    @Test
    void staffCanViewPaymentOfOwnBooking() throws Exception {
        mockMvc.perform(get("/api/v1/payments/booking/" + bookingA.getId())
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId", is(bookingA.getId().intValue())))
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    void staffCannotViewPaymentOfAnotherStaffBooking() throws Exception {
        mockMvc.perform(get("/api/v1/payments/booking/" + bookingB.getId())
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanViewPaymentOfAnyBooking() throws Exception {
        mockMvc.perform(get("/api/v1/payments/booking/" + bookingB.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")));
    }

    @Test
    void crossTenantOwnerCannotViewPayment() throws Exception {
        mockMvc.perform(get("/api/v1/payments/booking/" + bookingA.getId())
                        .header("Authorization", "Bearer " + otherOwnerJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void unlinkedStaffCannotViewPayment() throws Exception {
        mockMvc.perform(get("/api/v1/payments/booking/" + bookingA.getId())
                        .header("Authorization", "Bearer " + unlinkedStaffJwt))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // REFUND ELIGIBILITY / REFUND DETAIL ISOLATION
    // =========================================================================

    @Test
    void staffCanViewRefundEligibilityOfOwnBooking() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/" + bookingA.getId() + "/refund-eligibility")
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isOk());
    }

    @Test
    void staffCannotViewRefundEligibilityOfAnotherStaffBooking() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/" + bookingB.getId() + "/refund-eligibility")
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotViewRefundOfAnotherStaffBooking() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/" + bookingB.getId() + "/refund")
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffViewsOwnRefundWithoutRefundRecord() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/" + bookingA.getId() + "/refund")
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanViewRefundDataOfAnyBooking() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/" + bookingB.getId() + "/refund-eligibility")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/bookings/" + bookingB.getId() + "/refund")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // UNLINKED STAFF FAIL-CLOSED (DEFENSE IN DEPTH)
    // =========================================================================

    @Test
    void unlinkedStaffCannotListBookings() throws Exception {
        // Without the fail-closed guard an unlinked STAFF account would receive all tenant bookings
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + unlinkedStaffJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void unlinkedStaffCannotAccessBookingDetail() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/" + bookingA.getId())
                        .header("Authorization", "Bearer " + unlinkedStaffJwt))
                .andExpect(status().isForbidden());
    }
}
