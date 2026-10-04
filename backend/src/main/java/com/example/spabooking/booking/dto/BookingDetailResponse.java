package com.example.spabooking.booking.dto;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;

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
    private Integer serviceDuration;
    private BigDecimal servicePrice;

    // Staff
    private Long staffId;
    private String staffName;
    private String staffPhone;
    private String staffEmail;

    public static BookingDetailResponse fromEntity(Booking booking, boolean isOwner) {
        BookingDetailResponse response = new BookingDetailResponse();
        response.setId(booking.getId());
        response.setBookingCode(booking.getBookingCode());
        response.setStatus(booking.getStatus());
        response.setStartTime(booking.getStartTime());
        response.setEndTime(booking.getEndTime());
        response.setDurationMinutes(booking.getService() != null ? booking.getService().getDurationMinutes() : null);
        response.setPrice(booking.getPrice());
        response.setIsReminded(booking.getIsReminded());
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
        }

        if (booking.getStaff() != null) {
            response.setStaffId(booking.getStaff().getId());
            response.setStaffName(booking.getStaff().getName());
            if (isOwner) {
                response.setStaffPhone(booking.getStaff().getPhone());
                response.setStaffEmail(booking.getStaff().getEmail());
            }
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

    public Integer getServiceDuration() { return serviceDuration; }
    public void setServiceDuration(Integer serviceDuration) { this.serviceDuration = serviceDuration; }

    public BigDecimal getServicePrice() { return servicePrice; }
    public void setServicePrice(BigDecimal servicePrice) { this.servicePrice = servicePrice; }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getStaffPhone() { return staffPhone; }
    public void setStaffPhone(String staffPhone) { this.staffPhone = staffPhone; }

    public String getStaffEmail() { return staffEmail; }
    public void setStaffEmail(String staffEmail) { this.staffEmail = staffEmail; }
}
