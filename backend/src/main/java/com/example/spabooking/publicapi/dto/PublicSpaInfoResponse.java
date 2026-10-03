package com.example.spabooking.publicapi.dto;

import com.example.spabooking.tenant.entity.Tenant;

public class PublicSpaInfoResponse {
    private String name;
    private String slug;
    private String phone;
    private String email;
    private String address;
    private String timezone;

    public static PublicSpaInfoResponse fromEntity(Tenant tenant) {
        PublicSpaInfoResponse resp = new PublicSpaInfoResponse();
        resp.setName(tenant.getName());
        resp.setSlug(tenant.getSlug());
        resp.setPhone(tenant.getPhone());
        resp.setEmail(tenant.getEmail());
        resp.setAddress(tenant.getAddress());
        resp.setTimezone(tenant.getTimezone());
        return resp;
    }

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
