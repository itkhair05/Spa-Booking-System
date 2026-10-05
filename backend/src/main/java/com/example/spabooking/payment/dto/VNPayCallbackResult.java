package com.example.spabooking.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VNPayCallbackResult {

    private boolean success;
    private String message;
    private String bookingCode;
    private String txnRef;
    private BigDecimal amount;
    private String status;
    private String responseCode;
    private String transactionNo;
    private LocalDateTime paidAt;

    public VNPayCallbackResult() {}

    public VNPayCallbackResult(boolean success, String message, String bookingCode, String txnRef,
                               BigDecimal amount, String status, String responseCode, String transactionNo, LocalDateTime paidAt) {
        this.success = success;
        this.message = message;
        this.bookingCode = bookingCode;
        this.txnRef = txnRef;
        this.amount = amount;
        this.status = status;
        this.responseCode = responseCode;
        this.transactionNo = transactionNo;
        this.paidAt = paidAt;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public String getTxnRef() { return txnRef; }
    public void setTxnRef(String txnRef) { this.txnRef = txnRef; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
