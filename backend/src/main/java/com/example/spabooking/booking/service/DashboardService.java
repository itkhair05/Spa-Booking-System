package com.example.spabooking.booking.service;

import com.example.spabooking.booking.dto.DashboardMetricsResponse;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spabooking.auth.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class DashboardService {

    private final BookingRepository bookingRepository;
    
    // Explicitly use Vietnam timezone per project requirements
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    public DashboardService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMetricsResponse getMetrics() {
        Long tenantId = TenantContext.requireTenantId();

        // Calculate time boundaries in Vietnam timezone
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDate todayDate = now.toLocalDate();
        LocalDateTime startOfToday = todayDate.atStartOfDay();
        LocalDateTime startOfTomorrow = todayDate.plusDays(1).atStartOfDay();

        Optional<CustomUserDetails> userDetailsOpt = getCurrentUserDetails();
        if (userDetailsOpt.isPresent() && userDetailsOpt.get().isStaff()) {
            CustomUserDetails userDetails = userDetailsOpt.get();
            Long linkedStaffId = userDetails.getStaffId();
            if (linkedStaffId != null) {
                long staffToday = bookingRepository.countTodayBookingsByStaff(tenantId, linkedStaffId, startOfToday, startOfTomorrow);
                long staffUpcoming = bookingRepository.countUpcomingBookingsByStaff(tenantId, linkedStaffId, now);
                long staffConfirmed = bookingRepository.countByTenantIdAndStaffIdAndStatus(tenantId, linkedStaffId, BookingStatus.CONFIRMED);
                // Staff only sees their assigned schedule; financial revenue is restricted to OWNER
                return new DashboardMetricsResponse(
                        staffToday,
                        staffUpcoming,
                        0L,
                        staffConfirmed,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );
            }
            return new DashboardMetricsResponse(0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // OWNER metrics for the entire facility
        long todayBookingCount = bookingRepository.countTodayBookings(tenantId, startOfToday, startOfTomorrow);
        long upcomingBookingCount = bookingRepository.countUpcomingBookings(tenantId, now);
        long pendingBookingCount = bookingRepository.countByTenantIdAndStatus(tenantId, BookingStatus.PENDING);
        long confirmedBookingCount = bookingRepository.countByTenantIdAndStatus(tenantId, BookingStatus.CONFIRMED);
        
        // Realized revenue: ONLY completed bookings contribute to completed revenue
        BigDecimal todayCompletedRevenue = bookingRepository.sumCompletedRevenue(tenantId, startOfToday, startOfTomorrow);

        // Expected revenue: from confirmed bookings scheduled for today
        BigDecimal todayExpectedRevenue = bookingRepository.sumExpectedRevenue(tenantId, startOfToday, startOfTomorrow);

        return new DashboardMetricsResponse(
                todayBookingCount,
                upcomingBookingCount,
                pendingBookingCount,
                confirmedBookingCount,
                todayExpectedRevenue,
                todayCompletedRevenue
        );
    }

    private Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }
}
