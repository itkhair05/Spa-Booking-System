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
}
