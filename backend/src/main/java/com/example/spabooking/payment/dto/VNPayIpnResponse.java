package com.example.spabooking.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard VNPay IPN response payload format.
 * RspCode values:
 * 00: Confirm Success
 * 01: Order not found
 * 02: Order already confirmed
 * 04: Invalid Amount
 * 97: Invalid Checksum
 * 99: Unknown error / Missing required parameters
 */
public class VNPayIpnResponse {

    @JsonProperty("RspCode")
    private String rspCode;

    @JsonProperty("Message")
    private String message;

    public VNPayIpnResponse() {
    }

    public VNPayIpnResponse(String rspCode, String message) {
        this.rspCode = rspCode;
        this.message = message;
    }

    public String getRspCode() {
        return rspCode;
    }

    public void setRspCode(String rspCode) {
        this.rspCode = rspCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
