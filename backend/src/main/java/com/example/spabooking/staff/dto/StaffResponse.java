package com.example.spabooking.staff.dto;

import com.example.spabooking.staff.entity.Staff;

import java.time.LocalDateTime;

public class StaffResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StaffResponse fromEntity(Staff staff) {
        StaffResponse response = new StaffResponse();
        response.setId(staff.getId());
        response.setName(staff.getName());
        response.setPhone(staff.getPhone());
        response.setEmail(staff.getEmail());
        response.setIsActive(staff.getIsActive());
        response.setCreatedAt(staff.getCreatedAt());
        response.setUpdatedAt(staff.getUpdatedAt());
        return response;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
