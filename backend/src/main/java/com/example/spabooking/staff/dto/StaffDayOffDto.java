package com.example.spabooking.staff.dto;

import com.example.spabooking.staff.entity.StaffDayOff;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class StaffDayOffDto {

    private Long id;
    private LocalDate date;
    private String reason;
    private LocalDateTime createdAt;

    public StaffDayOffDto() {}

    public StaffDayOffDto(Long id, LocalDate date, String reason, LocalDateTime createdAt) {
        this.id = id;
        this.date = date;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public static StaffDayOffDto fromEntity(StaffDayOff entity) {
        if (entity == null) return null;
        return new StaffDayOffDto(
                entity.getId(),
                entity.getDate(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
