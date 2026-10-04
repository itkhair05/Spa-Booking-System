package com.example.spabooking.publicapi.dto;

import com.example.spabooking.staff.entity.Staff;

public class PublicStaffResponse {
    private Long id;
    private String name;
    private String avatarUrl;

    public static PublicStaffResponse fromEntity(Staff staff) {
        PublicStaffResponse resp = new PublicStaffResponse();
        resp.setId(staff.getId());
        resp.setName(staff.getName());
        resp.setAvatarUrl(staff.getAvatarUrl());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
