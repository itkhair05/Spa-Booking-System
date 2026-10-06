package com.example.spabooking.payment.controller;

import com.example.spabooking.payment.dto.InitiateRefundRequest;
import com.example.spabooking.payment.dto.RefundEligibilityResponse;
import com.example.spabooking.payment.dto.RefundResponse;
import com.example.spabooking.payment.entity.Refund;
import com.example.spabooking.payment.service.RefundService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping("/refund-eligibility")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<RefundEligibilityResponse> getRefundEligibility(@PathVariable Long bookingId) {
        RefundEligibilityResponse eligibility = refundService.getRefundEligibility(bookingId, null);
        return ResponseEntity.ok(eligibility);
    }

    @PostMapping("/refund")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<RefundResponse> initiateRefund(
            @PathVariable Long bookingId,
            @RequestBody(required = false) InitiateRefundRequest request,
            HttpServletRequest httpServletRequest) {
        String clientIp = httpServletRequest.getRemoteAddr();
        String reason = request != null ? request.getReason() : null;
        RefundResponse response = refundService.initiateRefund(bookingId, reason, clientIp);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/refund")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<RefundResponse> getRefund(@PathVariable Long bookingId) {
        return refundService.findByBookingId(bookingId)
                .map(RefundResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
