package com.example.spabooking.staff.dto;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DailyScheduleItemResponse {

    private Long id;
    private String bookingCode;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long serviceId;
    private String serviceName;
    private Integer serviceDuration;
    private Long staffId;
    private String staffName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private BigDecimal price;

    public DailyScheduleItemResponse() {}

    public static DailyScheduleItemResponse fromEntity(Booking booking) {
        if (booking == null) return null;
        DailyScheduleItemResponse res = new DailyScheduleItemResponse();
        res.setId(booking.getId());
        res.setBookingCode(booking.getBookingCode());
        if (booking.getCustomer() != null) {
            res.setCustomerId(booking.getCustomer().getId());
            res.setCustomerName(booking.getCustomer().getName());
            res.setCustomerPhone(booking.getCustomer().getPhone());
        }
        if (booking.getService() != null) {
            res.setServiceId(booking.getService().getId());
            res.setServiceName(booking.getService().getName());
            res.setServiceDuration(booking.getService().getDurationMinutes());
        }
        if (booking.getStaff() != null) {
            res.setStaffId(booking.getStaff().getId());
            res.setStaffName(booking.getStaff().getName());
        }
        res.setStartTime(booking.getStartTime());
        res.setEndTime(booking.getEndTime());
        res.setStatus(booking.getStatus());
        res.setPrice(booking.getPrice());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public Integer getServiceDuration() { return serviceDuration; }
    public void setServiceDuration(Integer serviceDuration) { this.serviceDuration = serviceDuration; }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
