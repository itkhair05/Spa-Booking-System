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
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.enums.PaymentMethod;
import com.example.spabooking.payment.enums.PaymentProvider;
import com.example.spabooking.payment.enums.PaymentStatus;
import com.example.spabooking.payment.repository.PaymentRepository;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class PhaseB6PaymentSecurityIntegrationTest {

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

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Tenant tenantA;
    private Tenant tenantB;

    private Staff staffA;
    private Service serviceA;
    private ServiceCategory catA;

    private String ownerTokenA;
    private String staffTokenA;
    private String ownerTokenB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Setup Tenant A
        tenantA = new Tenant();
        tenantA.setName("TIKEY SPA HCM");
        tenantA.setSlug("tikey-b6-a-" + System.currentTimeMillis());
        tenantA.setIsActive(true);
        tenantA.setAddress("789 Dien Bien Phu, Q3, TP.HCM");
        tenantA.setPhone("0988888888");
        tenantA.setEmail("info@tikey.com");
        tenantA = tenantRepository.saveAndFlush(tenantA);

        // Setup Tenant B
        tenantB = new Tenant();
        tenantB.setName("OTHER SPA B");
        tenantB.setSlug("other-b6-b-" + System.currentTimeMillis());
        tenantB.setIsActive(true);
        tenantB = tenantRepository.saveAndFlush(tenantB);

        // Users
        User ownerA = new User();
        ownerA.setUsername("b6_owner_a_" + System.currentTimeMillis());
        ownerA.setPassword("encoded");
        ownerA.setRole(UserRole.OWNER);
        ownerA.setTenant(tenantA);
        ownerA.setIsActive(true);
        ownerA = userRepository.saveAndFlush(ownerA);
        ownerTokenA = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerA), null, new CustomUserDetails(ownerA).getAuthorities()));

        User staffUserA = new User();
        staffUserA.setUsername("b6_staff_a_" + System.currentTimeMillis());
        staffUserA.setPassword("encoded");
        staffUserA.setRole(UserRole.STAFF);
        staffUserA.setTenant(tenantA);
        staffUserA.setIsActive(true);
        staffUserA = userRepository.saveAndFlush(staffUserA);
        staffTokenA = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(staffUserA), null, new CustomUserDetails(staffUserA).getAuthorities()));

        User ownerB = new User();
        ownerB.setUsername("b6_owner_b_" + System.currentTimeMillis());
        ownerB.setPassword("encoded");
        ownerB.setRole(UserRole.OWNER);
        ownerB.setTenant(tenantB);
        ownerB.setIsActive(true);
        ownerB = userRepository.saveAndFlush(ownerB);
        ownerTokenB = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ownerB), null, new CustomUserDetails(ownerB).getAuthorities()));

        // Service & Category
        catA = new ServiceCategory();
        catA.setTenant(tenantA);
        catA.setName("Chăm sóc trị liệu");
        catA.setDisplayOrder(1);
        catA.setIsActive(true);
        catA = serviceCategoryRepository.saveAndFlush(catA);

        serviceA = new Service();
        serviceA.setTenant(tenantA);
        serviceA.setCategory(catA);
        serviceA.setName("Trị liệu chuyên sâu thảo dược");
        serviceA.setDescription("Liệu trình phục hồi da và cơ bắp với thảo dược");
        serviceA.setProcessSteps("[\"1. Rửa mặt\", \"2. Xông hơi\", \"3. Đắp thảo dược\"]");
        serviceA.setDurationMinutes(75);
        serviceA.setPrice(new BigDecimal("450000.00"));
        serviceA.setIsActive(true);
        serviceA.setIsFeatured(true);
        serviceA = serviceRepository.saveAndFlush(serviceA);

        // Staff
        staffA = new Staff();
        staffA.setTenant(tenantA);
        staffA.setName("Nguyễn Kỹ Thuật Viên");
        staffA.setEmail("ktv@tikey.com");
        staffA.setPhone("0912345678");
        staffA.setIsActive(true);
        staffA.setIsDeleted(false);
        staffA.setShowOnWebsite(true);
        staffA = staffRepository.saveAndFlush(staffA);
    }

    // =========================================================================
    // 1. PAYMENT CREATION & METHOD ROUTING
    // =========================================================================

    @Test
    @DisplayName("Public booking with PAY_AT_SPA creates UNPAID payment with authoritative price")
    void testCreateBookingPayAtSpa() throws Exception {
        Map<String, Object> payload = Map.of(
                "serviceId", serviceA.getId(),
                "staffId", staffA.getId(),
                "startTime", LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).toString(),
                "customerName", "Trần Khách Hàng",
                "customerPhone", "0901234567",
                "paymentMethod", "PAY_AT_SPA"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingCode", notNullValue()))
                .andExpect(jsonPath("$.paymentMethod", is("PAY_AT_SPA")))
                .andExpect(jsonPath("$.paymentStatus", is("UNPAID")))
                .andExpect(jsonPath("$.price", is(450000.00)))
                .andExpect(jsonPath("$.paymentUrl").doesNotExist())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String code = node.get("bookingCode").asText();

        // Verify authoritative Payment row in DB
        Payment payment = paymentRepository.findByBookingBookingCodeAndTenantId(code, tenantA.getId()).orElseThrow();
        assertEquals(PaymentMethod.PAY_AT_SPA, payment.getPaymentMethod());
        assertEquals(PaymentStatus.UNPAID, payment.getStatus());
        assertEquals(new BigDecimal("450000.00"), payment.getAmount());
        assertNull(payment.getPaidAt());
    }

    @Test
    @DisplayName("Public booking with VNPAY creates PENDING payment and returns server-signed paymentUrl")
    void testCreateBookingVnPay() throws Exception {
        Map<String, Object> payload = Map.of(
                "serviceId", serviceA.getId(),
                "staffId", staffA.getId(),
                "startTime", LocalDateTime.now().plusDays(3).withHour(15).withMinute(0).toString(),
                "customerName", "Lê Khách Hàng",
                "customerPhone", "0909876543",
                "paymentMethod", "VNPAY"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod", is("VNPAY")))
                .andExpect(jsonPath("$.paymentStatus", is("PENDING")))
                .andExpect(jsonPath("$.paymentUrl", notNullValue()))
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String paymentUrl = node.get("paymentUrl").asText();
        String code = node.get("bookingCode").asText();

        // Verify URL parameters
        assertTrue(paymentUrl.contains("vnp_Amount=45000000")); // 450,000 * 100
        assertTrue(paymentUrl.contains("vnp_SecureHash="));
        assertTrue(paymentUrl.contains("vnp_TxnRef="));

        // Verify DB payment status
        Payment payment = paymentRepository.findByBookingBookingCodeAndTenantId(code, tenantA.getId()).orElseThrow();
        assertEquals(PaymentMethod.VNPAY, payment.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(new BigDecimal("450000.00"), payment.getAmount());
    }

    // =========================================================================
    // 2. AMOUNT & STATUS TAMPERING DEFENSE
    // =========================================================================

    @Test
    @DisplayName("Tampered client price in request is ignored; payment uses authoritative booking price")
    void testAmountTamperingIgnored() throws Exception {
        // Attempting to send a client-crafted "price" or "amount" in payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("serviceId", serviceA.getId());
        payload.put("staffId", staffA.getId());
        payload.put("startTime", LocalDateTime.now().plusDays(3).withHour(16).withMinute(0).toString());
        payload.put("customerName", "Hacker A");
        payload.put("customerPhone", "0909999999");
        payload.put("price", 1000); // Attempting to tamper price to 1,000 VND
        payload.put("amount", 1000);

        MvcResult result = mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price", is(450000.00))) // Authoritative price from DB
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String code = node.get("bookingCode").asText();

        Payment payment = paymentRepository.findByBookingBookingCodeAndTenantId(code, tenantA.getId()).orElseThrow();
        assertEquals(new BigDecimal("450000.00"), payment.getAmount());
    }

    // =========================================================================
    // 3. VNPAY SIGNATURE VERIFICATION & CALLBACK HANDLING
    // =========================================================================

    private String calculateVnPayHash(Map<String, String> fields, String secret) {
        Map<String, String> vnpFields = new HashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getKey().startsWith("vnp_")
                    && !entry.getKey().equals("vnp_SecureHash")
                    && !entry.getKey().equals("vnp_SecureHashType")
                    && entry.getValue() != null && !entry.getValue().isEmpty()) {
                vnpFields.put(entry.getKey(), entry.getValue());
            }
        }
        List<String> fieldNames = new ArrayList<>(vnpFields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        try {
            for (String fieldName : fieldNames) {
                String fieldValue = vnpFields.get(fieldName);
                if (hashData.length() > 0) {
                    hashData.append('&');
                }
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
            }
            return VNPayConfig.hmacSHA512(secret, hashData.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Valid VNPay callback marks payment as PAID, sets transaction info and is idempotent")
    void testVnPayCallbackSuccessAndIdempotency() throws Exception {
        // 1. Create a booking + payment
        Map<String, Object> payload = Map.of(
                "serviceId", serviceA.getId(),
                "staffId", staffA.getId(),
                "startTime", LocalDateTime.now().plusDays(4).withHour(10).withMinute(0).toString(),
                "customerName", "Hoang Lan",
                "customerPhone", "0903333333",
                "paymentMethod", "VNPAY"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        String code = objectMapper.readTree(result.getResponse().getContentAsString()).get("bookingCode").asText();
        Payment payment = paymentRepository.findByBookingBookingCodeAndTenantId(code, tenantA.getId()).orElseThrow();

        // 2. Prepare VNPay callback parameters
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "45000000"); // 450000 x 100
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan dat lich TIKEY SPA - " + code);
        params.put("vnp_PayDate", "20261005120000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_TransactionNo", "14567890");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", payment.getTxnRef());

        String secureHash = calculateVnPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        // 3. First callback invocation -> PAID
        var req = get("/api/v1/public/spas/" + tenantA.getSlug() + "/payments/vnpay-callback");
        params.forEach(req::param);

        MvcResult cbResult = mockMvc.perform(req).andReturn();
        System.out.println("CALLBACK RESULT: " + cbResult.getResponse().getContentAsString());
        assertEquals(200, cbResult.getResponse().getStatus());
        JsonNode cbJson = objectMapper.readTree(cbResult.getResponse().getContentAsString());
        assertTrue(cbJson.get("success").asBoolean(), "Expected success but got: " + cbJson.get("message").asText());
        assertEquals("PAID", cbJson.get("status").asText());
        assertEquals("14567890", cbJson.get("transactionNo").asText());

        Payment updated = paymentRepository.findByTxnRef(payment.getTxnRef()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getStatus());
        assertNotNull(updated.getPaidAt());
        assertEquals("14567890", updated.getTransactionNo());

        // 4. Duplicate callback invocation (Idempotency test)
        var dupReq = get("/api/v1/public/spas/" + tenantA.getSlug() + "/payments/vnpay-callback");
        params.forEach(dupReq::param);
        mockMvc.perform(dupReq)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status", is("PAID")));

        // Status remains PAID, no exceptions thrown
        Payment duplicateCheck = paymentRepository.findByTxnRef(payment.getTxnRef()).orElseThrow();
        assertEquals(PaymentStatus.PAID, duplicateCheck.getStatus());
    }

    @Test
    @DisplayName("Tampered signature in VNPay callback is rejected with error")
    void testVnPayCallbackInvalidSignatureRejected() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "45000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "TIKEY-INVALID-TXN");
        params.put("vnp_SecureHash", "FAKE_CORRUPTED_HASH_12345");

        mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/payments/vnpay-callback")
                        .param("vnp_Amount", "45000000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "TIKEY-INVALID-TXN")
                        .param("vnp_SecureHash", "FAKE_CORRUPTED_HASH_12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Chữ ký không hợp lệ")));
    }

    private Customer createCustomer(String name, String phone, String email, Tenant tenant) {
        Customer c = new Customer();
        c.setName(name);
        c.setPhone(phone);
        c.setEmail(email);
        c.setTenant(tenant);
        c.setIsActive(true);
        return customerRepository.saveAndFlush(c);
    }

    @Test
    @DisplayName("Amount mismatch in VNPay callback is rejected without marking PAID")
    void testVnPayCallbackAmountMismatchRejected() throws Exception {
        // Booking with price 450,000 VND
        Booking booking = new Booking();
        booking.setTenant(tenantA);
        booking.setCustomer(createCustomer("Test", "0900000001", "test@a.com", tenantA));
        booking.setService(serviceA);
        booking.setStaff(staffA);
        booking.setStartTime(LocalDateTime.now().plusDays(2));
        booking.setEndTime(LocalDateTime.now().plusDays(2).plusMinutes(60));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPrice(new BigDecimal("450000.00"));
        booking.setBookingCode("BK-TAMPER-" + System.currentTimeMillis());
        booking = bookingRepository.saveAndFlush(booking);

        Payment payment = paymentService.createPaymentForBooking(booking, PaymentMethod.VNPAY, PaymentProvider.VNPAY);

        // Hacker crafts VNPay callback with 1,000 VND (100,000 cents) instead of 45,000,000 cents
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "100000"); // 1,000 VND
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", payment.getTxnRef());

        String secureHash = calculateVnPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var req = get("/api/v1/public/spas/" + tenantA.getSlug() + "/payments/vnpay-callback");
        params.forEach(req::param);

        mockMvc.perform(req)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Số tiền giao dịch không khớp")));

        // Status remains PENDING, NOT PAID
        Payment afterCheck = paymentRepository.findByTxnRef(payment.getTxnRef()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, afterCheck.getStatus());
    }

    @Test
    @DisplayName("Customer cancellation response code 24 transitions payment to CANCELLED")
    void testVnPayCallbackCancelled() throws Exception {
        Booking booking = new Booking();
        booking.setTenant(tenantA);
        booking.setCustomer(createCustomer("Cancel Customer", "0900000002", "c@a.com", tenantA));
        booking.setService(serviceA);
        booking.setStaff(staffA);
        booking.setStartTime(LocalDateTime.now().plusDays(2));
        booking.setEndTime(LocalDateTime.now().plusDays(2).plusMinutes(60));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPrice(new BigDecimal("450000.00"));
        booking.setBookingCode("BK-CANCEL-" + System.currentTimeMillis());
        booking = bookingRepository.saveAndFlush(booking);

        Payment payment = paymentService.createPaymentForBooking(booking, PaymentMethod.VNPAY, PaymentProvider.VNPAY);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "45000000");
        params.put("vnp_ResponseCode", "24"); // Customer cancelled code
        params.put("vnp_TxnRef", payment.getTxnRef());

        String secureHash = calculateVnPayHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        var cancelReq = get("/api/v1/public/spas/" + tenantA.getSlug() + "/payments/vnpay-callback");
        params.forEach(cancelReq::param);

        mockMvc.perform(cancelReq)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        Payment afterCheck = paymentRepository.findByTxnRef(payment.getTxnRef()).orElseThrow();
        assertEquals(PaymentStatus.CANCELLED, afterCheck.getStatus());
    }

    // =========================================================================
    // 4. REDESIGNED BOOKING LOOKUP — COMPLETE PUBLIC APPOINTMENT SUMMARY
    // =========================================================================

    @Test
    @DisplayName("Redesigned booking lookup returns full appointment, customer, payment, spa, and service details")
    void testRedesignedBookingLookupComprehensiveDetails() throws Exception {
        Booking booking = new Booking();
        booking.setTenant(tenantA);
        Customer customer = createCustomer("Trần Thị Mai", "0911223344", "mai@example.com", tenantA);
        booking.setCustomer(customer);
        booking.setService(serviceA);
        booking.setStaff(staffA);
        booking.setStartTime(LocalDateTime.of(2026, 10, 15, 10, 0));
        booking.setEndTime(LocalDateTime.of(2026, 10, 15, 11, 15));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPrice(new BigDecimal("450000.00"));
        booking.setBookingCode("BK-LOOKUP-" + System.currentTimeMillis());
        booking = bookingRepository.saveAndFlush(booking);

        Payment payment = paymentService.createPaymentForBooking(booking, PaymentMethod.PAY_AT_SPA, PaymentProvider.LOCAL);

        MvcResult lookupResult = mockMvc.perform(get("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings/" + booking.getBookingCode()))
                .andExpect(status().isOk())
                // Appointment
                .andExpect(jsonPath("$.bookingCode", is(booking.getBookingCode())))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.serviceName", is("Trị liệu chuyên sâu thảo dược")))
                .andExpect(jsonPath("$.categoryName", is("Chăm sóc trị liệu")))
                .andExpect(jsonPath("$.durationMinutes", is(75)))
                .andExpect(jsonPath("$.price", is(450000.00)))
                .andExpect(jsonPath("$.staffName", is("Nguyễn Kỹ Thuật Viên")))
                // Customer
                .andExpect(jsonPath("$.customerName", is("Trần Thị Mai")))
                .andExpect(jsonPath("$.customerPhone", is("0911223344")))
                .andExpect(jsonPath("$.customerEmail", is("mai@example.com")))
                // Payment
                .andExpect(jsonPath("$.paymentMethod", is("PAY_AT_SPA")))
                .andExpect(jsonPath("$.paymentStatus", is("UNPAID")))
                .andExpect(jsonPath("$.paidAmount", is(450000.00)))
                // Spa
                .andExpect(jsonPath("$.spaName", is("TIKEY SPA HCM")))
                .andExpect(jsonPath("$.spaAddress", is("789 Dien Bien Phu, Q3, TP.HCM")))
                .andExpect(jsonPath("$.spaPhone", is("0988888888")))
                .andExpect(jsonPath("$.spaEmail", is("info@tikey.com")))
                // Service details
                .andExpect(jsonPath("$.serviceDescription", containsString("Liệu trình phục hồi da")))
                .andExpect(jsonPath("$.processSteps", containsString("Rửa mặt")))
                // Security boundary: ensure NO sensitive internal fields leaked
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.bookingId").doesNotExist())
                .andExpect(jsonPath("$.staffId").doesNotExist())
                .andExpect(jsonPath("$.customerId").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.hash").doesNotExist())
                .andReturn();
    }

    // =========================================================================
    // 5. TENANT ISOLATION & RBAC SECURITY
    // =========================================================================

    @Test
    @DisplayName("Cross-tenant booking lookup returns 404 Not Found")
    void testCrossTenantBookingLookupRejected() throws Exception {
        Booking bookingInA = new Booking();
        bookingInA.setTenant(tenantA);
        bookingInA.setCustomer(createCustomer("Cust A", "0900000003", "a@a.com", tenantA));
        bookingInA.setService(serviceA);
        bookingInA.setStaff(staffA);
        bookingInA.setStartTime(LocalDateTime.now().plusDays(2));
        bookingInA.setEndTime(LocalDateTime.now().plusDays(2).plusMinutes(60));
        bookingInA.setStatus(BookingStatus.CONFIRMED);
        bookingInA.setPrice(new BigDecimal("450000.00"));
        bookingInA.setBookingCode("BK-TENANT-A-" + System.currentTimeMillis());
        bookingInA = bookingRepository.saveAndFlush(bookingInA);

        // Attempt lookup under Tenant B slug -> 404
        mockMvc.perform(get("/api/v1/public/spas/" + tenantB.getSlug() + "/bookings/" + bookingInA.getBookingCode()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Payment management: OWNER can view; Anonymous gets 401 Unauthorized")
    void testPaymentManagementRBAC() throws Exception {
        // Anonymous request to /api/v1/payments -> 401
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isUnauthorized());

        // OWNER Tenant A request -> 200 OK
        mockMvc.perform(get("/api/v1/payments")
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 6. PHASE B.6.1 PHONE VALIDATION & ADMIN BOOKING DETAIL
    // =========================================================================

    @Test
    @DisplayName("Public Booking: 10-digit phone accepted (even with formatting spaces)")
    void testPublicBooking_Accepted10DigitsWithFormatting() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("serviceId", serviceA.getId());
        payload.put("staffId", staffA.getId());
        payload.put("startTime", LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).toString());
        payload.put("customerName", "Nguyen Van An");
        payload.put("customerPhone", "0901 234 567");
        payload.put("customerEmail", "an.nguyen@example.com");
        payload.put("paymentMethod", "PAY_AT_SPA");

        mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingCode", notNullValue()));

        Customer savedCust = customerRepository.findByPhoneAndTenantId("0901234567", tenantA.getId()).orElse(null);
        assertNotNull(savedCust, "Customer should be saved with normalized 10-digit phone");
        assertEquals("0901234567", savedCust.getPhone());
    }

    @Test
    @DisplayName("Public Booking: Reject 11-digit phone number")
    void testPublicBooking_Reject11Digits() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("serviceId", serviceA.getId());
        payload.put("staffId", staffA.getId());
        payload.put("startTime", LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).toString());
        payload.put("customerName", "Nguyen Van An");
        payload.put("customerPhone", "09012345678");
        payload.put("customerEmail", "an.nguyen@example.com");
        payload.put("paymentMethod", "PAY_AT_SPA");

        mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerPhone", is("Số điện thoại phải gồm đúng 10 chữ số")));
    }

    @Test
    @DisplayName("Public Booking: Reject fewer than 10 digits")
    void testPublicBooking_RejectFewerThan10Digits() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("serviceId", serviceA.getId());
        payload.put("staffId", staffA.getId());
        payload.put("startTime", LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).toString());
        payload.put("customerName", "Nguyen Van An");
        payload.put("customerPhone", "090123456");
        payload.put("customerEmail", "an.nguyen@example.com");
        payload.put("paymentMethod", "PAY_AT_SPA");

        mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerPhone", is("Số điện thoại phải gồm đúng 10 chữ số")));
    }

    @Test
    @DisplayName("Public Booking: Reject malformed phone (letters)")
    void testPublicBooking_RejectMalformedPhone() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("serviceId", serviceA.getId());
        payload.put("staffId", staffA.getId());
        payload.put("startTime", LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).toString());
        payload.put("customerName", "Nguyen Van An");
        payload.put("customerPhone", "0901abc567");
        payload.put("customerEmail", "an.nguyen@example.com");
        payload.put("paymentMethod", "PAY_AT_SPA");

        mockMvc.perform(post("/api/v1/public/spas/" + tenantA.getSlug() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerPhone", is("Số điện thoại phải gồm đúng 10 chữ số")));
    }

    @Test
    @DisplayName("Admin Booking Detail: OWNER sees payment info, service process steps and category without exposing secrets")
    void testAdminBookingDetail_OwnerSeesPaymentAndProcess() throws Exception {
        Booking booking = new Booking();
        booking.setTenant(tenantA);
        Customer customer = createCustomer("VIP Customer", "0987654321", "vip@example.com", tenantA);
        booking.setCustomer(customer);
        booking.setService(serviceA);
        booking.setStaff(staffA);
        booking.setStartTime(LocalDateTime.now().plusDays(3).withHour(14).withMinute(0));
        booking.setEndTime(booking.getStartTime().plusMinutes(75));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPrice(serviceA.getPrice());
        booking.setBookingCode("BK-DETAIL-" + System.currentTimeMillis());
        booking = bookingRepository.saveAndFlush(booking);

        Payment payment = new Payment();
        payment.setTenant(tenantA);
        payment.setBooking(booking);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setAmount(serviceA.getPrice());
        payment.setStatus(PaymentStatus.PAID);
        payment.setTxnRef("TXN-DETAIL-" + System.currentTimeMillis());
        payment.setTransactionNo("14567890");
        payment.setBankCode("NCB");
        payment.setCardType("ATM");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.saveAndFlush(payment);

        mockMvc.perform(get("/api/v1/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingCode", is(booking.getBookingCode())))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.categoryName", is("Chăm sóc trị liệu")))
                .andExpect(jsonPath("$.serviceDescription", notNullValue()))
                .andExpect(jsonPath("$.processSteps", notNullValue()))
                .andExpect(jsonPath("$.paymentMethod", is("VNPAY")))
                .andExpect(jsonPath("$.paymentProvider", is("VNPAY")))
                .andExpect(jsonPath("$.paymentStatus", is("PAID")))
                .andExpect(jsonPath("$.transactionNo", is("14567890")))
                .andExpect(jsonPath("$.bankCode", is("NCB")))
                .andExpect(jsonPath("$.cardType", is("ATM")))
                .andExpect(jsonPath("$.paidAmount", is(450000.0)))
                .andExpect(jsonPath("$.vnp_SecureHash").doesNotExist())
                .andExpect(jsonPath("$.hashSecret").doesNotExist())
                .andExpect(jsonPath("$.secretKey").doesNotExist());
    }
}
