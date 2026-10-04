package com.example.spabooking.tenant.dto;

import com.example.spabooking.tenant.entity.Tenant;

public class BusinessProfileResponse {

    private Long id;
    private String name;
    private String slug;
    private String phone;
    private String email;
    private String address;
    private String timezone;

    public static BusinessProfileResponse fromEntity(Tenant tenant) {
        BusinessProfileResponse response = new BusinessProfileResponse();
        response.setId(tenant.getId());
        response.setName(tenant.getName());
        response.setSlug(tenant.getSlug());
        response.setPhone(tenant.getPhone());
        response.setEmail(tenant.getEmail());
        response.setAddress(tenant.getAddress());
        response.setTimezone(tenant.getTimezone());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}
