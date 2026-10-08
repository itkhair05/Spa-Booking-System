package com.example.spabooking.payment.controller;

import com.example.spabooking.booking.service.BookingService;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.common.ratelimit.ClientIpResolver;
import com.example.spabooking.payment.dto.PaymentResponse;
import com.example.spabooking.payment.dto.ReconciliationResponse;
import com.example.spabooking.payment.dto.VNPayIpnResponse;
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final ClientIpResolver clientIpResolver;

    @Autowired
    public PaymentController(PaymentService paymentService, BookingService bookingService, @Autowired(required = false) ClientIpResolver clientIpResolver) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.clientIpResolver = clientIpResolver;
    }

    public PaymentController(PaymentService paymentService, BookingService bookingService) {
        this(paymentService, bookingService, null);
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        List<Payment> payments = paymentService.findAllForCurrentTenant();
        List<PaymentResponse> responses = payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable Long bookingId) {
        // findById enforces staff-own booking access before payment data is exposed
        bookingService.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        Payment payment = paymentService.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + bookingId));
        return ResponseEntity.ok(PaymentResponse.fromEntity(payment));
    }

    @RequestMapping(value = {"/vnpay-ipn", "/vnpay/ipn"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<VNPayIpnResponse> handleVNPayIpn(@RequestParam Map<String, String> allParams) {
        try {
            VNPayIpnResponse response = paymentService.processVNPayIpn(allParams);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.ok(new VNPayIpnResponse("99", "Unknown error"));
        }
    }

    @PostMapping("/{paymentId}/reconcile")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReconciliationResponse> reconcilePayment(
            @PathVariable Long paymentId,
            HttpServletRequest request) {
        String clientIp = clientIpResolver != null ? clientIpResolver.resolveClientIp(request) : request.getRemoteAddr();
        ReconciliationResponse response = paymentService.reconcilePayment(paymentId, clientIp);
        return ResponseEntity.ok(response);
    }
}
