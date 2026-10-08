package com.example.spabooking.payment.dto;

public class ProviderQueryResult {

    private final boolean success;
    private final String responseCode;
    private final String transactionStatus;
    private final String transactionNo;
    private final String bankCode;
    private final String payDate;
    private final String message;

    public ProviderQueryResult(boolean success, String responseCode, String transactionStatus,
                               String transactionNo, String bankCode, String payDate, String message) {
        this.success = success;
        this.responseCode = responseCode;
        this.transactionStatus = transactionStatus;
        this.transactionNo = transactionNo;
        this.bankCode = bankCode;
        this.payDate = payDate;
        this.message = message;
    }

    public static ProviderQueryResult success(String responseCode, String transactionStatus,
                                              String transactionNo, String bankCode, String payDate, String message) {
        return new ProviderQueryResult(true, responseCode, transactionStatus, transactionNo, bankCode, payDate, message);
    }

    public static ProviderQueryResult error(String responseCode, String message) {
        return new ProviderQueryResult(false, responseCode, null, null, null, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getPayDate() {
        return payDate;
    }

    public String getMessage() {
        return message;
    }
}
