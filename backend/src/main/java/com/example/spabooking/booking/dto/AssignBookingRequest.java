package com.example.spabooking.booking.dto;

import jakarta.validation.constraints.NotNull;

public class AssignBookingRequest {

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    public AssignBookingRequest() {}

    public AssignBookingRequest(Long staffId) {
        this.staffId = staffId;
    }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
}
