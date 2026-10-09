package com.example.spabooking.notification.event;

public class BookingConfirmedEvent {

    private final Long bookingId;

    public BookingConfirmedEvent(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    @Override
    public String toString() {
        return "BookingConfirmedEvent{" +
                "bookingId=" + bookingId +
                '}';
    }
}
