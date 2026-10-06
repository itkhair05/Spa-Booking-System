package com.example.spabooking.booking.dto;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingDetailResponse {

    private Long id;
    private String bookingCode;
    private BookingStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private BigDecimal price;
    private Boolean isReminded;
    private LocalDateTime confirmedAt;
    private LocalDateTime checkedInAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime noShowAt;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Customer
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // Service
    private Long serviceId;
    private String serviceName;
    private String categoryName;
    private Integer serviceDuration;
    private BigDecimal servicePrice;
    private String serviceDescription;
    private String processSteps;

    // Staff
    private Long staffId;
    private String staffName;
    private String staffPhone;
    private String staffEmail;

    // Payment
    private String paymentMethod;
    private String paymentProvider;
    private String paymentStatus;
    private BigDecimal paidAmount;
    private LocalDateTime paidAt;
    private String txnRef;
    private String transactionNo;
    private String bankCode;
    private String cardType;

    // Refund (Phase B.8.1)
    private BigDecimal refundAmount;
    private BigDecimal cancellationFee;
    private String refundStatus;
    private Integer refundPolicyPercentage;
    private String refundReason;
    private String refundRequestId;
    private LocalDateTime refundProcessedAt;

    public static BookingDetailResponse fromEntity(Booking booking, boolean isOwner) {
        return fromEntity(booking, null, null, isOwner);
    }

    public static BookingDetailResponse fromEntity(Booking booking, Payment payment, boolean isOwner) {
        return fromEntity(booking, payment, null, isOwner);
    }

    public static BookingDetailResponse fromEntity(Booking booking, Payment payment, com.example.spabooking.payment.entity.Refund refund, boolean isOwner) {
        BookingDetailResponse response = new BookingDetailResponse();
        response.setId(booking.getId());
        response.setBookingCode(booking.getBookingCode());
        response.setStatus(booking.getStatus());
        response.setStartTime(booking.getStartTime());
        response.setEndTime(booking.getEndTime());
        response.setDurationMinutes(booking.getService() != null ? booking.getService().getDurationMinutes() : null);
        response.setPrice(booking.getPrice());
        response.setIsReminded(booking.getIsReminded());
        response.setConfirmedAt(booking.getConfirmedAt());
        response.setCheckedInAt(booking.getCheckedInAt());
        response.setStartedAt(booking.getStartedAt());
        response.setCompletedAt(booking.getCompletedAt());
        response.setCancelledAt(booking.getCancelledAt());
        response.setNoShowAt(booking.getNoShowAt());
        response.setCancellationReason(booking.getCancellationReason());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());

        if (booking.getCustomer() != null) {
            response.setCustomerId(booking.getCustomer().getId());
            response.setCustomerName(booking.getCustomer().getName());
            response.setCustomerPhone(booking.getCustomer().getPhone());
            if (isOwner) {
                response.setCustomerEmail(booking.getCustomer().getEmail());
            }
        }

        if (booking.getService() != null) {
            response.setServiceId(booking.getService().getId());
            response.setServiceName(booking.getService().getName());
            response.setServiceDuration(booking.getService().getDurationMinutes());
            response.setServicePrice(booking.getService().getPrice());
            response.setServiceDescription(booking.getService().getDescription());
            response.setProcessSteps(booking.getService().getProcessSteps());
            if (booking.getService().getCategory() != null) {
                response.setCategoryName(booking.getService().getCategory().getName());
            }
        }

        if (booking.getStaff() != null) {
            response.setStaffId(booking.getStaff().getId());
            response.setStaffName(booking.getStaff().getName());
            if (isOwner) {
                response.setStaffPhone(booking.getStaff().getPhone());
                response.setStaffEmail(booking.getStaff().getEmail());
            }
        }

        if (payment != null) {
            response.setPaymentMethod(payment.getPaymentMethod().name());
            response.setPaymentProvider(payment.getProvider().name());
            response.setPaymentStatus(payment.getStatus().name());
            response.setPaidAmount(payment.getAmount());
            response.setPaidAt(payment.getPaidAt());
            if (isOwner) {
                response.setTxnRef(payment.getTxnRef());
                response.setTransactionNo(payment.getTransactionNo());
                response.setBankCode(payment.getBankCode());
                response.setCardType(payment.getCardType());
            }
        } else {
            response.setPaymentMethod("PAY_AT_SPA");
            response.setPaymentProvider("SPA");
            response.setPaymentStatus("UNPAID");
            response.setPaidAmount(booking.getPrice());
        }

        if (refund != null) {
            response.setRefundAmount(refund.getRefundAmount());
            response.setCancellationFee(refund.getCancellationFee());
            response.setRefundStatus(refund.getStatus().name());
            response.setRefundPolicyPercentage(refund.getPolicyPercentage());
            response.setRefundReason(refund.getReason());
            response.setRefundRequestId(refund.getRefundRequestId());
            response.setRefundProcessedAt(refund.getProcessedAt());
        }

        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Boolean getIsReminded() { return isReminded; }
    public void setIsReminded(Boolean isReminded) { this.isReminded = isReminded; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public LocalDateTime getNoShowAt() { return noShowAt; }
    public void setNoShowAt(LocalDateTime noShowAt) { this.noShowAt = noShowAt; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Integer getServiceDuration() { return serviceDuration; }
    public void setServiceDuration(Integer serviceDuration) { this.serviceDuration = serviceDuration; }

    public BigDecimal getServicePrice() { return servicePrice; }
    public void setServicePrice(BigDecimal servicePrice) { this.servicePrice = servicePrice; }

    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }

    public String getProcessSteps() { return processSteps; }
    public void setProcessSteps(String processSteps) { this.processSteps = processSteps; }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getStaffPhone() { return staffPhone; }
    public void setStaffPhone(String staffPhone) { this.staffPhone = staffPhone; }

    public String getStaffEmail() { return staffEmail; }
    public void setStaffEmail(String staffEmail) { this.staffEmail = staffEmail; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentProvider() { return paymentProvider; }
    public void setPaymentProvider(String paymentProvider) { this.paymentProvider = paymentProvider; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getTxnRef() { return txnRef; }
    public void setTxnRef(String txnRef) { this.txnRef = txnRef; }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public BigDecimal getCancellationFee() { return cancellationFee; }
    public void setCancellationFee(BigDecimal cancellationFee) { this.cancellationFee = cancellationFee; }

    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }

    public Integer getRefundPolicyPercentage() { return refundPolicyPercentage; }
    public void setRefundPolicyPercentage(Integer refundPolicyPercentage) { this.refundPolicyPercentage = refundPolicyPercentage; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public String getRefundRequestId() { return refundRequestId; }
    public void setRefundRequestId(String refundRequestId) { this.refundRequestId = refundRequestId; }

    public LocalDateTime getRefundProcessedAt() { return refundProcessedAt; }
    public void setRefundProcessedAt(LocalDateTime refundProcessedAt) { this.refundProcessedAt = refundProcessedAt; }
}
