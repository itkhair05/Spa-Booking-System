package com.example.spabooking.publicapi.dto;

import com.example.spabooking.booking.entity.Booking;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PublicBookingDetailResponse {

    private String bookingCode;
    private String status;
    private String serviceName;
    private Integer durationMinutes;
    private BigDecimal price;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String staffName;
    private String spaName;
    private String spaAddress;
    private String spaPhone;

    public static PublicBookingDetailResponse fromEntity(Booking booking) {
        PublicBookingDetailResponse resp = new PublicBookingDetailResponse();
        resp.setBookingCode(booking.getBookingCode());
        resp.setStatus(booking.getStatus().name());
        if (booking.getService() != null) {
            resp.setServiceName(booking.getService().getName());
            resp.setDurationMinutes(booking.getService().getDurationMinutes());
        }
        resp.setPrice(booking.getPrice());
        resp.setStartTime(booking.getStartTime());
        resp.setEndTime(booking.getEndTime());
        if (booking.getStaff() != null) {
            resp.setStaffName(booking.getStaff().getName());
        }
        if (booking.getTenant() != null) {
            resp.setSpaName(booking.getTenant().getName());
            resp.setSpaAddress(booking.getTenant().getAddress());
            resp.setSpaPhone(booking.getTenant().getPhone());
        }
        return resp;
    }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

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

    public String getSpaName() { return spaName; }
    public void setSpaName(String spaName) { this.spaName = spaName; }

    public String getSpaAddress() { return spaAddress; }
    public void setSpaAddress(String spaAddress) { this.spaAddress = spaAddress; }

    public String getSpaPhone() { return spaPhone; }
    public void setSpaPhone(String spaPhone) { this.spaPhone = spaPhone; }
}
