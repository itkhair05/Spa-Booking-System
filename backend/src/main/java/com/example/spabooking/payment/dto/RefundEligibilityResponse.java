package com.example.spabooking.payment.dto;

import com.example.spabooking.payment.enums.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundEligibilityResponse {

    private Long bookingId;
    private String bookingCode;
    private String bookingStatus;
    private String paymentStatus;
    private String paymentMethod;
    private BigDecimal originalPaidAmount;
    private BigDecimal refundAmount;
    private BigDecimal cancellationFee;
    private Integer policyPercentage;
    private String policyDescription;
    private LocalDateTime deadline;
    private boolean refundEligible;
    private String reason;
    private RefundStatus existingRefundStatus;
    private String refundRequestId;

    public RefundEligibilityResponse() {}

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getOriginalPaidAmount() { return originalPaidAmount; }
    public void setOriginalPaidAmount(BigDecimal originalPaidAmount) { this.originalPaidAmount = originalPaidAmount; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public BigDecimal getCancellationFee() { return cancellationFee; }
    public void setCancellationFee(BigDecimal cancellationFee) { this.cancellationFee = cancellationFee; }

    public Integer getPolicyPercentage() { return policyPercentage; }
    public void setPolicyPercentage(Integer policyPercentage) { this.policyPercentage = policyPercentage; }

    public String getPolicyDescription() { return policyDescription; }
    public void setPolicyDescription(String policyDescription) { this.policyDescription = policyDescription; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public boolean isRefundEligible() { return refundEligible; }
    public void setRefundEligible(boolean refundEligible) { this.refundEligible = refundEligible; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public RefundStatus getExistingRefundStatus() { return existingRefundStatus; }
    public void setExistingRefundStatus(RefundStatus existingRefundStatus) { this.existingRefundStatus = existingRefundStatus; }

    public String getRefundRequestId() { return refundRequestId; }
    public void setRefundRequestId(String refundRequestId) { this.refundRequestId = refundRequestId; }
}
