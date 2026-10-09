package com.example.spabooking.payment.service;

import com.example.spabooking.payment.config.VNPayConfig;
import com.example.spabooking.payment.entity.Payment;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    private static final DateTimeFormatter VNP_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public VNPayService(VNPayConfig vnPayConfig) {
        this.vnPayConfig = vnPayConfig;
    }

    public String createPaymentUrl(Payment payment, String ipAddress, String returnUrlOverride, String orderInfo) {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());

        // VNPay amount is in VND x 100
        long amountVal = payment.getAmount().multiply(new java.math.BigDecimal(100)).longValue();
        vnpParams.put("vnp_Amount", String.valueOf(amountVal));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", payment.getTxnRef());

        String info = (orderInfo != null && !orderInfo.isBlank())
                ? orderInfo
                : "Thanh toan lich hen TIKEY SPA - " + payment.getBooking().getBookingCode();
        vnpParams.put("vnp_OrderInfo", info);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");

        String returnUrl = (returnUrlOverride != null && !returnUrlOverride.isBlank())
                ? returnUrlOverride
                : vnPayConfig.getReturnUrl();
        vnpParams.put("vnp_ReturnUrl", returnUrl);

        String ip = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
        vnpParams.put("vnp_IpAddr", ip);

        Instant now = Instant.now();
        vnpParams.put("vnp_CreateDate", VNP_DATE_FORMAT.format(now));
        vnpParams.put("vnp_ExpireDate", VNP_DATE_FORMAT.format(now.plus(15, ChronoUnit.MINUTES)));

        vnPayConfig.validateSandboxEndpoint(vnPayConfig.getPaymentUrl(), "payment-url");

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        try {
            for (String fieldName : fieldNames) {
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                        query.append('&');
                    }
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));

                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()))
                            .append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                }
            }

            String secureHash = VNPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
            query.append("&vnp_SecureHash=").append(secureHash);

            return vnPayConfig.getPaymentUrl() + "?" + query.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to construct VNPay URL", ex);
        }
    }

    public boolean verifySignature(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }

        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey() != null && entry.getKey().startsWith("vnp_")
                    && !entry.getKey().equals("vnp_SecureHash")
                    && !entry.getKey().equals("vnp_SecureHashType")
                    && entry.getValue() != null && !entry.getValue().isEmpty()) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        try {
            for (String fieldName : fieldNames) {
                String fieldValue = fields.get(fieldName);
                if (hashData.length() > 0) {
                    hashData.append('&');
                }
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
            }

            String calculatedHash = VNPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
            if (java.security.MessageDigest.isEqual(
                    calculatedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                    secureHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))) {
                return true;
            }

            // Fallback for space encoding: handle clients/proxies that preserve %20 instead of +
            if (hashData.indexOf("+") >= 0) {
                String hashDataPercent20 = hashData.toString().replace("+", "%20");
                String calculatedHashPercent20 = VNPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashDataPercent20);
                if (java.security.MessageDigest.isEqual(
                        calculatedHashPercent20.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                        secureHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))) {
                    return true;
                }
            }

            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}
