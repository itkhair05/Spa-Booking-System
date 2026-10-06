package com.example.spabooking.payment.service;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.payment.dto.ProviderRefundResult;
import com.example.spabooking.payment.dto.RefundEligibilityResponse;
import com.example.spabooking.payment.dto.RefundResponse;
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.entity.Refund;
import com.example.spabooking.payment.enums.PaymentMethod;
import com.example.spabooking.payment.enums.PaymentStatus;
import com.example.spabooking.payment.enums.RefundStatus;
import com.example.spabooking.payment.repository.PaymentRepository;
import com.example.spabooking.payment.repository.RefundRepository;
import com.example.spabooking.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class RefundService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentProviderService paymentProviderService;

    public RefundService(RefundRepository refundRepository,
                         PaymentRepository paymentRepository,
                         BookingRepository bookingRepository,
                         PaymentProviderService paymentProviderService) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.paymentProviderService = paymentProviderService;
    }

    /**
     * Authoritative server-side evaluation of cancellation policy and refund eligibility.
     */
    @Transactional(readOnly = true)
    public RefundEligibilityResponse getRefundEligibility(Long bookingId, LocalDateTime customCancellationTime) {
        Long tenantId = TenantContext.requireTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(bookingId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        return calculateEligibility(booking, tenantId, customCancellationTime);
    }

    public RefundEligibilityResponse calculateEligibility(Booking booking, Long tenantId, LocalDateTime customCancellationTime) {
        RefundEligibilityResponse resp = new RefundEligibilityResponse();
        resp.setBookingId(booking.getId());
        resp.setBookingCode(booking.getBookingCode());
        resp.setBookingStatus(booking.getStatus().name());

        LocalDateTime deadline = booking.getStartTime() != null ? booking.getStartTime().minusHours(24) : null;
        resp.setDeadline(deadline);

        Optional<Payment> paymentOpt = paymentRepository.findByBookingIdAndTenantId(booking.getId(), tenantId);
        if (paymentOpt.isEmpty()) {
            resp.setRefundEligible(false);
            resp.setReason("Lịch hẹn chưa có thông tin thanh toán");
            resp.setOriginalPaidAmount(BigDecimal.ZERO);
            resp.setRefundAmount(BigDecimal.ZERO);
            resp.setCancellationFee(BigDecimal.ZERO);
            resp.setPolicyPercentage(0);
            return resp;
        }

        Payment payment = paymentOpt.get();
        resp.setPaymentStatus(payment.getStatus().name());
        resp.setPaymentMethod(payment.getPaymentMethod().name());
        resp.setOriginalPaidAmount(payment.getAmount());

        // Check if an existing refund record exists
        Optional<Refund> existingRefund = refundRepository.findByPaymentIdAndTenantId(payment.getId(), tenantId);
        if (existingRefund.isPresent()) {
            resp.setExistingRefundStatus(existingRefund.get().getStatus());
            resp.setRefundRequestId(existingRefund.get().getRefundRequestId());
        }

        // 1. Status checks
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            resp.setRefundEligible(false);
            resp.setReason("Lịch hẹn đã hoàn tất. Không áp dụng chính sách hủy/hoàn tiền.");
            resp.setRefundAmount(BigDecimal.ZERO);
            resp.setCancellationFee(BigDecimal.ZERO);
            resp.setPolicyPercentage(0);
            resp.setPolicyDescription("Lịch hẹn đã hoàn tất: Không hoàn tiền");
            return resp;
        }

        if (booking.getStatus() == BookingStatus.NO_SHOW) {
            resp.setRefundEligible(false);
            resp.setReason("Khách không đến (No-show). Không hoàn tiền theo chính sách TIKEY SPA.");
            resp.setRefundAmount(BigDecimal.ZERO);
            resp.setCancellationFee(payment.getAmount());
            resp.setPolicyPercentage(0);
            resp.setPolicyDescription("Vắng mặt (No-show): Không hoàn tiền (phí hủy 100%)");
            return resp;
        }

        if (payment.getStatus() != PaymentStatus.PAID
                && payment.getStatus() != PaymentStatus.REFUND_PENDING
                && payment.getStatus() != PaymentStatus.REFUNDED
                && payment.getStatus() != PaymentStatus.REFUND_FAILED) {
            resp.setRefundEligible(false);
            resp.setReason("Chưa thanh toán hoặc giao dịch không thành công");
            resp.setRefundAmount(BigDecimal.ZERO);
            resp.setCancellationFee(BigDecimal.ZERO);
            resp.setPolicyPercentage(0);
            resp.setPolicyDescription("Chưa phát sinh giao dịch thanh toán thành công");
            return resp;
        }

        if (payment.getPaymentMethod() != PaymentMethod.VNPAY) {
            resp.setRefundEligible(false);
            resp.setReason("Phương thức thanh toán " + payment.getPaymentMethod() + " không hỗ trợ hoàn tiền tự động qua cổng thanh toán");
            resp.setRefundAmount(BigDecimal.ZERO);
            resp.setCancellationFee(BigDecimal.ZERO);
            resp.setPolicyPercentage(0);
            return resp;
        }

        // 2. Cancellation time calculation
        LocalDateTime cancelTime = customCancellationTime;
        if (cancelTime == null) {
            cancelTime = booking.getCancelledAt() != null
                    ? booking.getCancelledAt()
                    : LocalDateTime.now(BUSINESS_ZONE);
        }

        // Policy evaluation
        // Policy A: cancel >= 24h before appointment -> 100% refund, 0% fee
        // Policy B: cancel < 24h before appointment -> 90% refund, 10% fee
        if (deadline != null && !cancelTime.isAfter(deadline)) {
            // >= 24 hours
            resp.setRefundEligible(true);
            resp.setPolicyPercentage(100);
            resp.setRefundAmount(payment.getAmount());
            resp.setCancellationFee(BigDecimal.ZERO);
            resp.setPolicyDescription("Hủy trước 24 giờ so với giờ hẹn: Hoàn 100% tiền đã thanh toán (Phí hủy: 0 ₫)");
        } else {
            // < 24 hours
            resp.setRefundEligible(true);
            resp.setPolicyPercentage(90);
            BigDecimal refundAmt = payment.getAmount().multiply(new BigDecimal("0.90")).setScale(0, RoundingMode.HALF_UP);
            BigDecimal feeAmt = payment.getAmount().subtract(refundAmt);
            resp.setRefundAmount(refundAmt);
            resp.setCancellationFee(feeAmt);
            resp.setPolicyDescription("Hủy trong vòng 24 giờ trước giờ hẹn: Hoàn 90% tiền đã thanh toán (Phí hủy: 10%)");
        }

        return resp;
    }

    /**
     * Executes the refund operation idempotently for an eligible booking.
     */
    @Transactional
    public RefundResponse initiateRefund(Long bookingId, String reason, String ipAddress) {
        Long tenantId = TenantContext.requireTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(bookingId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Payment payment = paymentRepository.findByBookingIdAndTenantId(bookingId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin thanh toán cho lịch hẹn này"));

        // Idempotency: If refund is already completed or pending, return existing state
        Optional<Refund> existingOpt = refundRepository.findByPaymentIdAndTenantId(payment.getId(), tenantId);
        if (existingOpt.isPresent()) {
            Refund existing = existingOpt.get();
            if (existing.getStatus() == RefundStatus.REFUNDED || existing.getStatus() == RefundStatus.REFUND_PENDING) {
                return RefundResponse.fromEntity(existing);
            }
        }

        // Verify eligibility
        RefundEligibilityResponse eligibility = calculateEligibility(booking, tenantId, null);
        if (!eligibility.isRefundEligible()) {
            throw new IllegalArgumentException("Lịch hẹn không đủ điều kiện hoàn tiền: " + eligibility.getReason());
        }

        BigDecimal refundAmount = eligibility.getRefundAmount();
        BigDecimal cancellationFee = eligibility.getCancellationFee();
        Integer policyPercentage = eligibility.getPolicyPercentage();

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn phải lớn hơn 0");
        }
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Số tiền hoàn không được vượt quá số tiền đã thanh toán");
        }

        // Create or reuse refund record
        Refund refund = existingOpt.orElseGet(Refund::new);
        String refundRequestId = "TIKEY-REF-" + booking.getBookingCode() + "-" + System.currentTimeMillis();

        refund.setTenant(booking.getTenant());
        refund.setBooking(booking);
        refund.setPayment(payment);
        refund.setRefundRequestId(refundRequestId);
        refund.setProvider(payment.getProvider());
        refund.setOriginalAmount(payment.getAmount());
        refund.setRefundAmount(refundAmount);
        refund.setCancellationFee(cancellationFee);
        refund.setPolicyPercentage(policyPercentage);
        refund.setReason(reason != null && !reason.isBlank() ? reason.trim() : "Khách yêu cầu hủy lịch hẹn");
        refund.setStatus(RefundStatus.REFUND_PENDING);
        refund.setRequestedAt(LocalDateTime.now());

        payment.setStatus(PaymentStatus.REFUND_PENDING);
        paymentRepository.save(payment);
        refund = refundRepository.save(refund);

        // Call provider
        ProviderRefundResult result = paymentProviderService.refund(
                payment,
                refundAmount,
                refundRequestId,
                refund.getReason(),
                ipAddress
        );

        // Update state based on provider response
        refund.setStatus(result.getStatus());
        refund.setProviderResponseCode(result.getResponseCode());
        refund.setProviderResponseMessage(result.getResponseMessage());
        refund.setProviderTransactionReference(result.getProviderTransactionReference());

        if (result.getStatus() == RefundStatus.REFUNDED) {
            refund.setProcessedAt(LocalDateTime.now());
            payment.setStatus(PaymentStatus.REFUNDED);
        } else if (result.getStatus() == RefundStatus.REFUND_PENDING) {
            payment.setStatus(PaymentStatus.REFUND_PENDING);
        } else {
            payment.setStatus(PaymentStatus.REFUND_FAILED);
        }

        paymentRepository.save(payment);
        refund = refundRepository.save(refund);

        return RefundResponse.fromEntity(refund);
    }

    @Transactional(readOnly = true)
    public Optional<Refund> findByBookingId(Long bookingId) {
        Long tenantId = TenantContext.requireTenantId();
        return refundRepository.findByBookingIdAndTenantId(bookingId, tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<Refund> findByBookingCode(String bookingCode) {
        Long tenantId = TenantContext.requireTenantId();
        return bookingRepository.findByBookingCodeAndTenantId(bookingCode, tenantId)
                .flatMap(b -> refundRepository.findByBookingIdAndTenantId(b.getId(), tenantId));
    }
}
