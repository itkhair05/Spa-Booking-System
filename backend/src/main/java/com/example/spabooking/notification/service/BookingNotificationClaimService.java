package com.example.spabooking.notification.service;

import com.example.spabooking.booking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BookingNotificationClaimService {

    private final BookingRepository bookingRepository;

    @Autowired
    public BookingNotificationClaimService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimConfirmationEmail(Long bookingId, LocalDateTime sentAt) {
        return bookingRepository.claimConfirmationEmail(bookingId, sentAt) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unclaimConfirmationEmail(Long bookingId) {
        bookingRepository.unclaimConfirmationEmail(bookingId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimAppointmentReminder(Long bookingId, LocalDateTime claimTime) {
        return bookingRepository.claimAppointmentReminder(bookingId, claimTime) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unclaimAppointmentReminder(Long bookingId) {
        bookingRepository.unclaimAppointmentReminder(bookingId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markReminderSkippedNoEmail(Long bookingId, LocalDateTime now) {
        bookingRepository.markReminderSkippedNoEmail(bookingId, now);
    }
}
