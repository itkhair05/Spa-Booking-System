package com.example.spabooking.payment.dto;

import com.example.spabooking.payment.enums.RefundStatus;

public class ProviderRefundResult {

    private final RefundStatus status;
    private final String responseCode;
    private final String responseMessage;
    private final String providerTransactionReference;

    public ProviderRefundResult(RefundStatus status, String responseCode, String responseMessage, String providerTransactionReference) {
        this.status = status;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.providerTransactionReference = providerTransactionReference;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public String getProviderTransactionReference() {
        return providerTransactionReference;
    }
}
