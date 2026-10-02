package com.example.spabooking.booking.repository;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByTenantId(Long tenantId);
    
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.customer " +
           "JOIN FETCH b.service " +
           "JOIN FETCH b.staff " +
           "WHERE b.tenant.id = :tenantId " +
           "AND (:staffId IS NULL OR b.staff.id = :staffId) " +
           "AND (:startDate IS NULL OR b.startTime >= :startDate) " +
           "AND (:endDate IS NULL OR b.endTime <= :endDate) " +
           "AND (:status IS NULL OR b.status = :status) " +
           "ORDER BY b.startTime ASC")
    List<Booking> findAllByFilters(
            @Param("tenantId") Long tenantId,
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.startTime >= :startDate AND b.startTime < :endDate")
    long countTodayBookings(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.startTime >= :now AND b.status != 'CANCELLED'")
    long countUpcomingBookings(
            @Param("tenantId") Long tenantId,
            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tenant.id = :tenantId AND b.status = :status")
    long countByTenantIdAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("status") BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.price), 0) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.startTime >= :startDate AND b.startTime < :endDate " +
           "AND b.status IN :statuses")
    java.math.BigDecimal sumExpectedRevenue(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("statuses") List<BookingStatus> statuses);

    Optional<Booking> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.staff.id = :staffId " +
           "AND b.status != 'CANCELLED' " +
           "AND b.startTime < :endTime AND b.endTime > :startTime " +
           "AND (:excludeBookingId IS NULL OR b.id != :excludeBookingId)")
    long countOverlappingStaffBookings(
            @Param("tenantId") Long tenantId,
            @Param("staffId") Long staffId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeBookingId") Long excludeBookingId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.customer.id = :customerId " +
           "AND b.status != 'CANCELLED' " +
           "AND b.startTime < :endTime AND b.endTime > :startTime " +
           "AND (:excludeBookingId IS NULL OR b.id != :excludeBookingId)")
    long countOverlappingCustomerBookings(
            @Param("tenantId") Long tenantId,
            @Param("customerId") Long customerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeBookingId") Long excludeBookingId);
}
