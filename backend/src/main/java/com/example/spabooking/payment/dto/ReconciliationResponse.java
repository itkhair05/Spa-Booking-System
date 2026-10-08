package com.example.spabooking.payment.dto;

import java.time.LocalDateTime;

public class ReconciliationResponse {

    private Long paymentId;
    private String txnRef;
    private String localStatusBefore;
    private String localStatusAfter;
    private String remoteTransactionStatus;
    private String remoteResponseCode;
    private String remoteMessage;
    private boolean reconciled;
    private boolean discrepancy;
    private String message;
    private LocalDateTime reconciledAt;

    public ReconciliationResponse() {
    }

    public ReconciliationResponse(Long paymentId, String txnRef, String localStatusBefore, String localStatusAfter,
                                  String remoteTransactionStatus, String remoteResponseCode, String remoteMessage,
                                  boolean reconciled, boolean discrepancy, String message, LocalDateTime reconciledAt) {
        this.paymentId = paymentId;
        this.txnRef = txnRef;
        this.localStatusBefore = localStatusBefore;
        this.localStatusAfter = localStatusAfter;
        this.remoteTransactionStatus = remoteTransactionStatus;
        this.remoteResponseCode = remoteResponseCode;
        this.remoteMessage = remoteMessage;
        this.reconciled = reconciled;
        this.discrepancy = discrepancy;
        this.message = message;
        this.reconciledAt = reconciledAt;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getTxnRef() {
        return txnRef;
    }

    public void setTxnRef(String txnRef) {
        this.txnRef = txnRef;
    }

    public String getLocalStatusBefore() {
        return localStatusBefore;
    }

    public void setLocalStatusBefore(String localStatusBefore) {
        this.localStatusBefore = localStatusBefore;
    }

    public String getLocalStatusAfter() {
        return localStatusAfter;
    }

    public void setLocalStatusAfter(String localStatusAfter) {
        this.localStatusAfter = localStatusAfter;
    }

    public String getRemoteTransactionStatus() {
        return remoteTransactionStatus;
    }

    public void setRemoteTransactionStatus(String remoteTransactionStatus) {
        this.remoteTransactionStatus = remoteTransactionStatus;
    }

    public String getRemoteResponseCode() {
        return remoteResponseCode;
    }

    public void setRemoteResponseCode(String remoteResponseCode) {
        this.remoteResponseCode = remoteResponseCode;
    }

    public String getRemoteMessage() {
        return remoteMessage;
    }

    public void setRemoteMessage(String remoteMessage) {
        this.remoteMessage = remoteMessage;
    }

    public boolean isReconciled() {
        return reconciled;
    }

    public void setReconciled(boolean reconciled) {
        this.reconciled = reconciled;
    }

    public boolean isDiscrepancy() {
        return discrepancy;
    }

    public void setDiscrepancy(boolean discrepancy) {
        this.discrepancy = discrepancy;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(LocalDateTime reconciledAt) {
        this.reconciledAt = reconciledAt;
    }
}
