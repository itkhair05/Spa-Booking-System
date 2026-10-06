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
import com.example.spabooking.payment.config.VNPayConfig;
import com.example.spabooking.payment.dto.ProviderRefundResult;
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.entity.Refund;
import com.example.spabooking.payment.enums.PaymentMethod;
import com.example.spabooking.payment.enums.PaymentProvider;
import com.example.spabooking.payment.enums.PaymentStatus;
import com.example.spabooking.payment.enums.RefundStatus;
import com.example.spabooking.payment.repository.PaymentRepository;
import com.example.spabooking.payment.repository.RefundRepository;
import com.example.spabooking.payment.service.PaymentProviderService;
import com.example.spabooking.payment.service.RefundService;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.staff.service.StaffScheduleService;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class PhaseB81RefundSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StaffScheduleService staffScheduleService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private RefundService refundService;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private PaymentProviderService mockPaymentProviderService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private Tenant tenantA;
    private Tenant tenantB;

    private Staff staffA1;
    private Service serviceA;
    private Customer customerA;

    private User ownerA;
    private User staffUserA;
    private User ownerB;

    private String ownerAJwt;
    private String staffAJwt;
    private String ownerBJwt;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tenantA = new Tenant();
        tenantA.setName("Spa A");
        tenantA.setSlug("spa-a-" + System.currentTimeMillis());
        tenantA.setTimezone("Asia/Ho_Chi_Minh");
        tenantA.setIsActive(true);
        tenantA = tenantRepository.save(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Spa B");
        tenantB.setSlug("spa-b-" + System.currentTimeMillis());
        tenantB.setTimezone("Asia/Ho_Chi_Minh");
        tenantB.setIsActive(true);
        tenantB = tenantRepository.save(tenantB);

        staffA1 = new Staff();
        staffA1.setName("Staff A1");
        staffA1.setTenant(tenantA);
        staffA1.setIsActive(true);
        staffA1.setIsDeleted(false);
        staffA1.setShowOnWebsite(true);
        staffA1 = staffRepository.save(staffA1);
        staffScheduleService.initDefaultWorkingHours(staffA1);

        serviceA = new Service();
        serviceA.setName("Massage Body Thao Duoc");
        serviceA.setDurationMinutes(60);
        serviceA.setPrice(BigDecimal.valueOf(500000));
        serviceA.setTenant(tenantA);
        serviceA.setIsActive(true);
        serviceA = serviceRepository.save(serviceA);

        customerA = new Customer();
        customerA.setName("Customer Alpha");
        customerA.setPhone("0901234567");
        customerA.setTenant(tenantA);
        customerA.setIsActive(true);
        customerA = customerRepository.save(customerA);

        ownerA = new User();
        ownerA.setUsername("ownerA_" + System.currentTimeMillis());
        ownerA.setPassword("secret");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA.setIsActive(true);
        ownerA = userRepository.save(ownerA);

        staffUserA = new User();
        staffUserA.setUsername("staffA_" + System.currentTimeMillis());
        staffUserA.setPassword("secret");
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setTenant(tenantA);
        staffUserA.setStaff(staffA1);
        staffUserA.setIsActive(true);
        staffUserA = userRepository.save(staffUserA);

        ownerB = new User();
        ownerB.setUsername("ownerB_" + System.currentTimeMillis());
        ownerB.setPassword("secret");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB.setIsActive(true);
        ownerB = userRepository.save(ownerB);

        ownerAJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));
        staffAJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(staffUserA), null, new CustomUserDetails(staffUserA).getAuthorities()));
        ownerBJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));
    }

    private Booking createBooking(Tenant tenant, Customer customer, Staff staff, Service service,
                                  LocalDateTime startTime, BookingStatus status, BigDecimal price) {
        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setCustomer(customer);
        booking.setStaff(staff);
        booking.setService(service);
        booking.setStartTime(startTime);
        booking.setEndTime(startTime.plusMinutes(service.getDurationMinutes()));
        booking.setStatus(status);
        booking.setPrice(price);
        booking.setBookingCode("BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return bookingRepository.save(booking);
    }

    private Payment createPaidVNPayPayment(Booking booking, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setTenant(booking.getTenant());
        payment.setBooking(booking);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PAID);
        payment.setTxnRef("TIKEY-REF-" + booking.getBookingCode());
        payment.setTransactionNo("14567890");
        payment.setPaidAt(LocalDateTime.now().minusHours(2));
        return paymentRepository.save(payment);
    }

    // =========================================================================
    // 1. CANCELLATION POLICY TESTS
    // =========================================================================

    @Test
    @DisplayName("Policy A: Cancel >= 24h before appointment results in 100% refund and 0 fee")
    void testPolicyA_CancelMoreThan24HoursBefore_100PercentRefund() throws Exception {
        // Appointment is 48 hours in the future
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(48);
        Booking booking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CONFIRMED, BigDecimal.valueOf(500000));
        createPaidVNPayPayment(booking, BigDecimal.valueOf(500000));

        mockMvc.perform(get("/api/v1/bookings/{id}/refund-eligibility", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundEligible", is(true)))
                .andExpect(jsonPath("$.originalPaidAmount", is(500000)))
                .andExpect(jsonPath("$.refundAmount", is(500000)))
                .andExpect(jsonPath("$.cancellationFee", is(0)))
                .andExpect(jsonPath("$.policyPercentage", is(100)));
    }

    @Test
    @DisplayName("Policy B: Cancel < 24h before appointment results in 90% refund and 10% cancellation fee")
    void testPolicyB_CancelLessThan24HoursBefore_90PercentRefund() throws Exception {
        // Appointment is only 5 hours in the future
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(5);
        Booking booking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CONFIRMED, BigDecimal.valueOf(500000));
        createPaidVNPayPayment(booking, BigDecimal.valueOf(500000));

        mockMvc.perform(get("/api/v1/bookings/{id}/refund-eligibility", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundEligible", is(true)))
                .andExpect(jsonPath("$.originalPaidAmount", is(500000)))
                .andExpect(jsonPath("$.refundAmount", is(450000)))
                .andExpect(jsonPath("$.cancellationFee", is(50000)))
                .andExpect(jsonPath("$.policyPercentage", is(90)));
    }

    @Test
    @DisplayName("Policy C & D: Completed or NO_SHOW bookings are ineligible for refund")
    void testCompletedAndNoShowIneligibleForRefund() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().minusHours(2);
        Booking completedBooking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.COMPLETED, BigDecimal.valueOf(500000));
        createPaidVNPayPayment(completedBooking, BigDecimal.valueOf(500000));

        mockMvc.perform(get("/api/v1/bookings/{id}/refund-eligibility", completedBooking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundEligible", is(false)))
                .andExpect(jsonPath("$.refundAmount", is(0)));

        Booking noShowBooking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.NO_SHOW, BigDecimal.valueOf(500000));
        createPaidVNPayPayment(noShowBooking, BigDecimal.valueOf(500000));

        mockMvc.perform(get("/api/v1/bookings/{id}/refund-eligibility", noShowBooking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundEligible", is(false)))
                .andExpect(jsonPath("$.refundAmount", is(0)));
    }

    @Test
    @DisplayName("Policy E: Pay at Spa (UNPAID) has no refund")
    void testPayAtSpaUnpaidIneligibleForRefund() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(48);
        Booking booking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CONFIRMED, BigDecimal.valueOf(500000));

        Payment unpaidPayment = new Payment();
        unpaidPayment.setTenant(tenantA);
        unpaidPayment.setBooking(booking);
        unpaidPayment.setPaymentMethod(PaymentMethod.PAY_AT_SPA);
        unpaidPayment.setProvider(PaymentProvider.LOCAL);
        unpaidPayment.setAmount(BigDecimal.valueOf(500000));
        unpaidPayment.setStatus(PaymentStatus.UNPAID);
        unpaidPayment.setTxnRef("TIKEY-LOCAL-" + booking.getBookingCode());
        paymentRepository.save(unpaidPayment);

        mockMvc.perform(get("/api/v1/bookings/{id}/refund-eligibility", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundEligible", is(false)))
                .andExpect(jsonPath("$.refundAmount", is(0)));
    }

    // =========================================================================
    // 2. REFUND EXECUTION & IDEMPOTENCY TESTS
    // =========================================================================

    @Test
    @DisplayName("OWNER initiates refund: Provider success transitions to REFUNDED")
    void testOwnerCanInitiateRefund_Success() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(48);
        Booking booking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CONFIRMED, BigDecimal.valueOf(500000));
        Payment payment = createPaidVNPayPayment(booking, BigDecimal.valueOf(500000));

        // Mock provider returning success
        when(mockPaymentProviderService.refund(any(), any(), any(), any(), any()))
                .thenReturn(new ProviderRefundResult(RefundStatus.REFUNDED, "00", "Hoan tien thanh cong", "VN123456"));

        mockMvc.perform(post("/api/v1/bookings/{id}/refund", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Khách bận việc đột xuất\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REFUNDED")))
                .andExpect(jsonPath("$.refundAmount", is(500000)))
                .andExpect(jsonPath("$.cancellationFee", is(0)))
                .andExpect(jsonPath("$.policyPercentage", is(100)));

        // Verify database state
        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, updatedPayment.getStatus());

        Refund refund = refundRepository.findByPaymentIdAndTenantId(payment.getId(), tenantA.getId()).orElseThrow();
        assertEquals(RefundStatus.REFUNDED, refund.getStatus());
        assertEquals("00", refund.getProviderResponseCode());
        assertNotNull(refund.getProcessedAt());
    }

    @Test
    @DisplayName("Refund Idempotency: Clicking refund twice does not call provider twice and returns existing state")
    void testRefundIdempotency_SameStateReturned() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(48);
        Booking booking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CONFIRMED, BigDecimal.valueOf(500000));
        createPaidVNPayPayment(booking, BigDecimal.valueOf(500000));

        when(mockPaymentProviderService.refund(any(), any(), any(), any(), any()))
                .thenReturn(new ProviderRefundResult(RefundStatus.REFUNDED, "00", "Hoan tien thanh cong", "VN123456"));

        // Call #1
        mockMvc.perform(post("/api/v1/bookings/{id}/refund", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REFUNDED")));

        // Call #2 (Immediate duplicate)
        mockMvc.perform(post("/api/v1/bookings/{id}/refund", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REFUNDED")));

        // Provider should only have been invoked ONCE
        verify(mockPaymentProviderService, times(1)).refund(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Provider failure maps to REFUND_FAILED and does NOT claim REFUNDED")
    void testProviderFailure_MapsToRefundFailed() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(5);
        Booking booking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CONFIRMED, BigDecimal.valueOf(500000));
        Payment payment = createPaidVNPayPayment(booking, BigDecimal.valueOf(500000));

        when(mockPaymentProviderService.refund(any(), any(), any(), any(), any()))
                .thenReturn(new ProviderRefundResult(RefundStatus.REFUND_FAILED, "91", "Giao dich khong tim thay", null));

        mockMvc.perform(post("/api/v1/bookings/{id}/refund", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REFUND_FAILED")))
                .andExpect(jsonPath("$.providerResponseCode", is("91")));

        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.REFUND_FAILED, updatedPayment.getStatus());

        Refund refund = refundRepository.findByPaymentIdAndTenantId(payment.getId(), tenantA.getId()).orElseThrow();
        assertEquals(RefundStatus.REFUND_FAILED, refund.getStatus());
        assertNull(refund.getProcessedAt());
    }

    // =========================================================================
    // 3. SECURITY & RBAC TESTS
    // =========================================================================

    @Test
    @DisplayName("STAFF cannot initiate financial refund (403 Forbidden)")
    void testStaffCannotInitiateRefund() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(48);
        Booking booking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CONFIRMED, BigDecimal.valueOf(500000));
        createPaidVNPayPayment(booking, BigDecimal.valueOf(500000));

        mockMvc.perform(post("/api/v1/bookings/{id}/refund", booking.getId())
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-tenant isolation: OWNER B cannot refund Tenant A booking")
    void testCrossTenantRefund_Rejected() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(48);
        Booking booking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CONFIRMED, BigDecimal.valueOf(500000));
        createPaidVNPayPayment(booking, BigDecimal.valueOf(500000));

        mockMvc.perform(post("/api/v1/bookings/{id}/refund", booking.getId())
                        .header("Authorization", "Bearer " + ownerBJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Public Booking Lookup exposes customer-safe financial information")
    void testPublicBookingLookup_CustomerSafeRefundInfo() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(5);
        Booking booking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CANCELLED, BigDecimal.valueOf(500000));
        Payment payment = createPaidVNPayPayment(booking, BigDecimal.valueOf(500000));

        // Create refund record
        Refund refund = new Refund();
        refund.setTenant(tenantA);
        refund.setBooking(booking);
        refund.setPayment(payment);
        refund.setRefundRequestId("TIKEY-REF-" + booking.getBookingCode());
        refund.setProvider(PaymentProvider.VNPAY);
        refund.setOriginalAmount(BigDecimal.valueOf(500000));
        refund.setRefundAmount(BigDecimal.valueOf(450000));
        refund.setCancellationFee(BigDecimal.valueOf(50000));
        refund.setPolicyPercentage(90);
        refund.setStatus(RefundStatus.REFUNDED);
        refund.setRequestedAt(LocalDateTime.now());
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        mockMvc.perform(get("/api/v1/public/spas/{slug}/bookings/{bookingCode}", tenantA.getSlug(), booking.getBookingCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingCode", is(booking.getBookingCode())))
                .andExpect(jsonPath("$.paymentStatus", is("REFUNDED")))
                .andExpect(jsonPath("$.refundAmount", is(450000)))
                .andExpect(jsonPath("$.cancellationFee", is(50000)))
                .andExpect(jsonPath("$.refundStatus", is("REFUNDED")))
                // Ensure no sensitive or internal data leaked
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.refundRequestId").doesNotExist())
                .andExpect(jsonPath("$.txnRef").doesNotExist());
    }

    // =========================================================================
    // 4. REAL-WORLD REGRESSION & PAYMENT STATE MACHINE TESTS
    // =========================================================================

    @Test
    @DisplayName("Real-world scenario: BK-C826390C5B (350k, VNPay 15696680) with Provider Pending results in REFUND_PENDING, never UNPAID or REFUNDED prematurely")
    void testRealWorldBooking_ProviderPending_ResultsInRefundPending() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(12); // < 24h -> 90%
        Booking booking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CANCELLED, BigDecimal.valueOf(350000));
        String regressionBookingCode = "BK-C826390C5B-TEST-" + System.currentTimeMillis();
        booking.setBookingCode(regressionBookingCode);
        bookingRepository.save(booking);

        Payment payment = new Payment();
        payment.setTenant(tenantA);
        payment.setBooking(booking);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setAmount(BigDecimal.valueOf(350000));
        payment.setStatus(PaymentStatus.PAID);
        payment.setTxnRef("TIKEY-" + regressionBookingCode);
        payment.setTransactionNo("15696680");
        payment.setPaidAt(LocalDateTime.now().minusHours(1));
        paymentRepository.save(payment);

        // Provider returns REFUND_PENDING (vnp_ResponseCode=00, Merchant Portal: "CHỜ DUYỆT")
        when(mockPaymentProviderService.refund(any(), any(), any(), any(), any()))
                .thenReturn(new ProviderRefundResult(RefundStatus.REFUND_PENDING, "00", "Yêu cầu hoàn tiền đã được VNPay tiếp nhận (Chờ duyệt / Đang xử lý)", "15696685"));

        // 1. Check eligibility response
        mockMvc.perform(get("/api/v1/bookings/{id}/refund-eligibility", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundEligible", is(true)))
                .andExpect(jsonPath("$.originalPaidAmount", is(350000)))
                .andExpect(jsonPath("$.refundAmount", is(315000)))
                .andExpect(jsonPath("$.cancellationFee", is(35000)))
                .andExpect(jsonPath("$.policyPercentage", is(90)))
                .andExpect(jsonPath("$.refundPercentage", is(90)))
                .andExpect(jsonPath("$.cancellationFeePercentage", is(10)));

        // 2. Initiate refund
        mockMvc.perform(post("/api/v1/bookings/{id}/refund", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Khách yêu cầu hủy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REFUND_PENDING")))
                .andExpect(jsonPath("$.refundAmount", is(315000)))
                .andExpect(jsonPath("$.cancellationFee", is(35000)))
                .andExpect(jsonPath("$.policyPercentage", is(90)))
                .andExpect(jsonPath("$.providerResponseCode", is("00")));

        // 3. Verify Database state
        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.REFUND_PENDING, updatedPayment.getStatus());
        assertNotEquals(PaymentStatus.UNPAID, updatedPayment.getStatus());
        assertNotEquals(PaymentStatus.REFUNDED, updatedPayment.getStatus());

        Refund updatedRefund = refundRepository.findByPaymentIdAndTenantId(payment.getId(), tenantA.getId()).orElseThrow();
        assertEquals(RefundStatus.REFUND_PENDING, updatedRefund.getStatus());
        assertEquals("15696685", updatedRefund.getProviderTransactionReference());
        assertNull(updatedRefund.getProcessedAt()); // Not processed until confirmed

        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, updatedBooking.getStatus());

        // 4. Verify Admin Booking Detail
        mockMvc.perform(get("/api/v1/bookings/{id}", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus", is("REFUND_PENDING")))
                .andExpect(jsonPath("$.refundStatus", is("REFUND_PENDING")))
                .andExpect(jsonPath("$.paidAmount", is(350000)))
                .andExpect(jsonPath("$.refundAmount", is(315000)))
                .andExpect(jsonPath("$.cancellationFee", is(35000)))
                .andExpect(jsonPath("$.transactionNo", is("15696680")))
                .andExpect(jsonPath("$.txnRef", is("TIKEY-" + regressionBookingCode)));

        // 5. Verify Public Booking Lookup
        mockMvc.perform(get("/api/v1/public/spas/{slug}/bookings/{bookingCode}", tenantA.getSlug(), booking.getBookingCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus", is("REFUND_PENDING")))
                .andExpect(jsonPath("$.refundStatus", is("REFUND_PENDING")))
                .andExpect(jsonPath("$.refundAmount", is(315000)))
                .andExpect(jsonPath("$.cancellationFee", is(35000)));
    }

    @Test
    @DisplayName("Invalid payment states cannot be refunded: UNPAID, FAILED, and CANCELLED must be rejected")
    void testInvalidPaymentStates_CannotBeRefunded() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(10);

        // Case 1: UNPAID payment
        Booking bookingUnpaid = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CANCELLED, BigDecimal.valueOf(300000));
        Payment paymentUnpaid = new Payment();
        paymentUnpaid.setTenant(tenantA);
        paymentUnpaid.setBooking(bookingUnpaid);
        paymentUnpaid.setPaymentMethod(PaymentMethod.PAY_AT_SPA);
        paymentUnpaid.setProvider(PaymentProvider.LOCAL);
        paymentUnpaid.setAmount(BigDecimal.valueOf(300000));
        paymentUnpaid.setStatus(PaymentStatus.UNPAID);
        paymentUnpaid.setTxnRef("TIKEY-LOCAL-" + bookingUnpaid.getBookingCode());
        paymentRepository.save(paymentUnpaid);

        mockMvc.perform(post("/api/v1/bookings/{id}/refund", bookingUnpaid.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isBadRequest());

        // Case 2: FAILED payment
        Booking bookingFailed = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CANCELLED, BigDecimal.valueOf(300000));
        Payment paymentFailed = new Payment();
        paymentFailed.setTenant(tenantA);
        paymentFailed.setBooking(bookingFailed);
        paymentFailed.setPaymentMethod(PaymentMethod.VNPAY);
        paymentFailed.setProvider(PaymentProvider.VNPAY);
        paymentFailed.setAmount(BigDecimal.valueOf(300000));
        paymentFailed.setStatus(PaymentStatus.FAILED);
        paymentFailed.setTxnRef("TIKEY-FAILED-" + bookingFailed.getBookingCode());
        paymentRepository.save(paymentFailed);

        mockMvc.perform(post("/api/v1/bookings/{id}/refund", bookingFailed.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isBadRequest());

        // Case 3: CANCELLED payment
        Booking bookingCancelled = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CANCELLED, BigDecimal.valueOf(300000));
        Payment paymentCancelled = new Payment();
        paymentCancelled.setTenant(tenantA);
        paymentCancelled.setBooking(bookingCancelled);
        paymentCancelled.setPaymentMethod(PaymentMethod.VNPAY);
        paymentCancelled.setProvider(PaymentProvider.VNPAY);
        paymentCancelled.setAmount(BigDecimal.valueOf(300000));
        paymentCancelled.setStatus(PaymentStatus.CANCELLED);
        paymentCancelled.setTxnRef("TIKEY-CANCELLED-" + bookingCancelled.getBookingCode());
        paymentRepository.save(paymentCancelled);

        mockMvc.perform(post("/api/v1/bookings/{id}/refund", bookingCancelled.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isBadRequest());

        // Provider should never have been invoked for any of these
        verify(mockPaymentProviderService, never()).refund(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Idempotency: When refund is REFUND_PENDING, duplicate refund request returns existing without provider call")
    void testRefundPending_IdempotencyReturnsExistingRecord() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(48);
        Booking booking = createBooking(tenantA, customerA, staffA1, serviceA, appointmentTime, BookingStatus.CONFIRMED, BigDecimal.valueOf(500000));
        createPaidVNPayPayment(booking, BigDecimal.valueOf(500000));

        when(mockPaymentProviderService.refund(any(), any(), any(), any(), any()))
                .thenReturn(new ProviderRefundResult(RefundStatus.REFUND_PENDING, "00", "Dang cho duyet", "REF_PENDING_TXN"));

        // Call #1
        mockMvc.perform(post("/api/v1/bookings/{id}/refund", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REFUND_PENDING")));

        // Call #2
        mockMvc.perform(post("/api/v1/bookings/{id}/refund", booking.getId())
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REFUND_PENDING")));

        // Verify provider was only called once
        verify(mockPaymentProviderService, times(1)).refund(any(), any(), any(), any(), any());
    }
}
