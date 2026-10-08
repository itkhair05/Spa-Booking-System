package com.example.spabooking.payment;

import com.example.spabooking.payment.config.VNPayConfig;
import com.example.spabooking.payment.dto.ProviderRefundResult;
import com.example.spabooking.payment.enums.RefundStatus;
import com.example.spabooking.payment.service.VNPayPaymentProviderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VNPayPaymentProviderServiceTest {

    private VNPayPaymentProviderService service;

    @BeforeEach
    void setUp() {
        VNPayConfig config = new VNPayConfig();
        service = new VNPayPaymentProviderService(config, new ObjectMapper(), null);
    }

    @Test
    @DisplayName("VNPay response 00 with null or in-flight transactionStatus maps to REFUND_PENDING")
    void testResponse00_AwaitingApproval_MapsToRefundPending() {
        // Real-world scenario: VNPay returns 00 (request accepted), Merchant portal shows "CHỜ DUYỆT" (status 05 or null)
        ProviderRefundResult resultNullStatus = service.mapResponse("00", null, "Yeu cau da duoc tiep nhan", "15696685");
        assertEquals(RefundStatus.REFUND_PENDING, resultNullStatus.getStatus());
        assertEquals("00", resultNullStatus.getResponseCode());
        assertEquals("15696685", resultNullStatus.getProviderTransactionReference());
        assertTrue(resultNullStatus.getResponseMessage().contains("Chờ duyệt"));

        ProviderRefundResult result05Status = service.mapResponse("00", "05", "VNPAY dang xu ly giao dich", "15696685");
        assertEquals(RefundStatus.REFUND_PENDING, result05Status.getStatus());

        ProviderRefundResult result06Status = service.mapResponse("00", "06", "Da gui sang ngan hang", "15696685");
        assertEquals(RefundStatus.REFUND_PENDING, result06Status.getStatus());
    }

    @Test
    @DisplayName("VNPay response 00 with authoritative transactionStatus 00 maps to REFUNDED")
    void testResponse00_AuthoritativelyCompleted_MapsToRefunded() {
        ProviderRefundResult result = service.mapResponse("00", "00", "Hoan tien thanh cong", "15696685");
        assertEquals(RefundStatus.REFUNDED, result.getStatus());
        assertEquals("00", result.getResponseCode());
        assertEquals("15696685", result.getProviderTransactionReference());
    }

    @Test
    @DisplayName("VNPay response 00 with authoritative transactionStatus 09 (rejected) maps to REFUND_FAILED")
    void testResponse00_RejectedByBank_MapsToRefundFailed() {
        ProviderRefundResult result = service.mapResponse("00", "09", "Tu choi hoan tien", "15696685");
        assertEquals(RefundStatus.REFUND_FAILED, result.getStatus());
    }

    @Test
    @DisplayName("VNPay response 94 (processing) maps to REFUND_PENDING")
    void testResponse94_MapsToRefundPending() {
        ProviderRefundResult result = service.mapResponse("94", null, "Yeu cau dang duoc xu ly", "15696685");
        assertEquals(RefundStatus.REFUND_PENDING, result.getStatus());
    }

    @Test
    @DisplayName("VNPay explicit error codes map to REFUND_FAILED")
    void testResponseErrors_MapToRefundFailed() {
        assertEquals(RefundStatus.REFUND_FAILED, service.mapResponse("02", null, null, null).getStatus());
        assertEquals(RefundStatus.REFUND_FAILED, service.mapResponse("03", null, null, null).getStatus());
        assertEquals(RefundStatus.REFUND_FAILED, service.mapResponse("08", null, null, null).getStatus());
        assertEquals(RefundStatus.REFUND_FAILED, service.mapResponse("16", null, null, null).getStatus());
        assertEquals(RefundStatus.REFUND_FAILED, service.mapResponse("91", null, null, null).getStatus());
        assertEquals(RefundStatus.REFUND_FAILED, service.mapResponse("93", null, null, null).getStatus());
        assertEquals(RefundStatus.REFUND_FAILED, service.mapResponse("95", null, null, null).getStatus());
        assertEquals(RefundStatus.REFUND_FAILED, service.mapResponse("97", null, null, null).getStatus());
        assertEquals(RefundStatus.REFUND_FAILED, service.mapResponse(null, null, null, null).getStatus());
    }

    @Test
    @DisplayName("QueryDR rejects missing payment or missing txnRef")
    void testQueryTransaction_InvalidPaymentParameters() {
        var res1 = service.queryTransaction(null, "127.0.0.1");
        assertFalse(res1.isSuccess());
        assertEquals("ERR_INVALID_PAYMENT", res1.getResponseCode());

        com.example.spabooking.payment.entity.Payment p = new com.example.spabooking.payment.entity.Payment();
        var res2 = service.queryTransaction(p, "127.0.0.1");
        assertFalse(res2.isSuccess());
        assertEquals("ERR_INVALID_TXN_REF", res2.getResponseCode());
    }

    @Test
    @DisplayName("QueryDR rejects response with missing secure hash on responseCode=00")
    void testQueryTransaction_MissingSecureHashOnResponse00() throws Exception {
        VNPayConfig mockConfig = org.mockito.Mockito.mock(VNPayConfig.class);
        org.mockito.Mockito.when(mockConfig.getTmnCode()).thenReturn("TEST_TMN");
        org.mockito.Mockito.when(mockConfig.getHashSecret()).thenReturn("SECRET_KEY_12345678901234567890");
        org.mockito.Mockito.when(mockConfig.getQueryDrUrl()).thenReturn("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");

        java.net.http.HttpClient mockHttpClient = org.mockito.Mockito.mock(java.net.http.HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<String> mockResponse = org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
        org.mockito.Mockito.when(mockResponse.statusCode()).thenReturn(200);
        org.mockito.Mockito.when(mockResponse.body()).thenReturn("{\"vnp_ResponseCode\":\"00\",\"vnp_TransactionStatus\":\"00\"}");
        org.mockito.Mockito.doReturn(mockResponse).when(mockHttpClient).send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        VNPayPaymentProviderService providerService = new VNPayPaymentProviderService(mockConfig, new ObjectMapper(), mockHttpClient);

        com.example.spabooking.payment.entity.Payment payment = new com.example.spabooking.payment.entity.Payment();
        payment.setTxnRef("TIKEY-REF-12345");

        var result = providerService.queryTransaction(payment, "127.0.0.1");
        assertFalse(result.isSuccess());
        assertEquals("97", result.getResponseCode());
        assertTrue(result.getMessage().contains("chữ ký"));
    }

    @Test
    @DisplayName("QueryDR rejects response with invalid secure hash")
    void testQueryTransaction_InvalidSecureHash() throws Exception {
        VNPayConfig mockConfig = org.mockito.Mockito.mock(VNPayConfig.class);
        org.mockito.Mockito.when(mockConfig.getTmnCode()).thenReturn("TEST_TMN");
        org.mockito.Mockito.when(mockConfig.getHashSecret()).thenReturn("SECRET_KEY_12345678901234567890");
        org.mockito.Mockito.when(mockConfig.getQueryDrUrl()).thenReturn("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");

        java.net.http.HttpClient mockHttpClient = org.mockito.Mockito.mock(java.net.http.HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<String> mockResponse = org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
        org.mockito.Mockito.when(mockResponse.statusCode()).thenReturn(200);
        org.mockito.Mockito.when(mockResponse.body()).thenReturn(
                "{\"vnp_ResponseCode\":\"00\",\"vnp_TransactionStatus\":\"00\",\"vnp_SecureHash\":\"INVALID_HASH\"}");
        org.mockito.Mockito.doReturn(mockResponse).when(mockHttpClient).send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        VNPayPaymentProviderService providerService = new VNPayPaymentProviderService(mockConfig, new ObjectMapper(), mockHttpClient);

        com.example.spabooking.payment.entity.Payment payment = new com.example.spabooking.payment.entity.Payment();
        payment.setTxnRef("TIKEY-REF-12345");

        var result = providerService.queryTransaction(payment, "127.0.0.1");
        assertFalse(result.isSuccess());
        assertEquals("97", result.getResponseCode());
    }

    @Test
    @DisplayName("QueryDR with valid signature and responseCode=00 returns success with transactionStatus")
    void testQueryTransaction_ValidSignatureAndResponse00() throws Exception {
        String secret = "SECRET_KEY_12345678901234567890";
        VNPayConfig mockConfig = org.mockito.Mockito.mock(VNPayConfig.class);
        org.mockito.Mockito.when(mockConfig.getTmnCode()).thenReturn("TEST_TMN");
        org.mockito.Mockito.when(mockConfig.getHashSecret()).thenReturn(secret);
        org.mockito.Mockito.when(mockConfig.getQueryDrUrl()).thenReturn("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");

        // Format: vnp_ResponseId|vnp_Command|vnp_ResponseCode|vnp_Message|vnp_TmnCode|vnp_TxnRef|vnp_Amount|vnp_BankCode|vnp_PayDate|vnp_TransactionNo|vnp_TransactionType|vnp_TransactionStatus|vnp_OrderInfo
        String hashData = "1|querydr|00|Success|TEST_TMN|TIKEY-REF-12345|50000000|NCB|20261008120000|998877|01|00|OrderInfo";
        String validHash = VNPayConfig.hmacSHA512(secret, hashData);

        String json = "{"
                + "\"vnp_ResponseId\":\"1\","
                + "\"vnp_Command\":\"querydr\","
                + "\"vnp_ResponseCode\":\"00\","
                + "\"vnp_Message\":\"Success\","
                + "\"vnp_TmnCode\":\"TEST_TMN\","
                + "\"vnp_TxnRef\":\"TIKEY-REF-12345\","
                + "\"vnp_Amount\":\"50000000\","
                + "\"vnp_BankCode\":\"NCB\","
                + "\"vnp_PayDate\":\"20261008120000\","
                + "\"vnp_TransactionNo\":\"998877\","
                + "\"vnp_TransactionType\":\"01\","
                + "\"vnp_TransactionStatus\":\"00\","
                + "\"vnp_OrderInfo\":\"OrderInfo\","
                + "\"vnp_SecureHash\":\"" + validHash + "\""
                + "}";

        java.net.http.HttpClient mockHttpClient = org.mockito.Mockito.mock(java.net.http.HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<String> mockResponse = org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
        org.mockito.Mockito.when(mockResponse.statusCode()).thenReturn(200);
        org.mockito.Mockito.when(mockResponse.body()).thenReturn(json);
        org.mockito.Mockito.doReturn(mockResponse).when(mockHttpClient).send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        VNPayPaymentProviderService providerService = new VNPayPaymentProviderService(mockConfig, new ObjectMapper(), mockHttpClient);

        com.example.spabooking.payment.entity.Payment payment = new com.example.spabooking.payment.entity.Payment();
        payment.setTxnRef("TIKEY-REF-12345");

        var result = providerService.queryTransaction(payment, "127.0.0.1");
        assertTrue(result.isSuccess());
        assertEquals("00", result.getResponseCode());
        assertEquals("00", result.getTransactionStatus());
        assertEquals("998877", result.getTransactionNo());
        assertEquals("NCB", result.getBankCode());
    }

    @Test
    @DisplayName("QueryDR with non-zero responseCode is treated as query failure, never as payment failure")
    void testQueryTransaction_NonZeroResponseCode() throws Exception {
        String secret = "SECRET_KEY_12345678901234567890";
        VNPayConfig mockConfig = org.mockito.Mockito.mock(VNPayConfig.class);
        org.mockito.Mockito.when(mockConfig.getTmnCode()).thenReturn("TEST_TMN");
        org.mockito.Mockito.when(mockConfig.getHashSecret()).thenReturn(secret);
        org.mockito.Mockito.when(mockConfig.getQueryDrUrl()).thenReturn("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");

        // Response with responseCode=02 (invalid TmnCode)
        String hashData = "1|querydr|02|Invalid TMN|TEST_TMN|TIKEY-REF-12345|||||||";
        String validHash = VNPayConfig.hmacSHA512(secret, hashData);

        String json = "{"
                + "\"vnp_ResponseId\":\"1\","
                + "\"vnp_Command\":\"querydr\","
                + "\"vnp_ResponseCode\":\"02\","
                + "\"vnp_Message\":\"Invalid TMN\","
                + "\"vnp_TmnCode\":\"TEST_TMN\","
                + "\"vnp_TxnRef\":\"TIKEY-REF-12345\","
                + "\"vnp_SecureHash\":\"" + validHash + "\""
                + "}";

        java.net.http.HttpClient mockHttpClient = org.mockito.Mockito.mock(java.net.http.HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<String> mockResponse = org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
        org.mockito.Mockito.when(mockResponse.statusCode()).thenReturn(200);
        org.mockito.Mockito.when(mockResponse.body()).thenReturn(json);
        org.mockito.Mockito.doReturn(mockResponse).when(mockHttpClient).send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        VNPayPaymentProviderService providerService = new VNPayPaymentProviderService(mockConfig, new ObjectMapper(), mockHttpClient);

        com.example.spabooking.payment.entity.Payment payment = new com.example.spabooking.payment.entity.Payment();
        payment.setTxnRef("TIKEY-REF-12345");

        var result = providerService.queryTransaction(payment, "127.0.0.1");
        assertFalse(result.isSuccess(), "Non-zero responseCode must be treated as query failure");
        assertEquals("02", result.getResponseCode());
        assertNull(result.getTransactionStatus());
    }
}
