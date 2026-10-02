package com.example.spabooking.booking.dto;

import java.math.BigDecimal;

public class DashboardMetricsResponse {
    private long todayBookingCount;
    private long upcomingBookingCount;
    private long pendingBookingCount;
    private long confirmedBookingCount;
    private BigDecimal todayExpectedRevenue;

    public DashboardMetricsResponse() {}

    public DashboardMetricsResponse(long todayBookingCount, long upcomingBookingCount,
                                    long pendingBookingCount, long confirmedBookingCount,
                                    BigDecimal todayExpectedRevenue) {
        this.todayBookingCount = todayBookingCount;
        this.upcomingBookingCount = upcomingBookingCount;
        this.pendingBookingCount = pendingBookingCount;
        this.confirmedBookingCount = confirmedBookingCount;
        this.todayExpectedRevenue = todayExpectedRevenue;
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
}
