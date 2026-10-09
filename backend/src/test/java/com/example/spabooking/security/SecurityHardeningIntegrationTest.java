package com.example.spabooking.security;

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
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.enums.PaymentMethod;
import com.example.spabooking.payment.enums.PaymentProvider;
import com.example.spabooking.payment.enums.PaymentStatus;
import com.example.spabooking.payment.repository.PaymentRepository;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.entity.ServiceCategory;
import com.example.spabooking.service.repository.ServiceCategoryRepository;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "security.rate-limit.enabled=true",
        "app.cors.allowed-origins=https://example.com"
})
public class SecurityHardeningIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private VNPayConfig vnPayConfig;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;

    private Tenant tenantA;
    private Tenant tenantB;
    private Service serviceA;
    private Staff staffA;
    private Customer customerA;
    private String ownerTokenA;
    private String staffTokenA;
    private String ownerTokenB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long ts = System.currentTimeMillis();

        tenantA = new Tenant();
        tenantA.setName("Hardening Spa A");
        tenantA.setSlug("spa-a-" + ts);
        tenantA.setIsActive(true);
        tenantA = tenantRepository.saveAndFlush(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Hardening Spa B");
        tenantB.setSlug("spa-b-" + ts);
        tenantB.setIsActive(true);
        tenantB = tenantRepository.saveAndFlush(tenantB);

        ServiceCategory catA = new ServiceCategory();
        catA.setName("Category A");
        catA.setTenant(tenantA);
        catA = serviceCategoryRepository.saveAndFlush(catA);

        serviceA = new Service();
        serviceA.setName("Massage A");
        serviceA.setCategory(catA);
        serviceA.setTenant(tenantA);
        serviceA.setPrice(new BigDecimal("300000.00"));
        serviceA.setDurationMinutes(60);
        serviceA.setIsActive(true);
        serviceA = serviceRepository.saveAndFlush(serviceA);

        staffA = new Staff();
        staffA.setName("Staff Member A");
        staffA.setPhone("090111" + (ts % 10000));
        staffA.setEmail("staffA_" + ts + "@example.com");
        staffA.setTenant(tenantA);
        staffA.setIsActive(true);
        staffA = staffRepository.saveAndFlush(staffA);

        customerA = new Customer();
        customerA.setName("Customer A");
        customerA.setPhone("091222" + (ts % 10000));
        customerA.setEmail("custA_" + ts + "@example.com");
        customerA.setTenant(tenantA);
        customerA.setIsActive(true);
        customerA = customerRepository.saveAndFlush(customerA);

        User ownerA = new User();
        ownerA.setUsername("ownerA_" + ts);
        ownerA.setPassword("$2a$10$dummyhashedpasswordownerA");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA.setIsActive(true);
        ownerA = userRepository.saveAndFlush(ownerA);

        User staffUserA = new User();
        staffUserA.setUsername("staffA_" + ts);
        staffUserA.setPassword("$2a$10$dummyhashedpasswordstaffA");
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setTenant(tenantA);
        staffUserA.setStaff(staffA);
        staffUserA.setIsActive(true);
        staffUserA = userRepository.saveAndFlush(staffUserA);

        User ownerB = new User();
        ownerB.setUsername("ownerB_" + ts);
        ownerB.setPassword("$2a$10$dummyhashedpasswordownerB");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB.setIsActive(true);
        ownerB = userRepository.saveAndFlush(ownerB);

        ownerTokenA = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));
        staffTokenA = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(staffUserA), null, new CustomUserDetails(staffUserA).getAuthorities()));
        ownerTokenB = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));
    }

    private Payment createTestPayment(PaymentStatus status) {
        Booking booking = new Booking();
        booking.setTenant(tenantA);
        booking.setCustomer(customerA);
        booking.setService(serviceA);
        booking.setStaff(staffA);
        booking.setStartTime(LocalDateTime.now().plusDays(1));
        booking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        booking.setPrice(new BigDecimal("300000.00"));
        booking.setStatus(status == PaymentStatus.REFUNDED || status == PaymentStatus.CANCELLED
                ? BookingStatus.CANCELLED : BookingStatus.PENDING);
        booking = bookingRepository.saveAndFlush(booking);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setTenant(tenantA);
        payment.setAmount(booking.getPrice());
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setStatus(status);
        payment.setTxnRef("TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4));
        return paymentRepository.saveAndFlush(payment);
    }

    private String calculateVNPayHash(Map<String, String> fields, String secret) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        try {
            for (String fieldName : fieldNames) {
                String fieldValue = fields.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                }
            }
            return VNPayConfig.hmacSHA512(secret, hashData.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // 1. Payment State Machine Reversion Guard
    // =========================================================================

    @Test
    @DisplayName("Security: VNPay callback cannot revert REFUNDED payment back to PAID")
    void testVNPayCallbackCannotRevertRefundedPaymentToPaid() throws Exception {
        Payment refundedPayment = createTestPayment(PaymentStatus.REFUNDED);
        refundedPayment.setTransactionNo("TXN_PREV_123");
        refundedPayment = paymentRepository.saveAndFlush(refundedPayment);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "30000000"); // 300,000 * 100
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan test");
        params.put("vnp_PayDate", "20261009120000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "NEW_TXN_999");
        params.put("vnp_TxnRef", refundedPayment.getTxnRef());

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var requestBuilder = get("/api/v1/public/spas/" + tenantA.getSlug() + "/payments/vnpay-callback");
        params.forEach(requestBuilder::param);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        Payment reloaded = paymentRepository.findById(refundedPayment.getId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, reloaded.getStatus(), "Payment status must not be reverted to PAID");
        assertEquals("TXN_PREV_123", reloaded.getTransactionNo(), "TransactionNo must not be overwritten");
        assertEquals(BookingStatus.CANCELLED, reloaded.getBooking().getStatus(), "Booking must remain CANCELLED");
    }

    @Test
    @DisplayName("Security: VNPay callback cannot revert CANCELLED payment back to PAID")
    void testVNPayCallbackCannotRevertCancelledPaymentToPaid() throws Exception {
        Payment cancelledPayment = createTestPayment(PaymentStatus.CANCELLED);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "30000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan test");
        params.put("vnp_PayDate", "20261009120000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "NEW_TXN_888");
        params.put("vnp_TxnRef", cancelledPayment.getTxnRef());

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var requestBuilder = get("/api/v1/public/spas/" + tenantA.getSlug() + "/payments/vnpay-callback");
        params.forEach(requestBuilder::param);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Payment reloaded = paymentRepository.findById(cancelledPayment.getId()).orElseThrow();
        assertEquals(PaymentStatus.CANCELLED, reloaded.getStatus(), "Cancelled payment must not be reverted to PAID");
    }

    @Test
    @DisplayName("Security: VNPay IPN cannot revert REFUNDED payment back to PAID")
    void testVNPayIpnCannotRevertRefundedPaymentToPaid() throws Exception {
        Payment refundedPayment = createTestPayment(PaymentStatus.REFUNDED);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "30000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan test");
        params.put("vnp_PayDate", "20261009120000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "NEW_TXN_777");
        params.put("vnp_TxnRef", refundedPayment.getTxnRef());

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var requestBuilder = post("/api/v1/payments/vnpay-ipn");
        params.forEach(requestBuilder::param);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("02"))
                .andExpect(jsonPath("$.Message").value("Order already confirmed"));

        Payment reloaded = paymentRepository.findById(refundedPayment.getId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, reloaded.getStatus(), "Payment status must remain REFUNDED");
    }

    @Test
    @DisplayName("Security: VNPay callback cannot revert REFUND_FAILED payment back to PAID")
    void testVNPayCallbackCannotRevertRefundFailedPaymentToPaid() throws Exception {
        Payment refundFailedPayment = createTestPayment(PaymentStatus.REFUND_FAILED);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "30000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan test");
        params.put("vnp_PayDate", "20261009120000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "NEW_TXN_666");
        params.put("vnp_TxnRef", refundFailedPayment.getTxnRef());

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var requestBuilder = get("/api/v1/public/spas/" + tenantA.getSlug() + "/payments/vnpay-callback");
        params.forEach(requestBuilder::param);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("REFUND_FAILED"));

        Payment reloaded = paymentRepository.findById(refundFailedPayment.getId()).orElseThrow();
        assertEquals(PaymentStatus.REFUND_FAILED, reloaded.getStatus(), "REFUND_FAILED payment must not be reverted to PAID");
    }

    // =========================================================================
    // 2. Public Feedback Rate Limiting
    // =========================================================================

    @Test
    @DisplayName("Security: Rate limiter throttles excessive public feedback requests (>10/min)")
    void testPublicFeedbackRateLimiting() throws Exception {
        String testIp = "198.51.100.99";
        String feedbackPayload = """
                {
                    "name": "Spam Tester",
                    "phone": "0987654321",
                    "email": "spam@example.com",
                    "type": "OTHER",
                    "message": "Security rate limit test feedback"
                }
                """;

        // First 10 requests should succeed (status 201 Created)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/feedback")
                            .with(req -> {
                                req.setRemoteAddr(testIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(feedbackPayload))
                    .andExpect(status().isCreated());
        }

        // 11th request from the same IP must be rejected with HTTP 429 Too Many Requests
        mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/feedback")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackPayload))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));

        // A different IP should NOT be blocked
        mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/feedback")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.100");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackPayload))
                .andExpect(status().isCreated());
    }

    // =========================================================================
    // 3. Security Headers
    // =========================================================================

    @Test
    @DisplayName("Security: Response contains OWASP hardening security headers")
    void testSecurityHeadersPresent() throws Exception {
        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/services"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    // =========================================================================
    // 4. JWT Validation & Fail-Safe Configuration
    // =========================================================================

    @Test
    @DisplayName("Security: Tampered JWT signature is rejected with 401")
    void testTamperedJwtSignatureRejected() throws Exception {
        String tamperedToken = ownerTokenA.substring(0, ownerTokenA.length() - 4) + "abcd";

        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: JwtUtils fails fast on short or missing secret key")
    void testJwtUtilsFailsFastOnWeakSecret() {
        JwtUtils testJwtUtils = new JwtUtils();

        // Null secret
        ReflectionTestUtils.setField(testJwtUtils, "jwtSecret", null);
        assertThrows(IllegalStateException.class, testJwtUtils::validateSecretKey);

        // Blank secret
        ReflectionTestUtils.setField(testJwtUtils, "jwtSecret", "   ");
        assertThrows(IllegalStateException.class, testJwtUtils::validateSecretKey);

        // Secret shorter than 32 bytes (e.g. 16 bytes)
        ReflectionTestUtils.setField(testJwtUtils, "jwtSecret", "too-short-secret");
        assertThrows(IllegalStateException.class, testJwtUtils::validateSecretKey);

        // Valid 32-byte secret succeeds
        ReflectionTestUtils.setField(testJwtUtils, "jwtSecret", "12345678901234567890123456789012");
        assertDoesNotThrow(testJwtUtils::validateSecretKey);
    }

    // =========================================================================
    // 5. RBAC & Multi-Tenant Isolation
    // =========================================================================

    @Test
    @DisplayName("Security: Staff role is forbidden from customer management")
    void testStaffCannotAccessCustomerManagement() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Cross-tenant booking access is denied / not found")
    void testCrossTenantBookingAccessBlocked() throws Exception {
        Booking bookingA = new Booking();
        bookingA.setTenant(tenantA);
        bookingA.setCustomer(customerA);
        bookingA.setService(serviceA);
        bookingA.setStaff(staffA);
        bookingA.setStartTime(LocalDateTime.now().plusDays(2));
        bookingA.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        bookingA.setPrice(new BigDecimal("300000.00"));
        bookingA.setStatus(BookingStatus.CONFIRMED);
        bookingA = bookingRepository.saveAndFlush(bookingA);

        // Owner B attempts to access Booking A belonging to Tenant A
        mockMvc.perform(get("/api/v1/bookings/" + bookingA.getId())
                        .header("Authorization", "Bearer " + ownerTokenB))
                .andExpect(status().isNotFound());
    }
}
