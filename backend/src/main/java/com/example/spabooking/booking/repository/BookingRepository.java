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
           "AND b.startTime >= :startDate AND b.startTime < :endDate " +
           "AND b.status != 'CANCELLED'")
    long countTodayBookings(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.staff.id = :staffId " +
           "AND b.startTime >= :startDate AND b.startTime < :endDate " +
           "AND b.status != 'CANCELLED'")
    long countTodayBookingsByStaff(
            @Param("tenantId") Long tenantId,
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.startTime >= :now AND b.status != 'CANCELLED'")
    long countUpcomingBookings(
            @Param("tenantId") Long tenantId,
            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.staff.id = :staffId " +
           "AND b.startTime >= :now AND b.status != 'CANCELLED'")
    long countUpcomingBookingsByStaff(
            @Param("tenantId") Long tenantId,
            @Param("staffId") Long staffId,
            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tenant.id = :tenantId AND b.status = :status")
    long countByTenantIdAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("status") BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.staff.id = :staffId AND b.status = :status")
    long countByTenantIdAndStaffIdAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("staffId") Long staffId,
            @Param("status") BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.price), 0) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.startTime >= :startDate AND b.startTime < :endDate " +
           "AND b.status = 'COMPLETED'")
    java.math.BigDecimal sumCompletedRevenue(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(b.price), 0) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.startTime >= :startDate AND b.startTime < :endDate " +
           "AND b.status = 'CONFIRMED'")
    java.math.BigDecimal sumExpectedRevenue(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(b.price), 0) FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.startTime >= :startDate AND b.startTime < :endDate " +
           "AND b.status IN :statuses")
    java.math.BigDecimal sumRevenueByStatuses(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.customer " +
           "JOIN FETCH b.service " +
           "JOIN FETCH b.staff " +
           "WHERE b.id = :id AND b.tenant.id = :tenantId")
    Optional<Booking> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.customer " +
           "JOIN FETCH b.service " +
           "JOIN FETCH b.staff " +
           "WHERE b.bookingCode = :bookingCode AND b.tenant.id = :tenantId")
    Optional<Booking> findByBookingCodeAndTenantId(@Param("bookingCode") String bookingCode, @Param("tenantId") Long tenantId);

    Optional<Booking> findByBookingCode(String bookingCode);

    @Query("SELECT b.startTime FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.startTime >= :startDate AND b.startTime < :endDate " +
           "AND b.status != 'CANCELLED'")
    List<LocalDateTime> findStartTimesInRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b.status, COUNT(b) FROM Booking b WHERE b.tenant.id = :tenantId GROUP BY b.status")
    List<Object[]> countBookingsByStatus(@Param("tenantId") Long tenantId);

    @Query("SELECT s.name, COUNT(b) FROM Booking b JOIN b.service s " +
           "WHERE b.tenant.id = :tenantId AND b.status != 'CANCELLED' " +
           "GROUP BY s.id, s.name ORDER BY COUNT(b) DESC")
    List<Object[]> countBookingsByService(
            @Param("tenantId") Long tenantId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.price), 0) FROM Booking b WHERE b.tenant.id = :tenantId AND b.status = 'COMPLETED'")
    java.math.BigDecimal sumTotalCompletedRevenue(@Param("tenantId") Long tenantId);

    @Query("SELECT b.startTime, b.price FROM Booking b WHERE b.tenant.id = :tenantId " +
           "AND b.status = 'COMPLETED' AND b.startTime >= :startDate AND b.startTime < :endDate")
    List<Object[]> findCompletedRevenueRows(@Param("tenantId") Long tenantId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

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
