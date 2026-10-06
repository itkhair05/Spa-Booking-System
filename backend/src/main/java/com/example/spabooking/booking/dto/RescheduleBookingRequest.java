package com.example.spabooking.booking.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class RescheduleBookingRequest {

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    private Long staffId;

    public RescheduleBookingRequest() {}

    public RescheduleBookingRequest(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public RescheduleBookingRequest(LocalDateTime startTime, Long staffId) {
        this.startTime = startTime;
        this.staffId = staffId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }
}
