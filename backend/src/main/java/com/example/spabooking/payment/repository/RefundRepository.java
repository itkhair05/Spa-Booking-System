package com.example.spabooking.payment.repository;

import com.example.spabooking.payment.entity.Refund;
import com.example.spabooking.payment.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByPaymentIdAndTenantId(Long paymentId, Long tenantId);

    Optional<Refund> findByBookingIdAndTenantId(Long bookingId, Long tenantId);

    Optional<Refund> findByRefundRequestId(String refundRequestId);

    List<Refund> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    boolean existsByPaymentIdAndTenantIdAndStatusIn(Long paymentId, Long tenantId, Collection<RefundStatus> statuses);
}
