package com.example.spabooking.booking.dto;

public class PopularServiceCount {

    private String serviceName;
    private long bookingCount;

    public PopularServiceCount() {}

    public PopularServiceCount(String serviceName, long bookingCount) {
        this.serviceName = serviceName;
        this.bookingCount = bookingCount;
    }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public long getBookingCount() { return bookingCount; }
    public void setBookingCount(long bookingCount) { this.bookingCount = bookingCount; }
}
