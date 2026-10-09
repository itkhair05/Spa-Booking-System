package com.example.spabooking.notification;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.notification.config.EmailProperties;
import com.example.spabooking.notification.scheduler.AppointmentReminderScheduler;
import com.example.spabooking.notification.service.BookingNotificationService;
import com.example.spabooking.notification.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingNotificationService bookingNotificationService;

    @Mock
    private EmailService emailService;

    private EmailProperties emailProperties;
    private AppointmentReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setReminderWindowStartMinutes(15);
        emailProperties.setReminderWindowEndMinutes(75);

        scheduler = new AppointmentReminderScheduler(
                bookingRepository, bookingNotificationService, emailService, emailProperties
        );
    }

    @Test
    @DisplayName("Scheduler skips scan when email service is not configured")
    void testScheduler_skippedWhenNotConfigured() {
        when(emailService.isConfigured()).thenReturn(false);

        scheduler.sendUpcomingAppointmentReminders();

        verify(bookingRepository, never()).findEligibleForReminder(any(), any(), any());
        verify(bookingNotificationService, never()).processAppointmentReminder(any());
    }

    @Test
    @DisplayName("Scheduler queries eligible bookings and triggers reminders when configured")
    void testScheduler_processesEligibleBookings() {
        when(emailService.isConfigured()).thenReturn(true);

        Booking b1 = new Booking();
        b1.setId(101L);
        b1.setBookingCode("BK-101");

        Booking b2 = new Booking();
        b2.setId(102L);
        b2.setBookingCode("BK-102");

        when(bookingRepository.findEligibleForReminder(eq(BookingStatus.CONFIRMED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(b1, b2));

        scheduler.sendUpcomingAppointmentReminders();

        verify(bookingNotificationService, times(1)).processAppointmentReminder(101L);
        verify(bookingNotificationService, times(1)).processAppointmentReminder(102L);
    }

    @Test
    @DisplayName("Scheduler continues processing remaining bookings even if one throws exception")
    void testScheduler_exceptionIsolation() {
        when(emailService.isConfigured()).thenReturn(true);

        Booking b1 = new Booking();
        b1.setId(201L);
        b1.setBookingCode("BK-201");

        Booking b2 = new Booking();
        b2.setId(202L);
        b2.setBookingCode("BK-202");

        when(bookingRepository.findEligibleForReminder(eq(BookingStatus.CONFIRMED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(b1, b2));

        doThrow(new RuntimeException("DB glitch")).when(bookingNotificationService).processAppointmentReminder(201L);

        scheduler.sendUpcomingAppointmentReminders();

        verify(bookingNotificationService, times(1)).processAppointmentReminder(201L);
        verify(bookingNotificationService, times(1)).processAppointmentReminder(202L);
    }
}
