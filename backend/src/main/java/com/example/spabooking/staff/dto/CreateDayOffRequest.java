package com.example.spabooking.staff.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateDayOffRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String reason;

    public CreateDayOffRequest() {}

    public CreateDayOffRequest(LocalDate date, String reason) {
        this.date = date;
        this.reason = reason;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
