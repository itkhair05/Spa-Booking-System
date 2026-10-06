package com.example.spabooking.payment.dto;

public class InitiateRefundRequest {

    private String reason;

    public InitiateRefundRequest() {}

    public InitiateRefundRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
