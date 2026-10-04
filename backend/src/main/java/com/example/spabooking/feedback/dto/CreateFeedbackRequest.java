package com.example.spabooking.feedback.dto;

import com.example.spabooking.feedback.entity.FeedbackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateFeedbackRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String name;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotNull(message = "Loại phản hồi không được để trống")
    private FeedbackType type;

    @Size(max = 50, message = "Mã đặt lịch tối đa 50 ký tự")
    private String bookingCode;

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(max = 2000, message = "Nội dung phản hồi tối đa 2000 ký tự")
    private String message;

    public CreateFeedbackRequest() {}

    public CreateFeedbackRequest(String name, String phone, String email, FeedbackType type, String bookingCode, String message) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.type = type;
        this.bookingCode = bookingCode;
        this.message = message;
    }

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
}
