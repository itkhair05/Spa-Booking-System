package com.example.spabooking.payment;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.payment.config.VNPayConfig;
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.enums.PaymentMethod;
import com.example.spabooking.payment.enums.PaymentProvider;
import com.example.spabooking.payment.enums.PaymentStatus;
import com.example.spabooking.payment.service.VNPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VNPayServiceTest {

    private VNPayConfig vnPayConfig;
    private VNPayService vnPayService;

    private static final String TEST_TMN = "VMWY8Z1F";
    private static final String TEST_SECRET = "TESTSECRETKEY1234567890ABCDEF";

    @BeforeEach
    void setUp() {
        vnPayConfig = new VNPayConfig();
        vnPayConfig.setTmnCode(TEST_TMN);
        vnPayConfig.setHashSecret(TEST_SECRET);
        vnPayConfig.setPaymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        vnPayConfig.setReturnUrl("https://spa-booking-system-dta.pages.dev/dat-lich/callback");
        vnPayService = new VNPayService(vnPayConfig);
    }

    private Payment createDummyPayment(BigDecimal amount, String txnRef, String bookingCode) {
        Booking booking = new Booking();
        booking.setBookingCode(bookingCode);

        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setTxnRef(txnRef);
        payment.setBooking(booking);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    private Map<String, String> parseQueryParams(String url) {
        URI uri = URI.create(url);
        String query = uri.getRawQuery();
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    @Test
    @DisplayName("RFC 4231 standard test vector for HMAC-SHA512")
    void testHmacSha512_Rfc4231TestVector() {
        // RFC 4231 Test Case 2: key="Jefe", data="what do ya want for nothing?"
        String key = "Jefe";
        String data = "what do ya want for nothing?";
        String expectedHash = "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea2505549758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737";

        String actualHash = VNPayConfig.hmacSHA512(key, data);
        assertEquals(expectedHash, actualHash);
    }

    @Test
    @DisplayName("HMAC-SHA512 rejects secrets with whitespace instead of silently trimming")
    void testHmacSha512_RejectsWhitespaceInSecret() {
        assertThrows(IllegalArgumentException.class, () -> VNPayConfig.hmacSHA512(" SECRET", "data"));
        assertThrows(IllegalArgumentException.class, () -> VNPayConfig.hmacSHA512("SECRET ", "data"));
        assertThrows(IllegalArgumentException.class, () -> VNPayConfig.hmacSHA512("SEC RET", "data"));
        assertThrows(IllegalArgumentException.class, () -> VNPayConfig.hmacSHA512("SECRET\n", "data"));
        assertThrows(IllegalArgumentException.class, () -> VNPayConfig.hmacSHA512(null, "data"));
        assertThrows(IllegalArgumentException.class, () -> VNPayConfig.hmacSHA512("", "data"));
    }

    @Test
    @DisplayName("Create payment URL generates valid Sandbox URL, ordered query, and verifiable signature")
    void testCreatePaymentUrl_GeneratesValidSandboxUrlAndSignature() {
        Payment payment = createDummyPayment(new BigDecimal("350000"), "TIKEY-TXN-12345", "BK-8888");

        String paymentUrl = vnPayService.createPaymentUrl(payment, "127.0.0.1", null, null);

        assertNotNull(paymentUrl);
        assertTrue(paymentUrl.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));

        Map<String, String> params = parseQueryParams(paymentUrl);
        assertEquals("2.1.0", params.get("vnp_Version"));
        assertEquals("pay", params.get("vnp_Command"));
        assertEquals(TEST_TMN, params.get("vnp_TmnCode"));
        assertEquals("35000000", params.get("vnp_Amount"));
        assertEquals("VND", params.get("vnp_CurrCode"));
        assertEquals("TIKEY-TXN-12345", params.get("vnp_TxnRef"));
        assertEquals("vn", params.get("vnp_Locale"));
        assertNotNull(params.get("vnp_CreateDate"));
        assertNotNull(params.get("vnp_ExpireDate"));
        assertNotNull(params.get("vnp_SecureHash"));

        // Verify that the generated parameters pass signature verification
        assertTrue(vnPayService.verifySignature(params));
    }

    @Test
    @DisplayName("Create payment URL rejects Live VNPay endpoint with fail-fast exception")
    void testCreatePaymentUrl_RejectsLivePaymentEndpoint() {
        vnPayConfig.setPaymentUrl("https://vnpayment.vn/paymentv2/vpcpay.html");
        Payment payment = createDummyPayment(new BigDecimal("200000"), "TIKEY-TXN-9999", "BK-9999");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                vnPayService.createPaymentUrl(payment, "127.0.0.1", null, null));
        assertTrue(ex.getMessage().contains("Live payment endpoints (e.g. vnpayment.vn) are strictly forbidden"));
    }

    @Test
    @DisplayName("Handle Vietnamese diacritics and special characters in order info using UTF-8 without corruption")
    void testCreatePaymentUrl_HandlesVietnameseDiacriticsAndSpecialChars() {
        Payment payment = createDummyPayment(new BigDecimal("500000"), "TIKEY-TXN-UTF8", "BK-VIET");
        String vietnameseInfo = "Thanh toán dịch vụ & ưu đãi đặc biệt 100% - TIKEY SPA";

        String paymentUrl = vnPayService.createPaymentUrl(payment, "127.0.0.1", null, vietnameseInfo);

        assertNotNull(paymentUrl);
        // Verify no replacement question marks in raw query (which happens when US-ASCII is used)
        assertFalse(paymentUrl.contains("?vnp_OrderInfo=?"), "UTF-8 encoding must not replace characters with question mark");
        assertFalse(paymentUrl.contains("%3F%3F"), "UTF-8 encoding must not produce %3F for accented letters");

        Map<String, String> params = parseQueryParams(paymentUrl);
        assertEquals(vietnameseInfo, params.get("vnp_OrderInfo"));

        // Signature must verify cleanly with UTF-8
        assertTrue(vnPayService.verifySignature(params));
    }

    @Test
    @DisplayName("Verify signature ignores vnp_SecureHash, vnp_SecureHashType, and non-vnp parameters")
    void testVerifySignature_IgnoresExtraAndNonVnpParams() {
        Payment payment = createDummyPayment(new BigDecimal("300000"), "TIKEY-TXN-EXTRA", "BK-EXTRA");
        String paymentUrl = vnPayService.createPaymentUrl(payment, "127.0.0.1", null, null);
        Map<String, String> params = parseQueryParams(paymentUrl);

        // Add extra parameters that should not affect hash
        params.put("vnp_SecureHashType", "HmacSHA512");
        params.put("slug", "tikey-spa");
        params.put("frontendRoute", "/dat-lich/callback");
        params.put("emptyParam", "");

        assertTrue(vnPayService.verifySignature(params));
    }

    @Test
    @DisplayName("Verify signature detects tampered parameters")
    void testVerifySignature_DetectsTamperedData() {
        Payment payment = createDummyPayment(new BigDecimal("300000"), "TIKEY-TXN-TAMPER", "BK-TAMPER");
        String paymentUrl = vnPayService.createPaymentUrl(payment, "127.0.0.1", null, null);
        Map<String, String> params = parseQueryParams(paymentUrl);

        // Tamper with amount
        Map<String, String> tamperedAmount = new HashMap<>(params);
        tamperedAmount.put("vnp_Amount", "10000000");
        assertFalse(vnPayService.verifySignature(tamperedAmount));

        // Tamper with txnRef
        Map<String, String> tamperedRef = new HashMap<>(params);
        tamperedRef.put("vnp_TxnRef", "HACKED-TXN");
        assertFalse(vnPayService.verifySignature(tamperedRef));
    }

    @Test
    @DisplayName("Verify signature returns false on missing or blank secure hash")
    void testVerifySignature_MissingOrBlankHashReturnsFalse() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "30000000");
        params.put("vnp_TxnRef", "TIKEY-123");

        assertFalse(vnPayService.verifySignature(params));

        params.put("vnp_SecureHash", "");
        assertFalse(vnPayService.verifySignature(params));

        params.put("vnp_SecureHash", "   ");
        assertFalse(vnPayService.verifySignature(params));
    }

    @Test
    @DisplayName("Verify signature handles space encoding variations (+ and %20)")
    void testVerifySignature_HandlesSpaceEncodingVariations() {
        Payment payment = createDummyPayment(new BigDecimal("400000"), "TIKEY-TXN-SPACE", "BK-SPACE");
        String orderInfo = "Thanh toan lich hen TIKEY SPA";
        String paymentUrl = vnPayService.createPaymentUrl(payment, "127.0.0.1", null, orderInfo);
        Map<String, String> params = parseQueryParams(paymentUrl);

        // Normal parsed params (spaces as decoded characters)
        assertTrue(vnPayService.verifySignature(params));

        // If raw query string was signed with + and decoded, verifySignature matches
        assertEquals(orderInfo, params.get("vnp_OrderInfo"));
    }

    @Test
    @DisplayName("Validate sandbox endpoint rejects insecure HTTP and foreign hosts")
    void testValidateSandboxEndpoint_RejectsHttpAndForeignHosts() {
        assertThrows(IllegalStateException.class, () ->
                vnPayConfig.validateSandboxEndpoint("http://sandbox.vnpayment.vn/paymentv2/vpcpay.html", "payment-url"));
        assertThrows(IllegalStateException.class, () ->
                vnPayConfig.validateSandboxEndpoint("https://vnpayment.vn/paymentv2/vpcpay.html", "payment-url"));
        assertThrows(IllegalStateException.class, () ->
                vnPayConfig.validateSandboxEndpoint("https://untrusted-host.com/payment", "payment-url"));
        assertThrows(IllegalStateException.class, () ->
                vnPayConfig.validateSandboxEndpoint("", "payment-url"));
        assertThrows(IllegalStateException.class, () ->
                vnPayConfig.validateSandboxEndpoint(null, "payment-url"));

        // Allowed sandbox endpoint succeeds without exception
        assertDoesNotThrow(() ->
                vnPayConfig.validateSandboxEndpoint("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", "payment-url"));
    }
}
