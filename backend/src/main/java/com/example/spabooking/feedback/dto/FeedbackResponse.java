package com.example.spabooking.feedback.dto;

import com.example.spabooking.feedback.entity.FeedbackStatus;
import com.example.spabooking.feedback.entity.FeedbackType;

import java.time.LocalDateTime;

public class FeedbackResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private FeedbackType type;
    private String bookingCode;
    private String message;
    private FeedbackStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FeedbackResponse() {}

    public FeedbackResponse(Long id, String name, String phone, String email, FeedbackType type,
                            String bookingCode, String message, FeedbackStatus status,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.type = type;
        this.bookingCode = bookingCode;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public FeedbackType getType() { return type; }
    public void setType(FeedbackType type) { this.type = type; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public FeedbackStatus getStatus() { return status; }
    public void setStatus(FeedbackStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
