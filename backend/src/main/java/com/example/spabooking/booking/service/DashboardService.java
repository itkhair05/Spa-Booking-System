package com.example.spabooking.booking.service;

import com.example.spabooking.booking.dto.DashboardMetricsResponse;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

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

        // Calculate time boundaries
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDate todayDate = now.toLocalDate();
        LocalDateTime startOfToday = todayDate.atStartOfDay();
        LocalDateTime startOfTomorrow = todayDate.plusDays(1).atStartOfDay();

        long todayBookingCount = bookingRepository.countTodayBookings(tenantId, startOfToday, startOfTomorrow);
        long upcomingBookingCount = bookingRepository.countUpcomingBookings(tenantId, now);
        long pendingBookingCount = bookingRepository.countByTenantIdAndStatus(tenantId, BookingStatus.PENDING);
        long confirmedBookingCount = bookingRepository.countByTenantIdAndStatus(tenantId, BookingStatus.CONFIRMED);
        
        BigDecimal todayExpectedRevenue = bookingRepository.sumExpectedRevenue(
                tenantId, 
                startOfToday, 
                startOfTomorrow, 
                Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        return new DashboardMetricsResponse(
                todayBookingCount,
                upcomingBookingCount,
                pendingBookingCount,
                confirmedBookingCount,
                todayExpectedRevenue
        );
    }
}
