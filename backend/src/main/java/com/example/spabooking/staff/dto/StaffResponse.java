package com.example.spabooking.staff.dto;

import com.example.spabooking.staff.entity.Staff;

import java.time.LocalDateTime;

public class StaffResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private String username;
    private Boolean accountEnabled;
    private Boolean isActive;
    private Boolean isDeleted;
    private Boolean showOnWebsite;
    private String avatarUrl;
    private String accessToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StaffResponse fromEntity(Staff staff) {
        return fromEntity(staff, null, null);
    }

    public static StaffResponse fromEntity(Staff staff, String username) {
        return fromEntity(staff, username, null);
    }

    public static StaffResponse fromEntity(Staff staff, String username, Boolean accountEnabled) {
        StaffResponse response = new StaffResponse();
        response.setId(staff.getId());
        response.setName(staff.getName());
        response.setPhone(staff.getPhone());
        response.setEmail(staff.getEmail());
        response.setUsername(username);
        response.setAccountEnabled(accountEnabled);
        response.setIsActive(Boolean.TRUE.equals(staff.getIsActive()) && !Boolean.TRUE.equals(staff.getIsDeleted()));
        response.setIsDeleted(Boolean.TRUE.equals(staff.getIsDeleted()));
        response.setShowOnWebsite(staff.getShowOnWebsite());
        response.setAvatarUrl(staff.getAvatarUrl());
        response.setCreatedAt(staff.getCreatedAt());
        response.setUpdatedAt(staff.getUpdatedAt());
        return response;
    }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public Boolean getShowOnWebsite() { return showOnWebsite; }
    public void setShowOnWebsite(Boolean showOnWebsite) { this.showOnWebsite = showOnWebsite; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Boolean getAccountEnabled() { return accountEnabled; }
    public void setAccountEnabled(Boolean accountEnabled) { this.accountEnabled = accountEnabled; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
