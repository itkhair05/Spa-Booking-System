package com.example.spabooking.payment.service;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.payment.dto.VNPayCallbackResult;
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.enums.PaymentMethod;
import com.example.spabooking.payment.enums.PaymentProvider;
import com.example.spabooking.payment.enums.PaymentStatus;
import com.example.spabooking.payment.repository.PaymentRepository;
import com.example.spabooking.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final VNPayService vnPayService;

    public PaymentService(PaymentRepository paymentRepository, VNPayService vnPayService) {
        this.paymentRepository = paymentRepository;
        this.vnPayService = vnPayService;
    }

    @Transactional
    public Payment createPaymentForBooking(Booking booking, PaymentMethod method, PaymentProvider provider) {
        Payment payment = new Payment();
        payment.setTenant(booking.getTenant());
        payment.setBooking(booking);
        // Strictly use authoritative amount from booking
        payment.setAmount(booking.getPrice());
        payment.setPaymentMethod(method);
        payment.setProvider(provider);

        if (method == PaymentMethod.VNPAY) {
            payment.setStatus(PaymentStatus.PENDING);
        } else {
            payment.setStatus(PaymentStatus.UNPAID);
        }

        String txnRef = "TIKEY-" + booking.getBookingCode() + "-" + System.currentTimeMillis();
        payment.setTxnRef(txnRef);

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByBookingId(Long bookingId) {
        Long tenantId = TenantContext.requireTenantId();
        return paymentRepository.findByBookingIdAndTenantId(bookingId, tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByBookingCode(String bookingCode) {
        Long tenantId = TenantContext.requireTenantId();
        return paymentRepository.findByBookingBookingCodeAndTenantId(bookingCode, tenantId);
    }

    @Transactional(readOnly = true)
    public List<Payment> findAllForCurrentTenant() {
        Long tenantId = TenantContext.requireTenantId();
        return paymentRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public VNPayCallbackResult processVNPayCallback(Map<String, String> vnpParams) {
        // 1. Verify signature
        if (!vnPayService.verifySignature(vnpParams)) {
            return new VNPayCallbackResult(false, "Chữ ký không hợp lệ", null, null,
                    null, PaymentStatus.FAILED.name(), "97", null, null);
        }

        String txnRef = vnpParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            return new VNPayCallbackResult(false, "Thiếu mã tham chiếu giao dịch", null, null,
                    null, PaymentStatus.FAILED.name(), "99", null, null);
        }

        // 2. Lookup payment by txnRef
        Payment payment = paymentRepository.findByTxnRef(txnRef)
                .orElse(null);
        if (payment == null) {
            return new VNPayCallbackResult(false, "Không tìm thấy thông tin thanh toán", null, txnRef,
                    null, PaymentStatus.FAILED.name(), "01", null, null);
        }

        // 3. Verify amount
        String amountStr = vnpParams.get("vnp_Amount");
        if (amountStr == null) {
            return new VNPayCallbackResult(false, "Thiếu số tiền giao dịch", payment.getBooking().getBookingCode(), txnRef,
                    payment.getAmount(), payment.getStatus().name(), "04", null, null);
        }

        long receivedAmount = Long.parseLong(amountStr);
        long expectedAmount = payment.getAmount().multiply(new BigDecimal(100)).longValue();
        if (receivedAmount != expectedAmount) {
            return new VNPayCallbackResult(false, "Số tiền giao dịch không khớp", payment.getBooking().getBookingCode(), txnRef,
                    payment.getAmount(), payment.getStatus().name(), "04", null, null);
        }

        String responseCode = vnpParams.get("vnp_ResponseCode");
        String transactionNo = vnpParams.get("vnp_TransactionNo");
        String bankCode = vnpParams.get("vnp_BankCode");
        String cardType = vnpParams.get("vnp_CardType");

        // 4. Idempotent check
        if (payment.getStatus() == PaymentStatus.PAID) {
            return new VNPayCallbackResult(true, "Giao dịch đã được thanh toán thành công",
                    payment.getBooking().getBookingCode(), txnRef, payment.getAmount(),
                    payment.getStatus().name(), responseCode, payment.getTransactionNo(), payment.getPaidAt());
        }

        // 5. Status transitions
        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setTransactionNo(transactionNo);
            payment.setBankCode(bankCode);
            payment.setCardType(cardType);
            payment.setResponseCode(responseCode);
            paymentRepository.save(payment);

            return new VNPayCallbackResult(true, "Thanh toán VNPay thành công",
                    payment.getBooking().getBookingCode(), txnRef, payment.getAmount(),
                    payment.getStatus().name(), responseCode, transactionNo, payment.getPaidAt());
        } else if ("24".equals(responseCode)) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setResponseCode(responseCode);
            paymentRepository.save(payment);

            return new VNPayCallbackResult(false, "Khách hàng đã hủy giao dịch",
                    payment.getBooking().getBookingCode(), txnRef, payment.getAmount(),
                    payment.getStatus().name(), responseCode, transactionNo, null);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setResponseCode(responseCode);
            paymentRepository.save(payment);

            return new VNPayCallbackResult(false, "Giao dịch thanh toán thất bại",
                    payment.getBooking().getBookingCode(), txnRef, payment.getAmount(),
                    payment.getStatus().name(), responseCode, transactionNo, null);
        }
    }
}
