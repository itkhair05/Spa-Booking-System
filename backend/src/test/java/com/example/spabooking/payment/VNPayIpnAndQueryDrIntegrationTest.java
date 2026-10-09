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
import com.example.spabooking.payment.dto.ProviderQueryResult;
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.enums.PaymentMethod;
import com.example.spabooking.payment.enums.PaymentProvider;
import com.example.spabooking.payment.enums.PaymentStatus;
import com.example.spabooking.payment.repository.PaymentRepository;
import com.example.spabooking.payment.service.PaymentProviderService;
import com.example.spabooking.payment.service.PaymentService;
import com.example.spabooking.payment.service.VNPayService;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.entity.ServiceCategory;
import com.example.spabooking.service.repository.ServiceCategoryRepository;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class VNPayIpnAndQueryDrIntegrationTest {

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
    private PaymentService paymentService;

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private VNPayConfig vnPayConfig;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private PaymentProviderService paymentProviderService;

    private MockMvc mockMvc;

    private Tenant tenantA;
    private Tenant tenantB;

    private User ownerA;
    private User staffUserA;
    private User ownerB;

    private String ownerAJwt;
    private String staffAJwt;
    private String ownerBJwt;

    private Service serviceA;
    private Staff staffA;
    private Customer customerA;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Seed Tenant A
        tenantA = new Tenant();
        tenantA.setName("Orchid Spa Da Nang");
        tenantA.setSlug("orchid-spa-" + System.currentTimeMillis());
        tenantA = tenantRepository.save(tenantA);

        // Seed Staff and catalog for Tenant A first
        staffA = new Staff();
        staffA.setTenant(tenantA);
        staffA.setName("KTV Lan");
        staffA.setIsActive(true);
        staffA.setIsDeleted(false);
        staffA.setShowOnWebsite(true);
        staffA = staffRepository.save(staffA);

        ownerA = new User();
        ownerA.setTenant(tenantA);
        ownerA.setUsername("owner_a_" + System.currentTimeMillis());
        ownerA.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setIsActive(true);
        ownerA = userRepository.save(ownerA);
        ownerAJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));

        staffUserA = new User();
        staffUserA.setTenant(tenantA);
        staffUserA.setUsername("staff_a_" + System.currentTimeMillis());
        staffUserA.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF");
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setStaff(staffA);
        staffUserA.setIsActive(true);
        staffUserA = userRepository.save(staffUserA);
        staffAJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(staffUserA), null, new CustomUserDetails(staffUserA).getAuthorities()));

        // Seed Tenant B
        tenantB = new Tenant();
        tenantB.setName("Lotus Spa Hanoi");
        tenantB.setSlug("lotus-spa-" + System.currentTimeMillis());
        tenantB.setIsActive(true);
        tenantB = tenantRepository.save(tenantB);

        ownerB = new User();
        ownerB.setTenant(tenantB);
        ownerB.setUsername("owner_b_" + System.currentTimeMillis());
        ownerB.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setIsActive(true);
        ownerB = userRepository.save(ownerB);
        ownerBJwt = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));

        // Seed catalog for Tenant A
        ServiceCategory cat = new ServiceCategory();
        cat.setTenant(tenantA);
        cat.setName("Massage");
        cat = serviceCategoryRepository.save(cat);

        serviceA = new Service();
        serviceA.setTenant(tenantA);
        serviceA.setCategory(cat);
        serviceA.setName("Aromatherapy");
        serviceA.setPrice(new BigDecimal("500000.00"));
        serviceA.setDurationMinutes(60);
        serviceA.setIsActive(true);
        serviceA = serviceRepository.save(serviceA);

        customerA = new Customer();
        customerA.setTenant(tenantA);
        customerA.setName("Nguyen Van A");
        customerA.setPhone("0988776655");
        customerA = customerRepository.save(customerA);
    }

    private Payment createTestPayment(Tenant tenant, BigDecimal amount, PaymentStatus status) {
        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setCustomer(customerA);
        booking.setService(serviceA);
        booking.setStaff(staffA);
        booking.setPrice(amount);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingCode("BK" + System.currentTimeMillis() + (int)(Math.random() * 1000));
        booking.setStartTime(LocalDateTime.now().plusDays(2));
        booking.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        booking = bookingRepository.save(booking);

        Payment payment = new Payment();
        payment.setTenant(tenant);
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setStatus(status);
        payment.setTxnRef("TIKEY-" + booking.getBookingCode() + "-" + System.currentTimeMillis());
        return paymentRepository.save(payment);
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
    // IPN Tests (Cases 1 - 10)
    // =========================================================================

    @Test
    @DisplayName("1. Valid success IPN updates PENDING payment to PAID and returns 00")
    void test01_ValidSuccessIpn_TransitionsPendingToPaid() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "50000000"); // 500,000 VND * 100
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan lich hen");
        params.put("vnp_PayDate", "20261008120000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "14999999");
        params.put("vnp_TxnRef", payment.getTxnRef());

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var requestBuilder = post("/api/v1/payments/vnpay-ipn");
        params.forEach(requestBuilder::param);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getStatus());
        assertEquals("14999999", updated.getTransactionNo());
        assertEquals("NCB", updated.getBankCode());
        assertNotNull(updated.getPaidAt());
    }

    @Test
    @DisplayName("2. Invalid signature returns 97 Invalid Checksum")
    void test02_InvalidSignature_Returns97() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        var requestBuilder = post("/api/v1/payments/vnpay-ipn")
                .param("vnp_TmnCode", vnPayConfig.getTmnCode())
                .param("vnp_Amount", "50000000")
                .param("vnp_ResponseCode", "00")
                .param("vnp_TxnRef", payment.getTxnRef())
                .param("vnp_SecureHash", "invalid_signature_hash_12345");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"))
                .andExpect(jsonPath("$.Message").value("Invalid Checksum"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, updated.getStatus());
    }

    @Test
    @DisplayName("3. Missing required parameters returns 99")
    void test03_MissingRequiredParameter_Returns99() throws Exception {
        mockMvc.perform(post("/api/v1/payments/vnpay-ipn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("99"));
    }

    @Test
    @DisplayName("4. Amount mismatch returns 04 Invalid Amount")
    void test04_AmountMismatch_Returns04() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "10000000"); // 100,000 VND instead of 500,000 VND
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", payment.getTxnRef());

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var requestBuilder = post("/api/v1/payments/vnpay-ipn");
        params.forEach(requestBuilder::param);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("04"))
                .andExpect(jsonPath("$.Message").value("Invalid Amount"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, updated.getStatus());
    }

    @Test
    @DisplayName("5. Unknown payment/order returns 01 Order not found")
    void test05_UnknownPayment_Returns01() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "50000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "NON_EXISTENT_TXN_REF_9999");

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var requestBuilder = post("/api/v1/payments/vnpay-ipn");
        params.forEach(requestBuilder::param);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("01"))
                .andExpect(jsonPath("$.Message").value("Order not found"));
    }

    @Test
    @DisplayName("6. Duplicate success IPN returns 02 Order already confirmed")
    void test06_DuplicateSuccessIpn_Returns02() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PAID);
        payment.setTransactionNo("14888888");
        paymentRepository.save(payment);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "50000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "14888888");
        params.put("vnp_TxnRef", payment.getTxnRef());

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var requestBuilder = post("/api/v1/payments/vnpay-ipn");
        params.forEach(requestBuilder::param);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("02"))
                .andExpect(jsonPath("$.Message").value("Order already confirmed"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getStatus());
    }

    @Test
    @DisplayName("7. Failure IPN for pending payment transitions to CANCELLED or FAILED")
    void test07_FailureIpnForPendingPayment_UpdatesStatus() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "50000000");
        params.put("vnp_ResponseCode", "24"); // Customer cancelled
        params.put("vnp_TxnRef", payment.getTxnRef());

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var requestBuilder = post("/api/v1/payments/vnpay-ipn");
        params.forEach(requestBuilder::param);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.CANCELLED, updated.getStatus());
    }

    @Test
    @DisplayName("8. Conflicting IPN after payment is already PAID does NOT downgrade payment")
    void test08_ConflictingIpnAfterPaid_DoesNotDowngrade() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PAID);
        payment.setTransactionNo("14777777");
        paymentRepository.save(payment);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "50000000");
        params.put("vnp_ResponseCode", "24"); // Later failure
        params.put("vnp_TxnRef", payment.getTxnRef());

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var requestBuilder = post("/api/v1/payments/vnpay-ipn");
        params.forEach(requestBuilder::param);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("02"))
                .andExpect(jsonPath("$.Message").value("Order already confirmed"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getStatus(), "Payment status MUST NOT be downgraded from PAID");
    }

    @Test
    @DisplayName("9. Browser return is not required for payment success")
    void test09_BrowserReturnNotRequiredForSuccess() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "50000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "14666666");
        params.put("vnp_TxnRef", payment.getTxnRef());

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        // Direct GET IPN (VNPay can call via GET or POST)
        var requestBuilder = get("/api/v1/payments/vnpay-ipn");
        params.forEach(requestBuilder::param);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getStatus());
    }

    @Test
    @DisplayName("10. IPN does not duplicate business side effects")
    void test10_IpnDoesNotDuplicateBusinessSideEffects() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", "50000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "14555555");
        params.put("vnp_TxnRef", payment.getTxnRef());

        String secureHash = calculateVNPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var firstCall = post("/api/v1/payments/vnpay-ipn");
        params.forEach(firstCall::param);
        mockMvc.perform(firstCall).andExpect(jsonPath("$.RspCode").value("00"));

        var secondCall = post("/api/v1/payments/vnpay-ipn");
        params.forEach(secondCall::param);
        mockMvc.perform(secondCall).andExpect(jsonPath("$.RspCode").value("02"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getStatus());
    }

    // =========================================================================
    // QueryDR Tests (Cases 11 - 19)
    // =========================================================================

    @Test
    @DisplayName("11. Successful reconciliation returns complete response")
    void test11_SuccessfulReconciliation() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.success("00", "00", "99112233", "NCB", "20261008120000", "Success"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getId()))
                .andExpect(jsonPath("$.reconciled").value(true))
                .andExpect(jsonPath("$.discrepancy").value(false))
                .andExpect(jsonPath("$.localStatusBefore").value("PENDING"))
                .andExpect(jsonPath("$.localStatusAfter").value("PAID"));
    }

    @Test
    @DisplayName("12. VNPay success reconciles local pending payment to PAID")
    void test12_VNPaySuccessReconcilesPendingToPaid() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.success("00", "00", "99887766", "VCB", "20261008140000", "Success"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(true));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getStatus());
        assertEquals("99887766", updated.getTransactionNo());
        assertEquals("VCB", updated.getBankCode());
    }

    @Test
    @DisplayName("13. VNPay failure reconciles local pending payment to FAILED")
    void test13_VNPayFailureReconcilesPendingToFailed() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.success("00", "02", null, null, null, "Failed transaction"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(true))
                .andExpect(jsonPath("$.localStatusAfter").value("FAILED"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, updated.getStatus());
    }

    @Test
    @DisplayName("14. Local PAID + remote success is idempotent")
    void test14_LocalPaidAndRemoteSuccess_IsIdempotent() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PAID);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.success("00", "00", "99554433", "NCB", "20261008120000", "Success"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(true))
                .andExpect(jsonPath("$.discrepancy").value(false))
                .andExpect(jsonPath("$.localStatusBefore").value("PAID"))
                .andExpect(jsonPath("$.localStatusAfter").value("PAID"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getStatus());
    }

    @Test
    @DisplayName("15. Local PAID + remote failure is not downgraded; flags discrepancy")
    void test15_LocalPaidAndRemoteFailure_NotDowngraded() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PAID);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.success("00", "02", null, null, null, "Remote failed"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(false))
                .andExpect(jsonPath("$.discrepancy").value(true))
                .andExpect(jsonPath("$.localStatusAfter").value("PAID"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getStatus(), "Payment status MUST NOT be downgraded from PAID");
    }

    @Test
    @DisplayName("16. Remote timeout or error returns failure without mutating payment")
    void test16_RemoteTimeoutOrError_DoesNotMutatePayment() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.error("HTTP_504", "Gateway Timeout"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(false))
                .andExpect(jsonPath("$.discrepancy").value(false));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, updated.getStatus());
    }

    @Test
    @DisplayName("17. Invalid remote response signature handled safely")
    void test17_InvalidRemoteResponseSignature() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.error("97", "Chữ ký phản hồi truy vấn giao dịch không hợp lệ"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(false))
                .andExpect(jsonPath("$.remoteResponseCode").value("97"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, updated.getStatus());
    }

    @Test
    @DisplayName("18. Unauthorized user cannot trigger reconciliation")
    void test18_UnauthorizedUserCannotTriggerReconciliation() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        // Anonymous
        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile"))
                .andExpect(status().isUnauthorized());

        // STAFF role
        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + staffAJwt))
                .andExpect(status().isForbidden());

        // Owner B accessing Tenant A payment (tenant boundary isolation)
        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerBJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("19. OWNER can trigger reconciliation")
    void test19_OwnerCanTriggerReconciliation() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.success("00", "01", null, null, null, "Still pending"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remoteTransactionStatus").value("01"));
    }

    @Test
    @DisplayName("QueryDR request-level failure 02 (invalid TMN) causes NO mutation")
    void testQueryDrRequestFailure_ResponseCode02_NoMutation() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.error("02", "Invalid TMN Code"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(false))
                .andExpect(jsonPath("$.remoteResponseCode").value("02"))
                .andExpect(jsonPath("$.localStatusBefore").value("PENDING"))
                .andExpect(jsonPath("$.localStatusAfter").value("PENDING"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, updated.getStatus(), "Payment status MUST NOT be mutated on QueryDR responseCode=02");
    }

    @Test
    @DisplayName("QueryDR request-level failure 03 (data format error) causes NO mutation")
    void testQueryDrRequestFailure_ResponseCode03_NoMutation() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.error("03", "Data format error"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(false))
                .andExpect(jsonPath("$.remoteResponseCode").value("03"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, updated.getStatus(), "Payment status MUST NOT be mutated on QueryDR responseCode=03");
    }

    @Test
    @DisplayName("QueryDR request-level failure 91 (transaction not found) causes NO mutation")
    void testQueryDrRequestFailure_ResponseCode91_NoMutation() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.error("91", "Transaction not found at VNPay"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(false))
                .andExpect(jsonPath("$.remoteResponseCode").value("91"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, updated.getStatus(), "Payment status MUST NOT be mutated on QueryDR responseCode=91");
    }

    @Test
    @DisplayName("QueryDR request-level failure 97 (invalid signature) causes NO mutation")
    void testQueryDrRequestFailure_ResponseCode97_NoMutation() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.error("97", "Invalid checksum from gateway"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(false))
                .andExpect(jsonPath("$.remoteResponseCode").value("97"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, updated.getStatus(), "Payment status MUST NOT be mutated on QueryDR responseCode=97");
    }

    @Test
    @DisplayName("QueryDR success with transactionStatus=01 leaves payment as PENDING")
    void testQueryDrSuccess_TransactionStatus01_RemainsPending() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.success("00", "01", null, null, null, "Incomplete/Pending"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(true))
                .andExpect(jsonPath("$.discrepancy").value(false))
                .andExpect(jsonPath("$.localStatusBefore").value("PENDING"))
                .andExpect(jsonPath("$.localStatusAfter").value("PENDING"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, updated.getStatus());
    }

    @Test
    @DisplayName("QueryDR success with transactionStatus=04 reports discrepancy and does NOT mutate to PAID or FAILED")
    void testQueryDrSuccess_TransactionStatus04_ConservativeDiscrepancy() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.success("00", "04", null, null, null, "Transaction reversed"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(false))
                .andExpect(jsonPath("$.discrepancy").value(true))
                .andExpect(jsonPath("$.localStatusBefore").value("PENDING"))
                .andExpect(jsonPath("$.localStatusAfter").value("PENDING"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, updated.getStatus(), "Status 04 must NOT mutate payment to PAID or FAILED");
    }

    @Test
    @DisplayName("QueryDR success on terminal CANCELLED payment does NOT upgrade to PAID; reports discrepancy")
    void testQueryDrTerminalCancelled_SuccessRemote_DoesNotUpgrade() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.CANCELLED);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.success("00", "00", "11223344", "NCB", "20261008120000", "Success"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(false))
                .andExpect(jsonPath("$.discrepancy").value(true))
                .andExpect(jsonPath("$.localStatusBefore").value("CANCELLED"))
                .andExpect(jsonPath("$.localStatusAfter").value("CANCELLED"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.CANCELLED, updated.getStatus(), "CANCELLED payment MUST NOT be upgraded to PAID");
    }

    @Test
    @DisplayName("QueryDR success on terminal FAILED payment does NOT upgrade to PAID; reports discrepancy")
    void testQueryDrTerminalFailed_SuccessRemote_DoesNotUpgrade() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.FAILED);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.success("00", "00", "11223344", "NCB", "20261008120000", "Success"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(false))
                .andExpect(jsonPath("$.discrepancy").value(true))
                .andExpect(jsonPath("$.localStatusBefore").value("FAILED"))
                .andExpect(jsonPath("$.localStatusAfter").value("FAILED"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, updated.getStatus(), "FAILED payment MUST NOT be upgraded to PAID");
    }

    // =========================================================================
    // Security & Config Tests (Cases 20 - 22)
    // =========================================================================

    @Test
    @DisplayName("20. VNPay configuration bean contains QueryDR settings")
    void test20_VNPayConfigQueryDrSettings() {
        assertNotNull(vnPayConfig.getQueryDrUrl());
        assertFalse(vnPayConfig.getQueryDrUrl().isBlank());
    }

    @Test
    @DisplayName("21. Public IPN endpoint does not expose internal exception details")
    void test21_PublicIpnExceptionSafety() throws Exception {
        // Calling with completely corrupted parameters should still return clean JSON
        mockMvc.perform(post("/api/v1/payments/vnpay-ipn")
                        .param("vnp_Amount", "not-a-number")
                        .param("vnp_TxnRef", "test")
                        .param("vnp_SecureHash", "wrong"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"));
    }

    @Test
    @DisplayName("22. Reconciliation response contains no sensitive secrets or passwords")
    void test22_ReconciliationResponseNoSecrets() throws Exception {
        Payment payment = createTestPayment(tenantA, new BigDecimal("500000.00"), PaymentStatus.PENDING);

        when(paymentProviderService.queryTransaction(any(), any()))
                .thenReturn(ProviderQueryResult.success("00", "00", "12345", "NCB", "20261008120000", "OK"));

        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + ownerAJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vnp_SecureHash").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.hashSecret").doesNotExist());
    }
}
