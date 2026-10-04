package com.example.spabooking.staff.dto;

import com.example.spabooking.auth.entity.User;

public class StaffAccountResponse {

    private Long userId;
    private Long staffId;
    private String username;
    private String role;
    private Boolean isActive;

    public static StaffAccountResponse fromEntity(User user) {
        StaffAccountResponse response = new StaffAccountResponse();
        response.setUserId(user.getId());
        response.setStaffId(user.getStaff() != null ? user.getStaff().getId() : null);
        response.setUsername(user.getUsername());
        response.setRole(user.getRole().name());
        response.setIsActive(user.getIsActive());
        return response;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
