package com.example.spabooking.payment.service;

import com.example.spabooking.payment.config.VNPayConfig;
import com.example.spabooking.payment.dto.ProviderQueryResult;
import com.example.spabooking.payment.dto.ProviderRefundResult;
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.enums.RefundStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class VNPayPaymentProviderService implements PaymentProviderService {

    private static final Logger log = LoggerFactory.getLogger(VNPayPaymentProviderService.class);

    private static final DateTimeFormatter VNP_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final VNPayConfig vnPayConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public VNPayPaymentProviderService(VNPayConfig vnPayConfig) {
        this(vnPayConfig, new ObjectMapper(), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public VNPayPaymentProviderService(VNPayConfig vnPayConfig, ObjectMapper objectMapper, HttpClient httpClient) {
        this.vnPayConfig = vnPayConfig;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = httpClient;
    }

    @Override
    public ProviderRefundResult refund(Payment payment, BigDecimal refundAmount, String refundRequestId, String reason, String ipAddress) {
        if (payment == null) {
            return new ProviderRefundResult(RefundStatus.REFUND_FAILED, "ERR_INVALID_PAYMENT", "Thông tin thanh toán không hợp lệ", null);
        }
        if (payment.getTxnRef() == null || payment.getTxnRef().isBlank()) {
            return new ProviderRefundResult(RefundStatus.REFUND_FAILED, "ERR_INVALID_TXN_REF", "Thiếu mã tham chiếu giao dịch gốc", null);
        }
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(payment.getAmount()) > 0) {
            return new ProviderRefundResult(RefundStatus.REFUND_FAILED, "ERR_INVALID_AMOUNT", "Số tiền hoàn không hợp lệ", null);
        }

        String hashSecret = vnPayConfig.getHashSecret();
        if (hashSecret == null || hashSecret.isBlank()) {
            log.warn("VNPay hash secret is not configured; cannot process online refund");
            return new ProviderRefundResult(RefundStatus.REFUND_FAILED, "ERR_NO_SECRET", "Cổng VNPay chưa được cấu hình Secret Key", null);
        }

        try {
            // Determine transaction type: 02 = full refund, 03 = partial refund
            boolean isFull = refundAmount.compareTo(payment.getAmount()) == 0;
            String transactionType = isFull ? "02" : "03";

            long amountVal = refundAmount.multiply(new BigDecimal(100)).longValue();
            String amountStr = String.valueOf(amountVal);

            String createDate = VNP_DATE_FORMAT.format(Instant.now());
            String txnDate = payment.getPaidAt() != null
                    ? VNP_DATE_FORMAT.format(payment.getPaidAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : (payment.getCreatedAt() != null ? VNP_DATE_FORMAT.format(payment.getCreatedAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh"))) : createDate);

            String transactionNo = (payment.getTransactionNo() != null && !payment.getTransactionNo().isBlank())
                    ? payment.getTransactionNo()
                    : "0";

            String createBy = "OWNER";
            String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
            String orderInfo = (reason != null && !reason.isBlank())
                    ? reason
                    : "Hoan tien TIKEY SPA - " + (payment.getBooking() != null ? payment.getBooking().getBookingCode() : payment.getTxnRef());

            // Build hash string per VNPay API 2.1.0 specification
            String hashData = refundRequestId + "|"
                    + "2.1.0|"
                    + "refund|"
                    + vnPayConfig.getTmnCode() + "|"
                    + transactionType + "|"
                    + payment.getTxnRef() + "|"
                    + amountStr + "|"
                    + transactionNo + "|"
                    + txnDate + "|"
                    + createBy + "|"
                    + createDate + "|"
                    + clientIp + "|"
                    + orderInfo;

            String secureHash = VNPayConfig.hmacSHA512(hashSecret, hashData);

            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("vnp_RequestId", refundRequestId);
            requestPayload.put("vnp_Version", "2.1.0");
            requestPayload.put("vnp_Command", "refund");
            requestPayload.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            requestPayload.put("vnp_TransactionType", transactionType);
            requestPayload.put("vnp_TxnRef", payment.getTxnRef());
            requestPayload.put("vnp_Amount", amountVal);
            requestPayload.put("vnp_OrderInfo", orderInfo);
            requestPayload.put("vnp_TransactionNo", transactionNo);
            requestPayload.put("vnp_TransactionDate", txnDate);
            requestPayload.put("vnp_CreateBy", createBy);
            requestPayload.put("vnp_CreateDate", createDate);
            requestPayload.put("vnp_IpAddr", clientIp);
            requestPayload.put("vnp_SecureHash", secureHash);

            String jsonBody = objectMapper.writeValueAsString(requestPayload);

            vnPayConfig.validateSandboxEndpoint(vnPayConfig.getRefundUrl(), "refund-url");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(vnPayConfig.getRefundUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.info("Sending VNPay refund request: requestId={}, txnRef={}, amount={}", refundRequestId, payment.getTxnRef(), refundAmount);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("VNPay refund endpoint returned HTTP status {}", response.statusCode());
                return new ProviderRefundResult(RefundStatus.REFUND_FAILED, "HTTP_" + response.statusCode(), "Lỗi kết nối máy chủ VNPay (HTTP " + response.statusCode() + ")", null);
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.isBlank()) {
                return new ProviderRefundResult(RefundStatus.REFUND_FAILED, "ERR_EMPTY_RESPONSE", "Phản hồi rỗng từ cổng VNPay", null);
            }

            Map<String, String> respMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, String>>() {});
            String responseCode = respMap.get("vnp_ResponseCode");
            String responseMessage = respMap.get("vnp_Message");
            String responseSecureHash = respMap.get("vnp_SecureHash");
            String providerTxnNo = respMap.get("vnp_TransactionNo");

            // Verify response hash if present
            if (responseSecureHash != null && !responseSecureHash.isBlank()) {
                String responseHashData = respMap.getOrDefault("vnp_ResponseId", "") + "|"
                        + respMap.getOrDefault("vnp_Command", "") + "|"
                        + respMap.getOrDefault("vnp_ResponseCode", "") + "|"
                        + respMap.getOrDefault("vnp_Message", "") + "|"
                        + respMap.getOrDefault("vnp_TmnCode", "") + "|"
                        + respMap.getOrDefault("vnp_TxnRef", "") + "|"
                        + respMap.getOrDefault("vnp_Amount", "") + "|"
                        + respMap.getOrDefault("vnp_BankCode", "") + "|"
                        + respMap.getOrDefault("vnp_PayDate", "") + "|"
                        + respMap.getOrDefault("vnp_TransactionNo", "") + "|"
                        + respMap.getOrDefault("vnp_TransactionType", "") + "|"
                        + respMap.getOrDefault("vnp_TransactionStatus", "") + "|"
                        + respMap.getOrDefault("vnp_OrderInfo", "");

                String calculatedHash = VNPayConfig.hmacSHA512(hashSecret, responseHashData);
                if (!MessageDigest.isEqual(
                        calculatedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                        responseSecureHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))) {
                    log.warn("Invalid VNPay refund response signature");
                    return new ProviderRefundResult(RefundStatus.REFUND_FAILED, "97", "Chữ ký phản hồi hoàn tiền không hợp lệ", providerTxnNo);
                }
            }

            String transactionStatus = respMap.get("vnp_TransactionStatus");
            return mapResponse(responseCode, transactionStatus, responseMessage, providerTxnNo);

        } catch (Exception ex) {
            log.error("Exception during VNPay refund request: {}", ex.getMessage());
            return new ProviderRefundResult(RefundStatus.REFUND_FAILED, "ERR_EXCEPTION", "Không thể kết nối dịch vụ hoàn tiền VNPay: " + ex.getMessage(), null);
        }
    }

    @Override
    public ProviderQueryResult queryTransaction(Payment payment, String ipAddress) {
        if (payment == null) {
            return ProviderQueryResult.error("ERR_INVALID_PAYMENT", "Thông tin thanh toán không hợp lệ");
        }
        if (payment.getTxnRef() == null || payment.getTxnRef().isBlank()) {
            return ProviderQueryResult.error("ERR_INVALID_TXN_REF", "Thiếu mã tham chiếu giao dịch gốc");
        }

        String hashSecret = vnPayConfig.getHashSecret();
        if (hashSecret == null || hashSecret.isBlank()) {
            log.warn("VNPay hash secret is not configured; cannot query transaction");
            return ProviderQueryResult.error("ERR_NO_SECRET", "Cổng VNPay chưa được cấu hình Secret Key");
        }

        try {
            String requestId = System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String createDate = VNP_DATE_FORMAT.format(Instant.now());
            String txnDate = payment.getPaidAt() != null
                    ? VNP_DATE_FORMAT.format(payment.getPaidAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : (payment.getCreatedAt() != null ? VNP_DATE_FORMAT.format(payment.getCreatedAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh"))) : createDate);

            String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
            String orderInfo = "Truy van giao dich TIKEY SPA - " + payment.getTxnRef();

            // Hash string for QueryDR per VNPay 2.1.0 specification:
            // format: vnp_RequestId|vnp_Version|vnp_Command|vnp_TmnCode|vnp_TxnRef|vnp_TransactionDate|vnp_CreateDate|vnp_IpAddr|vnp_OrderInfo
            String hashData = requestId + "|"
                    + "2.1.0|"
                    + "querydr|"
                    + vnPayConfig.getTmnCode() + "|"
                    + payment.getTxnRef() + "|"
                    + txnDate + "|"
                    + createDate + "|"
                    + clientIp + "|"
                    + orderInfo;

            String secureHash = VNPayConfig.hmacSHA512(hashSecret, hashData);

            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("vnp_RequestId", requestId);
            requestPayload.put("vnp_Version", "2.1.0");
            requestPayload.put("vnp_Command", "querydr");
            requestPayload.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            requestPayload.put("vnp_TxnRef", payment.getTxnRef());
            requestPayload.put("vnp_OrderInfo", orderInfo);
            requestPayload.put("vnp_TransactionDate", txnDate);
            requestPayload.put("vnp_CreateDate", createDate);
            requestPayload.put("vnp_IpAddr", clientIp);
            requestPayload.put("vnp_SecureHash", secureHash);

            String jsonBody = objectMapper.writeValueAsString(requestPayload);

            vnPayConfig.validateSandboxEndpoint(vnPayConfig.getQueryDrUrl(), "querydr-url");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(vnPayConfig.getQueryDrUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.info("Sending VNPay QueryDR request: requestId={}, txnRef={}", requestId, payment.getTxnRef());

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("VNPay QueryDR endpoint returned HTTP status {}", response.statusCode());
                return ProviderQueryResult.error(
                        "HTTP_" + response.statusCode(), "Lỗi kết nối máy chủ VNPay (HTTP " + response.statusCode() + ")");
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.isBlank()) {
                return ProviderQueryResult.error("ERR_EMPTY_RESPONSE", "Phản hồi rỗng từ cổng VNPay");
            }

            Map<String, String> respMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, String>>() {});
            String responseCode = respMap.get("vnp_ResponseCode");
            String responseMessage = respMap.get("vnp_Message");
            String responseSecureHash = respMap.get("vnp_SecureHash");

            // Verify response hash: do not accept unsigned response as trusted provider data!
            if (responseSecureHash == null || responseSecureHash.isBlank()) {
                if ("00".equals(responseCode)) {
                    log.warn("VNPay QueryDR response with responseCode=00 is missing required secure hash; rejected as untrusted");
                    return ProviderQueryResult.error("97", "Phản hồi VNPay thiếu chữ ký bảo mật");
                }
                return ProviderQueryResult.error(
                        responseCode != null ? responseCode : "ERR_NO_CODE",
                        responseMessage != null ? responseMessage : "VNPay QueryDR request failed");
            }

            String responseHashData = respMap.getOrDefault("vnp_ResponseId", "") + "|"
                    + respMap.getOrDefault("vnp_Command", "") + "|"
                    + respMap.getOrDefault("vnp_ResponseCode", "") + "|"
                    + respMap.getOrDefault("vnp_Message", "") + "|"
                    + respMap.getOrDefault("vnp_TmnCode", "") + "|"
                    + respMap.getOrDefault("vnp_TxnRef", "") + "|"
                    + respMap.getOrDefault("vnp_Amount", "") + "|"
                    + respMap.getOrDefault("vnp_BankCode", "") + "|"
                    + respMap.getOrDefault("vnp_PayDate", "") + "|"
                    + respMap.getOrDefault("vnp_TransactionNo", "") + "|"
                    + respMap.getOrDefault("vnp_TransactionType", "") + "|"
                    + respMap.getOrDefault("vnp_TransactionStatus", "") + "|"
                    + respMap.getOrDefault("vnp_OrderInfo", "");

            String calculatedHash = VNPayConfig.hmacSHA512(hashSecret, responseHashData);
            if (!MessageDigest.isEqual(
                    calculatedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                    responseSecureHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))) {
                log.warn("Invalid VNPay QueryDR response signature");
                return ProviderQueryResult.error("97", "Chữ ký phản hồi truy vấn giao dịch không hợp lệ");
            }

            if ("00".equals(responseCode)) {
                String transactionStatus = respMap.get("vnp_TransactionStatus");
                String transactionNo = respMap.get("vnp_TransactionNo");
                String bankCode = respMap.get("vnp_BankCode");
                String payDate = respMap.get("vnp_PayDate");
                return ProviderQueryResult.success(
                        responseCode, transactionStatus, transactionNo, bankCode, payDate, responseMessage);
            } else {
                return ProviderQueryResult.error(
                        responseCode, responseMessage != null ? responseMessage : "Mã lỗi VNPay QueryDR: " + responseCode);
            }

        } catch (Exception ex) {
            log.error("Exception during VNPay QueryDR request: {}", ex.getMessage());
            return ProviderQueryResult.error("ERR_EXCEPTION", "Không thể kết nối dịch vụ truy vấn VNPay: " + ex.getMessage());
        }
    }

    public ProviderRefundResult mapResponse(String responseCode, String transactionStatus, String message, String providerTxnRef) {
        if (responseCode == null) {
            return new ProviderRefundResult(RefundStatus.REFUND_FAILED, "ERR_NO_CODE", "Không có mã phản hồi từ cổng VNPay", providerTxnRef);
        }

        switch (responseCode) {
            case "02":
                return new ProviderRefundResult(RefundStatus.REFUND_FAILED, responseCode, "Mã định danh kết nối (TmnCode) không hợp lệ", providerTxnRef);
            case "03":
                return new ProviderRefundResult(RefundStatus.REFUND_FAILED, responseCode, "Dữ liệu gửi sang VNPay không đúng định dạng", providerTxnRef);
            case "08":
                return new ProviderRefundResult(RefundStatus.REFUND_FAILED, responseCode, "Hệ thống VNPay đang bảo trì", providerTxnRef);
            case "16":
                return new ProviderRefundResult(RefundStatus.REFUND_FAILED, responseCode, "Chưa đến thời gian được phép hoàn tiền", providerTxnRef);
            case "91":
                return new ProviderRefundResult(RefundStatus.REFUND_FAILED, responseCode, "Không tìm thấy giao dịch yêu cầu hoàn tiền", providerTxnRef);
            case "93":
                return new ProviderRefundResult(RefundStatus.REFUND_FAILED, responseCode, "Số tiền yêu cầu hoàn không hợp lệ", providerTxnRef);
            case "95":
                return new ProviderRefundResult(RefundStatus.REFUND_FAILED, responseCode, "Giao dịch gốc không thành công, không thể hoàn", providerTxnRef);
            case "97":
                return new ProviderRefundResult(RefundStatus.REFUND_FAILED, responseCode, "Chữ ký kiểm tra không hợp lệ", providerTxnRef);
            case "94":
                return new ProviderRefundResult(RefundStatus.REFUND_PENDING, responseCode, "Yêu cầu hoàn tiền đang được VNPay xử lý", providerTxnRef);
            case "00":
                // VNPay refund response 00 means refund request was accepted and queued.
                // It must NOT be treated as REFUNDED unless transactionStatus is authoritatively confirmed completed ("00").
                if ("00".equals(transactionStatus)) {
                    return new ProviderRefundResult(RefundStatus.REFUNDED, responseCode, "Hoàn tiền thành công qua VNPay", providerTxnRef);
                } else if ("09".equals(transactionStatus)) {
                    return new ProviderRefundResult(RefundStatus.REFUND_FAILED, responseCode, "Giao dịch hoàn tiền bị từ chối bởi VNPay / Ngân hàng", providerTxnRef);
                } else {
                    // transactionStatus is "05" (Chờ duyệt), "06" (Chuyển ngân hàng), null, or pending
                    return new ProviderRefundResult(RefundStatus.REFUND_PENDING, responseCode, "Yêu cầu hoàn tiền đã được VNPay tiếp nhận (Chờ duyệt / Đang xử lý)", providerTxnRef);
                }
            default:
                String desc = message != null && !message.isBlank() ? message : "Lỗi hoàn tiền VNPay (Mã " + responseCode + ")";
                return new ProviderRefundResult(RefundStatus.REFUND_FAILED, responseCode, desc, providerTxnRef);
        }
    }
}
