package com.example.spabooking.payment.repository;

import com.example.spabooking.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Payment> findByTxnRef(String txnRef);

    Optional<Payment> findByTxnRefAndTenantId(String txnRef, Long tenantId);

    Optional<Payment> findByBookingIdAndTenantId(Long bookingId, Long tenantId);

    Optional<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByBookingBookingCodeAndTenantId(String bookingCode, Long tenantId);

    List<Payment> findAllByTenantId(Long tenantId);

    List<Payment> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
