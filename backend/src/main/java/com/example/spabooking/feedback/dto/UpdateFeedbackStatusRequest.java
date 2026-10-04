package com.example.spabooking.feedback.dto;

import com.example.spabooking.feedback.entity.FeedbackStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateFeedbackStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private FeedbackStatus status;

    public UpdateFeedbackStatusRequest() {}

    public UpdateFeedbackStatusRequest(FeedbackStatus status) {
        this.status = status;
    }

    public FeedbackStatus getStatus() { return status; }
    public void setStatus(FeedbackStatus status) { this.status = status; }
}
