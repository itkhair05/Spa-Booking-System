package com.example.spabooking.payment.dto;

import com.example.spabooking.payment.entity.Refund;
import com.example.spabooking.payment.enums.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundResponse {

    private Long id;
    private Long bookingId;
    private String bookingCode;
    private Long paymentId;
    private String refundRequestId;
    private String provider;
    private BigDecimal originalAmount;
    private BigDecimal refundAmount;
    private BigDecimal cancellationFee;
    private Integer policyPercentage;
    private String reason;
    private RefundStatus status;
    private String providerResponseCode;
    private String providerResponseMessage;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    public static RefundResponse fromEntity(Refund refund) {
        RefundResponse resp = new RefundResponse();
        resp.setId(refund.getId());
        if (refund.getBooking() != null) {
            resp.setBookingId(refund.getBooking().getId());
            resp.setBookingCode(refund.getBooking().getBookingCode());
        }
        if (refund.getPayment() != null) {
            resp.setPaymentId(refund.getPayment().getId());
        }
        resp.setRefundRequestId(refund.getRefundRequestId());
        resp.setProvider(refund.getProvider() != null ? refund.getProvider().name() : null);
        resp.setOriginalAmount(refund.getOriginalAmount());
        resp.setRefundAmount(refund.getRefundAmount());
        resp.setCancellationFee(refund.getCancellationFee());
        resp.setPolicyPercentage(refund.getPolicyPercentage());
        resp.setReason(refund.getReason());
        resp.setStatus(refund.getStatus());
        resp.setProviderResponseCode(refund.getProviderResponseCode());
        resp.setProviderResponseMessage(refund.getProviderResponseMessage());
        resp.setRequestedAt(refund.getRequestedAt());
        resp.setProcessedAt(refund.getProcessedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getRefundRequestId() { return refundRequestId; }
    public void setRefundRequestId(String refundRequestId) { this.refundRequestId = refundRequestId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public BigDecimal getCancellationFee() { return cancellationFee; }
    public void setCancellationFee(BigDecimal cancellationFee) { this.cancellationFee = cancellationFee; }

    public Integer getPolicyPercentage() { return policyPercentage; }
    public void setPolicyPercentage(Integer policyPercentage) { this.policyPercentage = policyPercentage; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public RefundStatus getStatus() { return status; }
    public void setStatus(RefundStatus status) { this.status = status; }

    public String getProviderResponseCode() { return providerResponseCode; }
    public void setProviderResponseCode(String providerResponseCode) { this.providerResponseCode = providerResponseCode; }

    public String getProviderResponseMessage() { return providerResponseMessage; }
    public void setProviderResponseMessage(String providerResponseMessage) { this.providerResponseMessage = providerResponseMessage; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
