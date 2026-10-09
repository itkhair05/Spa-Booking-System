package com.example.spabooking.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class VNPayConfig {

    @Value("${vnpay.tmn-code:VMWY8Z1F}")
    private String tmnCode = "VMWY8Z1F";

    @Value("${vnpay.hash-secret:}")
    private String hashSecret = "";

    @Value("${vnpay.payment-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    @Value("${vnpay.return-url:http://localhost:5173/dat-lich/callback}")
    private String returnUrl = "http://localhost:5173/dat-lich/callback";

    @Value("${vnpay.ipn-url:}")
    private String ipnUrl = "";

    @Value("${vnpay.refund-url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String refundUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    @Value("${vnpay.querydr-url:${vnpay.refund-url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}}")
    private String queryDrUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    private static final java.util.Set<String> ALLOWED_SANDBOX_HOSTS = java.util.Set.of("sandbox.vnpayment.vn");

    public String getTmnCode() {
        return tmnCode;
    }

    public void setTmnCode(String tmnCode) {
        this.tmnCode = tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public void setHashSecret(String hashSecret) {
        this.hashSecret = hashSecret;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getIpnUrl() {
        return ipnUrl;
    }

    public void setIpnUrl(String ipnUrl) {
        this.ipnUrl = ipnUrl;
    }

    public String getRefundUrl() {
        return refundUrl;
    }

    public void setRefundUrl(String refundUrl) {
        this.refundUrl = refundUrl;
    }

    public String getQueryDrUrl() {
        return queryDrUrl;
    }

    public void setQueryDrUrl(String queryDrUrl) {
        this.queryDrUrl = queryDrUrl;
    }

    public void validateSandboxEndpoint(String endpointUrl, String endpointName) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            throw new IllegalStateException("VNPay " + endpointName + " must not be blank");
        }
        java.net.URI uri;
        try {
            uri = java.net.URI.create(endpointUrl.trim());
        } catch (Exception e) {
            throw new IllegalStateException("VNPay " + endpointName + " is an invalid URI: " + endpointUrl);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException("VNPay " + endpointName + " must use HTTPS scheme");
        }
        String host = uri.getHost();
        if (host == null || !ALLOWED_SANDBOX_HOSTS.contains(host.toLowerCase(java.util.Locale.ROOT))) {
            throw new IllegalStateException(
                "VNPay " + endpointName + " host '" + host + "' is not permitted in Sandbox phase. " +
                "Allowed hosts: " + ALLOWED_SANDBOX_HOSTS + ". Live payment endpoints (e.g. vnpayment.vn) are strictly forbidden."
            );
        }
    }

    public static String hmacSHA512(String key, String data) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("VNPay Hash Secret must be configured in environment or .env");
        }
        if (key.length() != key.trim().length() || key.contains(" ") || key.contains("\t") || key.contains("\n") || key.contains("\r")) {
            throw new IllegalArgumentException("VNPay Hash Secret contains invalid whitespace characters");
        }
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error calculating HMAC-SHA512", ex);
        }
    }
}
