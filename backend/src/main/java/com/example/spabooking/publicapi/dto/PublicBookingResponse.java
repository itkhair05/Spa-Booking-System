package com.example.spabooking.publicapi.dto;

import com.example.spabooking.booking.entity.Booking;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class PublicBookingResponse {
    private Long id;
    private String bookingCode;
    private Long serviceId;
    private String serviceName;
    private Long staffId;
    private String staffName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private BigDecimal price;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentUrl;

    public static PublicBookingResponse fromEntity(Booking booking) {
        PublicBookingResponse resp = new PublicBookingResponse();
        resp.setId(booking.getId());
        resp.setBookingCode(booking.getBookingCode());
        resp.setServiceId(booking.getService().getId());
        resp.setServiceName(booking.getService().getName());
        resp.setStaffId(booking.getStaff().getId());
        resp.setStaffName(booking.getStaff().getName());
        resp.setStartTime(booking.getStartTime());
        resp.setEndTime(booking.getEndTime());
        resp.setStatus(booking.getStatus().name());
        resp.setPrice(booking.getPrice());
        return resp;
    }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
