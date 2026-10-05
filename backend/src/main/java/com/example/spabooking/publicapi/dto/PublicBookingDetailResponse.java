package com.example.spabooking.publicapi.dto;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.payment.entity.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PublicBookingDetailResponse {

    // Appointment
    private String bookingCode;
    private String status;
    private String serviceName;
    private String categoryName;
    private Integer durationMinutes;
    private BigDecimal price;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String staffName;

    // Customer
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // Payment
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal paidAmount;
    private LocalDateTime paidAt;

    // Spa
    private String spaName;
    private String spaAddress;
    private String spaPhone;
    private String spaEmail;

    // Service details
    private String serviceDescription;
    private String processSteps;

    public static PublicBookingDetailResponse fromEntity(Booking booking, Payment payment) {
        PublicBookingDetailResponse resp = new PublicBookingDetailResponse();
        resp.setBookingCode(booking.getBookingCode());
        resp.setStatus(booking.getStatus().name());

        if (booking.getService() != null) {
            resp.setServiceName(booking.getService().getName());
            resp.setDurationMinutes(booking.getService().getDurationMinutes());
            resp.setServiceDescription(booking.getService().getDescription());
            resp.setProcessSteps(booking.getService().getProcessSteps());
            if (booking.getService().getCategory() != null) {
                resp.setCategoryName(booking.getService().getCategory().getName());
            }
        }

        resp.setPrice(booking.getPrice());
        resp.setStartTime(booking.getStartTime());
        resp.setEndTime(booking.getEndTime());

        if (booking.getStaff() != null) {
            resp.setStaffName(booking.getStaff().getName());
        }

        if (booking.getCustomer() != null) {
            resp.setCustomerName(booking.getCustomer().getName());
            resp.setCustomerPhone(booking.getCustomer().getPhone());
            resp.setCustomerEmail(booking.getCustomer().getEmail());
        }

        if (payment != null) {
            resp.setPaymentMethod(payment.getPaymentMethod().name());
            resp.setPaymentStatus(payment.getStatus().name());
            resp.setPaidAmount(payment.getAmount());
            resp.setPaidAt(payment.getPaidAt());
        } else {
            resp.setPaymentMethod("PAY_AT_SPA");
            resp.setPaymentStatus("UNPAID");
            resp.setPaidAmount(booking.getPrice());
        }

        if (booking.getTenant() != null) {
            resp.setSpaName(booking.getTenant().getName());
            resp.setSpaAddress(booking.getTenant().getAddress());
            resp.setSpaPhone(booking.getTenant().getPhone());
            resp.setSpaEmail(booking.getTenant().getEmail());
        }

        return resp;
    }

    // Getters and Setters
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getSpaName() { return spaName; }
    public void setSpaName(String spaName) { this.spaName = spaName; }

    public String getSpaAddress() { return spaAddress; }
    public void setSpaAddress(String spaAddress) { this.spaAddress = spaAddress; }

    public String getSpaPhone() { return spaPhone; }
    public void setSpaPhone(String spaPhone) { this.spaPhone = spaPhone; }

    public String getSpaEmail() { return spaEmail; }
    public void setSpaEmail(String spaEmail) { this.spaEmail = spaEmail; }

    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }

    public String getProcessSteps() { return processSteps; }
    public void setProcessSteps(String processSteps) { this.processSteps = processSteps; }
}
