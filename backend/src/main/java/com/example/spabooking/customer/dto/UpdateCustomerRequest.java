package com.example.spabooking.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateCustomerRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @jakarta.validation.constraints.Pattern(regexp = "^(0\\d{9})?$", message = "Số điện thoại phải gồm đúng 10 chữ số")
    private String phone;

    @Email(message = "Email must be a valid email address")
    private String email;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        this.phone = phone != null ? phone.replaceAll("[\\s.-]", "") : null;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
