package com.example.spabooking.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateBusinessProfileRequest {

    @NotBlank(message = "Spa name cannot be blank")
    @Size(min = 2, max = 100, message = "Spa name must be between 2 and 100 characters")
    private String name;

    private String phone;

    @Email(message = "Email must be a valid email address")
    private String email;

    private String address;

    private String timezone;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}
