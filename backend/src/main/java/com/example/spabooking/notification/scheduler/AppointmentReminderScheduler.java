package com.example.spabooking.notification.scheduler;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.notification.config.EmailProperties;
import com.example.spabooking.notification.service.BookingNotificationService;
import com.example.spabooking.notification.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class AppointmentReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderScheduler.class);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingRepository bookingRepository;
    private final BookingNotificationService bookingNotificationService;
    private final EmailService emailService;
    private final EmailProperties emailProperties;

    @Autowired
    public AppointmentReminderScheduler(BookingRepository bookingRepository,
                                        BookingNotificationService bookingNotificationService,
                                        EmailService emailService,
                                        EmailProperties emailProperties) {
        this.bookingRepository = bookingRepository;
        this.bookingNotificationService = bookingNotificationService;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
    }

    @Scheduled(fixedDelayString = "${app.mail.reminder.fixed-delay-ms:120000}", initialDelayString = "${app.mail.reminder.initial-delay-ms:30000}")
    public void scheduledScan() {
        if (!emailProperties.isReminderSchedulerEnabled()) {
            log.debug("Appointment reminder background scheduler is disabled via config.");
            return;
        }
        sendUpcomingAppointmentReminders();
    }

    public void sendUpcomingAppointmentReminders() {
        if (!emailService.isConfigured()) {
            log.debug("Email service is not configured or disabled. Skipping appointment reminder scan.");
            return;
        }

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDateTime windowStart = now.plusMinutes(emailProperties.getReminderWindowStartMinutes());
        LocalDateTime windowEnd = now.plusMinutes(emailProperties.getReminderWindowEndMinutes());

        log.debug("Scanning for appointment reminders between {} and {}", windowStart, windowEnd);

        List<Booking> eligibleBookings = bookingRepository.findEligibleForReminder(
                BookingStatus.CONFIRMED, windowStart, windowEnd
        );

        if (eligibleBookings == null || eligibleBookings.isEmpty()) {
            return;
        }

        log.info("Found {} eligible booking(s) for appointment reminders", eligibleBookings.size());

        for (Booking booking : eligibleBookings) {
            try {
                bookingNotificationService.processAppointmentReminder(booking.getId());
            } catch (Exception ex) {
                log.error("Failed to process appointment reminder for booking {}: {}",
                        booking.getBookingCode(), ex.getMessage(), ex);
            }
        }
    }
}
