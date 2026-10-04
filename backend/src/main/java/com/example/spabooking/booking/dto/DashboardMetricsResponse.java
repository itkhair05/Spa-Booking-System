package com.example.spabooking.booking.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardMetricsResponse {
    private long todayBookingCount;
    private long upcomingBookingCount;
    private long pendingBookingCount;
    private long confirmedBookingCount;
    private BigDecimal todayExpectedRevenue;
    private BigDecimal todayCompletedRevenue;
    private BigDecimal totalCompletedRevenue;
    private List<BookingTrendPoint> bookingTrend = List.of();
    private List<BookingStatusCount> bookingStatusDistribution = List.of();
    private List<PopularServiceCount> popularServices = List.of();

    public DashboardMetricsResponse() {}

    public DashboardMetricsResponse(long todayBookingCount, long upcomingBookingCount,
                                    long pendingBookingCount, long confirmedBookingCount,
                                    BigDecimal todayExpectedRevenue) {
        this(todayBookingCount, upcomingBookingCount, pendingBookingCount, confirmedBookingCount, todayExpectedRevenue, BigDecimal.ZERO);
    }

    public DashboardMetricsResponse(long todayBookingCount, long upcomingBookingCount,
                                    long pendingBookingCount, long confirmedBookingCount,
                                    BigDecimal todayExpectedRevenue, BigDecimal todayCompletedRevenue) {
        this.todayBookingCount = todayBookingCount;
        this.upcomingBookingCount = upcomingBookingCount;
        this.pendingBookingCount = pendingBookingCount;
        this.confirmedBookingCount = confirmedBookingCount;
        this.todayExpectedRevenue = todayExpectedRevenue;
        this.todayCompletedRevenue = todayCompletedRevenue;
        this.totalCompletedRevenue = new BigDecimal("0.00");
        this.bookingTrend = List.of();
        this.bookingStatusDistribution = List.of();
        this.popularServices = List.of();
    }

    public long getTodayBookingCount() { return todayBookingCount; }
    public void setTodayBookingCount(long todayBookingCount) { this.todayBookingCount = todayBookingCount; }

    public long getUpcomingBookingCount() { return upcomingBookingCount; }
    public void setUpcomingBookingCount(long upcomingBookingCount) { this.upcomingBookingCount = upcomingBookingCount; }

    public long getPendingBookingCount() { return pendingBookingCount; }
    public void setPendingBookingCount(long pendingBookingCount) { this.pendingBookingCount = pendingBookingCount; }

    public long getConfirmedBookingCount() { return confirmedBookingCount; }
    public void setConfirmedBookingCount(long confirmedBookingCount) { this.confirmedBookingCount = confirmedBookingCount; }

    public BigDecimal getTodayExpectedRevenue() { return todayExpectedRevenue; }
    public void setTodayExpectedRevenue(BigDecimal todayExpectedRevenue) { this.todayExpectedRevenue = todayExpectedRevenue; }

    public BigDecimal getTodayCompletedRevenue() { return todayCompletedRevenue; }
    public void setTodayCompletedRevenue(BigDecimal todayCompletedRevenue) { this.todayCompletedRevenue = todayCompletedRevenue; }

    public BigDecimal getTotalCompletedRevenue() { return totalCompletedRevenue; }
    public void setTotalCompletedRevenue(BigDecimal totalCompletedRevenue) { this.totalCompletedRevenue = totalCompletedRevenue; }

    public List<BookingTrendPoint> getBookingTrend() { return bookingTrend; }
    public void setBookingTrend(List<BookingTrendPoint> bookingTrend) { this.bookingTrend = bookingTrend; }

    public List<BookingStatusCount> getBookingStatusDistribution() { return bookingStatusDistribution; }
    public void setBookingStatusDistribution(List<BookingStatusCount> bookingStatusDistribution) { this.bookingStatusDistribution = bookingStatusDistribution; }

    public List<PopularServiceCount> getPopularServices() { return popularServices; }
    public void setPopularServices(List<PopularServiceCount> popularServices) { this.popularServices = popularServices; }
}
