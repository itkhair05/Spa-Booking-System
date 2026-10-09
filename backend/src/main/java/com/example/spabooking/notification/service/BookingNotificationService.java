package com.example.spabooking.notification.service;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BookingNotificationService {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationService.class);

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    private final BookingNotificationClaimService claimService;

    // Concurrent in-flight guards to prevent duplicate emails within the same JVM
    private final Set<Long> inFlightConfirmations = ConcurrentHashMap.newKeySet();
    private final Set<Long> inFlightReminders = ConcurrentHashMap.newKeySet();

    @Autowired
    public BookingNotificationService(BookingRepository bookingRepository,
                                      PaymentRepository paymentRepository,
                                      EmailService emailService,
                                      BookingNotificationClaimService claimService) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
        this.claimService = claimService;
    }

    public void processBookingConfirmation(Long bookingId) {
        if (bookingId == null) {
            return;
        }

        if (!inFlightConfirmations.add(bookingId)) {
            log.debug("Confirmation email already in-flight for booking id {}", bookingId);
            return;
        }

        boolean claimed = false;
        try {
            Booking booking = bookingRepository.findByIdWithDetails(bookingId).orElse(null);
            if (booking == null) {
                log.warn("Booking with id {} not found for confirmation email", bookingId);
                return;
            }

            // Only CONFIRMED bookings are eligible
            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                log.info("Booking {} (status={}) is not eligible for confirmation email (must be CONFIRMED)",
                        booking.getBookingCode(), booking.getStatus());
                return;
            }

            // Duplicate check
            if (booking.getConfirmationEmailSentAt() != null) {
                log.info("Confirmation email already sent at {} for booking {}, skipping duplicate",
                        booking.getConfirmationEmailSentAt(), booking.getBookingCode());
                return;
            }

            if (booking.getCustomer() == null || booking.getCustomer().getEmail() == null || booking.getCustomer().getEmail().isBlank()) {
                log.info("Customer email is missing for booking {}, skipping confirmation email", booking.getBookingCode());
                return;
            }

            // Atomic database claim: prevents multi-instance concurrency without holding lock during SMTP
            claimed = claimService.claimConfirmationEmail(bookingId, LocalDateTime.now());
            if (!claimed) {
                log.info("Confirmation email for booking {} was already claimed/sent by another instance, skipping",
                        booking.getBookingCode());
                return;
            }

            Payment payment = null;
            if (booking.getTenant() != null) {
                payment = paymentRepository.findByBookingIdAndTenantId(booking.getId(), booking.getTenant().getId()).orElse(null);
            }
            if (payment == null) {
                payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
            }

            // SMTP sending executes completely outside any open DB lock or transaction
            boolean sent = emailService.sendBookingConfirmation(booking, payment);
            if (sent) {
                log.info("Successfully sent confirmation email for booking {}", booking.getBookingCode());
            } else {
                log.warn("Confirmation email was not delivered for booking {}. Reverting claim for future retry.",
                        booking.getBookingCode());
                claimService.unclaimConfirmationEmail(bookingId);
                claimed = false;
            }
        } catch (Exception ex) {
            log.error("Error processing booking confirmation notification for booking id {}: {}", bookingId, ex.getMessage(), ex);
            if (claimed) {
                try {
                    claimService.unclaimConfirmationEmail(bookingId);
                } catch (Exception rollbackEx) {
                    log.error("Failed to unclaim confirmation email for booking id {}: {}", bookingId, rollbackEx.getMessage());
                }
            }
        } finally {
            inFlightConfirmations.remove(bookingId);
        }
    }

    public void processAppointmentReminder(Long bookingId) {
        if (bookingId == null) {
            return;
        }

        if (!inFlightReminders.add(bookingId)) {
            log.debug("Appointment reminder already in-flight for booking id {}", bookingId);
            return;
        }

        boolean claimed = false;
        try {
            Booking booking = bookingRepository.findByIdWithDetails(bookingId).orElse(null);
            if (booking == null) {
                log.warn("Booking with id {} not found for appointment reminder", bookingId);
                return;
            }

            // Only CONFIRMED bookings are eligible (exclude CANCELLED, COMPLETED, NO_SHOW, etc.)
            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                log.debug("Booking {} (status={}) is not eligible for reminder email",
                        booking.getBookingCode(), booking.getStatus());
                return;
            }

            // Duplicate check
            if (Boolean.TRUE.equals(booking.getIsReminded()) || booking.getRemindedAt() != null) {
                log.debug("Reminder already sent for booking {}, skipping duplicate", booking.getBookingCode());
                return;
            }

            if (booking.getCustomer() == null || booking.getCustomer().getEmail() == null || booking.getCustomer().getEmail().isBlank()) {
                log.info("Customer email is missing for booking {}, marking isReminded=true to prevent infinite re-polling",
                        booking.getBookingCode());
                claimService.markReminderSkippedNoEmail(bookingId, LocalDateTime.now());
                return;
            }

            // Atomic database claim: prevents multi-instance concurrency without holding lock during SMTP
            claimed = claimService.claimAppointmentReminder(bookingId, LocalDateTime.now());
            if (!claimed) {
                log.info("Appointment reminder for booking {} was already claimed/sent by another instance, skipping",
                        booking.getBookingCode());
                return;
            }

            // SMTP sending executes completely outside any open DB lock or transaction
            boolean sent = emailService.sendAppointmentReminder(booking);
            if (sent) {
                log.info("Successfully sent appointment reminder for booking {}", booking.getBookingCode());
            } else {
                log.warn("Reminder email was not delivered for booking {}. Reverting claim for retry.",
                        booking.getBookingCode());
                claimService.unclaimAppointmentReminder(bookingId);
                claimed = false;
            }
        } catch (Exception ex) {
            log.error("Error processing appointment reminder for booking id {}: {}", bookingId, ex.getMessage(), ex);
            if (claimed) {
                try {
                    claimService.unclaimAppointmentReminder(bookingId);
                } catch (Exception rollbackEx) {
                    log.error("Failed to unclaim appointment reminder for booking id {}: {}", bookingId, rollbackEx.getMessage());
                }
            }
        } finally {
            inFlightReminders.remove(bookingId);
        }
    }
}
