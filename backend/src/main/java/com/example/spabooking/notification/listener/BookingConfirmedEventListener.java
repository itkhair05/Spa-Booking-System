package com.example.spabooking.notification.listener;

import com.example.spabooking.notification.event.BookingConfirmedEvent;
import com.example.spabooking.notification.service.BookingNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingConfirmedEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingConfirmedEventListener.class);

    private final BookingNotificationService bookingNotificationService;

    @Autowired
    public BookingConfirmedEventListener(BookingNotificationService bookingNotificationService) {
        this.bookingNotificationService = bookingNotificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        if (event == null || event.getBookingId() == null) {
            return;
        }
        log.debug("Received BookingConfirmedEvent for booking id {}", event.getBookingId());
        bookingNotificationService.processBookingConfirmation(event.getBookingId());
    }
}
