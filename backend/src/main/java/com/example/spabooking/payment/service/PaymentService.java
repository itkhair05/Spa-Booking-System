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
import com.example.spabooking.payment.dto.ProviderQueryResult;
import com.example.spabooking.payment.dto.ReconciliationResponse;
import com.example.spabooking.payment.dto.VNPayIpnResponse;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.notification.event.BookingConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final VNPayService vnPayService;
    private final PaymentProviderService paymentProviderService;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          VNPayService vnPayService,
                          PaymentProviderService paymentProviderService,
                          @org.springframework.beans.factory.annotation.Autowired(required = false) BookingRepository bookingRepository,
                          @org.springframework.beans.factory.annotation.Autowired(required = false) ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.vnPayService = vnPayService;
        this.paymentProviderService = paymentProviderService;
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
    }

    public PaymentService(PaymentRepository paymentRepository, VNPayService vnPayService, PaymentProviderService paymentProviderService) {
        this(paymentRepository, vnPayService, paymentProviderService, null, null);
    }

    public PaymentService(PaymentRepository paymentRepository, VNPayService vnPayService) {
        this(paymentRepository, vnPayService, null, null, null);
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

            confirmBookingOnPaymentSuccess(payment);

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

    @Transactional
    public VNPayIpnResponse processVNPayIpn(Map<String, String> vnpParams) {
        if (vnpParams == null || vnpParams.isEmpty()) {
            return new VNPayIpnResponse("99", "Input data required");
        }

        // 1. Verify signature
        if (!vnPayService.verifySignature(vnpParams)) {
            log.warn("VNPay IPN signature verification failed");
            return new VNPayIpnResponse("97", "Invalid Checksum");
        }

        String txnRef = vnpParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            log.warn("VNPay IPN missing vnp_TxnRef");
            return new VNPayIpnResponse("99", "Missing transaction reference");
        }

        // 2. Lookup payment by txnRef
        Payment payment = paymentRepository.findByTxnRef(txnRef).orElse(null);
        if (payment == null) {
            log.warn("VNPay IPN order not found for txnRef={}", txnRef);
            return new VNPayIpnResponse("01", "Order not found");
        }

        // 3. Amount verification (server-authoritative)
        String amountStr = vnpParams.get("vnp_Amount");
        if (amountStr == null || amountStr.isBlank()) {
            log.warn("VNPay IPN missing vnp_Amount for txnRef={}", txnRef);
            return new VNPayIpnResponse("04", "Invalid Amount");
        }

        long receivedAmount;
        try {
            receivedAmount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            log.warn("VNPay IPN malformed vnp_Amount: {} for txnRef={}", amountStr, txnRef);
            return new VNPayIpnResponse("04", "Invalid Amount");
        }

        long expectedAmount = payment.getAmount().multiply(new BigDecimal(100)).longValue();
        if (receivedAmount != expectedAmount) {
            log.warn("VNPay IPN amount mismatch for txnRef={}: received={}, expected={}",
                    txnRef, receivedAmount, expectedAmount);
            return new VNPayIpnResponse("04", "Invalid Amount");
        }

        String responseCode = vnpParams.get("vnp_ResponseCode");
        String transactionNo = vnpParams.get("vnp_TransactionNo");
        String bankCode = vnpParams.get("vnp_BankCode");
        String cardType = vnpParams.get("vnp_CardType");

        // 4. Idempotency & state transitions
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("VNPay IPN idempotent check: payment id={} txnRef={} already PAID", payment.getId(), txnRef);
            return new VNPayIpnResponse("02", "Order already confirmed");
        }

        if (payment.getStatus() == PaymentStatus.CANCELLED || payment.getStatus() == PaymentStatus.FAILED) {
            log.warn("VNPay IPN received for already terminated payment id={} txnRef={} with status={}",
                    payment.getId(), txnRef, payment.getStatus());
            return new VNPayIpnResponse("02", "Order already confirmed");
        }

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setTransactionNo(transactionNo);
            payment.setBankCode(bankCode);
            payment.setCardType(cardType);
            payment.setResponseCode(responseCode);
            paymentRepository.save(payment);

            confirmBookingOnPaymentSuccess(payment);

            log.info("VNPay IPN payment success confirmed: payment id={}, txnRef={}, vnpTxnNo={}",
                    payment.getId(), txnRef, transactionNo);
            return new VNPayIpnResponse("00", "Confirm Success");
        } else if ("24".equals(responseCode)) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setResponseCode(responseCode);
            paymentRepository.save(payment);

            log.info("VNPay IPN payment cancelled: payment id={}, txnRef={}", payment.getId(), txnRef);
            return new VNPayIpnResponse("00", "Confirm Success");
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setResponseCode(responseCode);
            paymentRepository.save(payment);

            log.info("VNPay IPN payment failed: payment id={}, txnRef={}, responseCode={}",
                    payment.getId(), txnRef, responseCode);
            return new VNPayIpnResponse("00", "Confirm Success");
        }
    }

    @Transactional
    public ReconciliationResponse reconcilePayment(Long paymentId, String clientIp) {
        Long tenantId = TenantContext.requireTenantId();
        Payment payment = paymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for id: " + paymentId));

        if (payment.getProvider() != PaymentProvider.VNPAY) {
            throw new IllegalArgumentException("Reconciliation is only supported for VNPay payments");
        }

        if (paymentProviderService == null) {
            throw new IllegalStateException("Payment provider service is not available");
        }

        String localStatusBefore = payment.getStatus().name();
        ProviderQueryResult queryResult = paymentProviderService.queryTransaction(payment, clientIp);

        if (!queryResult.isSuccess()) {
            log.warn("VNPay QueryDR failed for payment id={}, txnRef={}: code={}, message={}",
                    payment.getId(), payment.getTxnRef(), queryResult.getResponseCode(), queryResult.getMessage());
            return new ReconciliationResponse(
                    payment.getId(),
                    payment.getTxnRef(),
                    localStatusBefore,
                    payment.getStatus().name(),
                    queryResult.getTransactionStatus(),
                    queryResult.getResponseCode(),
                    queryResult.getMessage(),
                    false,
                    false,
                    "QueryDR call failed: " + queryResult.getMessage(),
                    LocalDateTime.now()
            );
        }

        String remoteStatus = queryResult.getTransactionStatus();
        String remoteTxnNo = queryResult.getTransactionNo();
        String bankCode = queryResult.getBankCode();
        String payDate = queryResult.getPayDate();

        // Safe reconciliation transitions
        if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.UNPAID) {
            if ("00".equals(remoteStatus)) {
                payment.setStatus(PaymentStatus.PAID);
                if (remoteTxnNo != null && !remoteTxnNo.isBlank()) {
                    payment.setTransactionNo(remoteTxnNo);
                }
                if (bankCode != null && !bankCode.isBlank()) {
                    payment.setBankCode(bankCode);
                }
                payment.setResponseCode("00");
                if (payDate != null && payDate.length() == 14) {
                    try {
                        payment.setPaidAt(LocalDateTime.parse(payDate, DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                    } catch (Exception e) {
                        payment.setPaidAt(LocalDateTime.now());
                    }
                } else if (payment.getPaidAt() == null) {
                    payment.setPaidAt(LocalDateTime.now());
                }
                paymentRepository.save(payment);

                confirmBookingOnPaymentSuccess(payment);

                log.info("VNPay QueryDR reconciled payment id={} from {} to PAID", payment.getId(), localStatusBefore);
                return new ReconciliationResponse(
                        payment.getId(), payment.getTxnRef(), localStatusBefore, payment.getStatus().name(),
                        remoteStatus, queryResult.getResponseCode(), queryResult.getMessage(),
                        true, false, "Payment reconciled successfully: updated from " + localStatusBefore + " to PAID",
                        LocalDateTime.now()
                );
            } else if ("01".equals(remoteStatus)) {
                log.info("VNPay QueryDR: payment id={} still pending at VNPay (status 01)", payment.getId());
                return new ReconciliationResponse(
                        payment.getId(), payment.getTxnRef(), localStatusBefore, payment.getStatus().name(),
                        remoteStatus, queryResult.getResponseCode(), queryResult.getMessage(),
                        true, false, "Payment is still pending at VNPay (status 01)",
                        LocalDateTime.now()
                );
            } else if ("02".equals(remoteStatus)) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setResponseCode(remoteStatus);
                paymentRepository.save(payment);

                log.info("VNPay QueryDR reconciled payment id={} from {} to FAILED", payment.getId(), localStatusBefore);
                return new ReconciliationResponse(
                        payment.getId(), payment.getTxnRef(), localStatusBefore, payment.getStatus().name(),
                        remoteStatus, queryResult.getResponseCode(), queryResult.getMessage(),
                        true, false, "Payment reconciled: updated from " + localStatusBefore + " to FAILED",
                        LocalDateTime.now()
                );
            } else {
                // Status 04 (reversed) or other abnormal/discrepancy status:
                // Remain conservative / report discrepancy, do NOT mark PAID or FAILED
                log.warn("VNPay QueryDR discrepancy for pending payment id={}: VNPay reported status {}",
                        payment.getId(), remoteStatus);
                return new ReconciliationResponse(
                        payment.getId(), payment.getTxnRef(), localStatusBefore, payment.getStatus().name(),
                        remoteStatus, queryResult.getResponseCode(), queryResult.getMessage(),
                        false, true, "Discrepancy detected: VNPay returned status " + remoteStatus + ". Payment not mutated.",
                        LocalDateTime.now()
                );
            }
        } else if (payment.getStatus() == PaymentStatus.PAID) {
            if ("00".equals(remoteStatus)) {
                return new ReconciliationResponse(
                        payment.getId(), payment.getTxnRef(), localStatusBefore, payment.getStatus().name(),
                        remoteStatus, queryResult.getResponseCode(), queryResult.getMessage(),
                        true, false, "Payment is already marked as PAID and matches VNPay status",
                        LocalDateTime.now()
                );
            } else {
                log.warn("Reconciliation discrepancy: payment id={} is locally PAID but VNPay status is {}",
                        payment.getId(), remoteStatus);
                return new ReconciliationResponse(
                        payment.getId(), payment.getTxnRef(), localStatusBefore, payment.getStatus().name(),
                        remoteStatus, queryResult.getResponseCode(), queryResult.getMessage(),
                        false, true, "Discrepancy detected: Local status is PAID but VNPay status is " + remoteStatus + ". Payment was not downgraded.",
                        LocalDateTime.now()
                );
            }
        } else {
            if ("00".equals(remoteStatus)) {
                log.warn("Reconciliation discrepancy: payment id={} is locally {} but VNPay reported success (00)",
                        payment.getId(), localStatusBefore);
                return new ReconciliationResponse(
                        payment.getId(), payment.getTxnRef(), localStatusBefore, payment.getStatus().name(),
                        remoteStatus, queryResult.getResponseCode(), queryResult.getMessage(),
                        false, true, "Discrepancy detected: Local status is " + localStatusBefore + " but VNPay reported success (00). Manual review required.",
                        LocalDateTime.now()
                );
            } else {
                return new ReconciliationResponse(
                        payment.getId(), payment.getTxnRef(), localStatusBefore, payment.getStatus().name(),
                        remoteStatus, queryResult.getResponseCode(), queryResult.getMessage(),
                        true, false, "Payment status is consistent (" + localStatusBefore + ")",
                        LocalDateTime.now()
                );
            }
        }
    }

    private void confirmBookingOnPaymentSuccess(Payment payment) {
        if (payment == null || payment.getBooking() == null) {
            return;
        }
        Booking booking = payment.getBooking();
        boolean shouldPublishEvent = false;

        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
            if (booking.getConfirmedAt() == null) {
                booking.setConfirmedAt(LocalDateTime.now());
            }
            if (bookingRepository != null) {
                booking = bookingRepository.save(booking);
            }
            shouldPublishEvent = true;
        } else if (booking.getStatus() == BookingStatus.CONFIRMED && booking.getConfirmationEmailSentAt() == null) {
            shouldPublishEvent = true;
        }

        if (shouldPublishEvent && eventPublisher != null) {
            eventPublisher.publishEvent(new BookingConfirmedEvent(booking.getId()));
        }
    }
}
