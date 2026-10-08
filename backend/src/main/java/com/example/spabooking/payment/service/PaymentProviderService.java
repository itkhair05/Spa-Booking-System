package com.example.spabooking.payment.service;

import com.example.spabooking.payment.dto.ProviderQueryResult;
import com.example.spabooking.payment.dto.ProviderRefundResult;
import com.example.spabooking.payment.entity.Payment;
import java.math.BigDecimal;

public interface PaymentProviderService {

    ProviderRefundResult refund(Payment payment, BigDecimal refundAmount, String refundRequestId, String reason, String ipAddress);

    ProviderQueryResult queryTransaction(Payment payment, String ipAddress);
}
