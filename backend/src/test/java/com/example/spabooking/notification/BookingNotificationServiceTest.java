package com.example.spabooking.notification;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.notification.service.BookingNotificationClaimService;
import com.example.spabooking.notification.service.BookingNotificationService;
import com.example.spabooking.notification.service.EmailService;
import com.example.spabooking.payment.repository.PaymentRepository;
import com.example.spabooking.tenant.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingNotificationServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private BookingNotificationClaimService claimService;

    private BookingNotificationService bookingNotificationService;

    @BeforeEach
    void setUp() {
        bookingNotificationService = new BookingNotificationService(
                bookingRepository, paymentRepository, emailService, claimService
        );
    }

    private Booking createBooking(BookingStatus status, String email) {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("TIKEY SPA");

        Customer customer = new Customer();
        customer.setId(2L);
        customer.setName("Nguyễn Văn A");
        customer.setEmail(email);

        Booking booking = new Booking();
        booking.setId(10L);
        booking.setTenant(tenant);
        booking.setCustomer(customer);
        booking.setStatus(status);
        booking.setBookingCode("BK-CONFIRM-1");
        booking.setStartTime(LocalDateTime.now().plusHours(2));
        booking.setIsReminded(false);

        return booking;
    }

    @Test
    @DisplayName("processBookingConfirmation skips if booking is not found")
    void testProcessBookingConfirmation_notFound() {
        when(bookingRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        bookingNotificationService.processBookingConfirmation(999L);

        verify(claimService, never()).claimConfirmationEmail(any(), any());
        verify(emailService, never()).sendBookingConfirmation(any(), any());
    }

    @Test
    @DisplayName("processBookingConfirmation skips if booking status is PENDING (unconfirmed)")
    void testProcessBookingConfirmation_pendingStatus() {
        Booking booking = createBooking(BookingStatus.PENDING, "test@example.com");
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));

        bookingNotificationService.processBookingConfirmation(10L);

        verify(claimService, never()).claimConfirmationEmail(any(), any());
        verify(emailService, never()).sendBookingConfirmation(any(), any());
    }

    @Test
    @DisplayName("processBookingConfirmation skips if booking status is CANCELLED")
    void testProcessBookingConfirmation_cancelledStatus() {
        Booking booking = createBooking(BookingStatus.CANCELLED, "test@example.com");
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));

        bookingNotificationService.processBookingConfirmation(10L);

        verify(claimService, never()).claimConfirmationEmail(any(), any());
        verify(emailService, never()).sendBookingConfirmation(any(), any());
    }

    @Test
    @DisplayName("processBookingConfirmation skips duplicate if confirmation_email_sent_at is already set")
    void testProcessBookingConfirmation_alreadySent() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, "test@example.com");
        booking.setConfirmationEmailSentAt(LocalDateTime.now().minusMinutes(5));
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));

        bookingNotificationService.processBookingConfirmation(10L);

        verify(claimService, never()).claimConfirmationEmail(any(), any());
        verify(emailService, never()).sendBookingConfirmation(any(), any());
    }

    @Test
    @DisplayName("processBookingConfirmation skips if customer email is missing")
    void testProcessBookingConfirmation_missingEmail() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, null);
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));

        bookingNotificationService.processBookingConfirmation(10L);

        verify(claimService, never()).claimConfirmationEmail(any(), any());
        verify(emailService, never()).sendBookingConfirmation(any(), any());
    }

    @Test
    @DisplayName("processBookingConfirmation skips when concurrent instance already claimed email")
    void testProcessBookingConfirmation_concurrentInstanceClaimed() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, "customer@example.com");
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));
        when(claimService.claimConfirmationEmail(eq(10L), any())).thenReturn(false);

        bookingNotificationService.processBookingConfirmation(10L);

        verify(emailService, never()).sendBookingConfirmation(any(), any());
    }

    @Test
    @DisplayName("processBookingConfirmation sends email outside locks on successful claim")
    void testProcessBookingConfirmation_success() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, "customer@example.com");
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));
        when(claimService.claimConfirmationEmail(eq(10L), any())).thenReturn(true);
        when(paymentRepository.findByBookingIdAndTenantId(10L, 1L)).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(emailService.sendBookingConfirmation(any(), any())).thenReturn(true);

        bookingNotificationService.processBookingConfirmation(10L);

        verify(emailService, times(1)).sendBookingConfirmation(eq(booking), any());
        verify(claimService, never()).unclaimConfirmationEmail(any());
    }

    @Test
    @DisplayName("processBookingConfirmation unclaims confirmation email when send fails (allows retry)")
    void testProcessBookingConfirmation_failureAllowsRetry() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, "customer@example.com");
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));
        when(claimService.claimConfirmationEmail(eq(10L), any())).thenReturn(true);
        when(paymentRepository.findByBookingIdAndTenantId(10L, 1L)).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(emailService.sendBookingConfirmation(any(), any())).thenReturn(false);

        bookingNotificationService.processBookingConfirmation(10L);

        verify(emailService, times(1)).sendBookingConfirmation(eq(booking), any());
        verify(claimService, times(1)).unclaimConfirmationEmail(10L);
    }

    @Test
    @DisplayName("processAppointmentReminder skips non-CONFIRMED bookings (CANCELLED/COMPLETED)")
    void testProcessAppointmentReminder_ineligibleStatus() {
        Booking cancelled = createBooking(BookingStatus.CANCELLED, "test@example.com");
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(cancelled));

        bookingNotificationService.processAppointmentReminder(10L);

        verify(claimService, never()).claimAppointmentReminder(any(), any());
        verify(emailService, never()).sendAppointmentReminder(any());

        Booking completed = createBooking(BookingStatus.COMPLETED, "test@example.com");
        when(bookingRepository.findByIdWithDetails(11L)).thenReturn(Optional.of(completed));

        bookingNotificationService.processAppointmentReminder(11L);

        verify(claimService, never()).claimAppointmentReminder(any(), any());
        verify(emailService, never()).sendAppointmentReminder(any());
    }

    @Test
    @DisplayName("processAppointmentReminder skips already reminded booking")
    void testProcessAppointmentReminder_alreadyReminded() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, "test@example.com");
        booking.setIsReminded(true);
        booking.setRemindedAt(LocalDateTime.now().minusMinutes(10));
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));

        bookingNotificationService.processAppointmentReminder(10L);

        verify(claimService, never()).claimAppointmentReminder(any(), any());
        verify(emailService, never()).sendAppointmentReminder(any());
    }

    @Test
    @DisplayName("processAppointmentReminder marks reminder skipped if email is missing")
    void testProcessAppointmentReminder_missingEmail() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, "");
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));

        bookingNotificationService.processAppointmentReminder(10L);

        verify(claimService, never()).claimAppointmentReminder(any(), any());
        verify(claimService, times(1)).markReminderSkippedNoEmail(eq(10L), any());
        verify(emailService, never()).sendAppointmentReminder(any());
    }

    @Test
    @DisplayName("processAppointmentReminder skips when concurrent instance already claimed reminder")
    void testProcessAppointmentReminder_concurrentInstanceClaimed() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, "test@example.com");
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));
        when(claimService.claimAppointmentReminder(eq(10L), any())).thenReturn(false);

        bookingNotificationService.processAppointmentReminder(10L);

        verify(emailService, never()).sendAppointmentReminder(any());
    }

    @Test
    @DisplayName("processAppointmentReminder sends reminder outside locks on successful claim")
    void testProcessAppointmentReminder_success() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, "test@example.com");
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));
        when(claimService.claimAppointmentReminder(eq(10L), any())).thenReturn(true);
        when(emailService.sendAppointmentReminder(booking)).thenReturn(true);

        bookingNotificationService.processAppointmentReminder(10L);

        verify(emailService, times(1)).sendAppointmentReminder(booking);
        verify(claimService, never()).unclaimAppointmentReminder(any());
    }

    @Test
    @DisplayName("processAppointmentReminder unclaims reminder when delivery fails (allows retry)")
    void testProcessAppointmentReminder_failureAllowsRetry() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, "test@example.com");
        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));
        when(claimService.claimAppointmentReminder(eq(10L), any())).thenReturn(true);
        when(emailService.sendAppointmentReminder(booking)).thenReturn(false);

        bookingNotificationService.processAppointmentReminder(10L);

        verify(emailService, times(1)).sendAppointmentReminder(booking);
        verify(claimService, times(1)).unclaimAppointmentReminder(10L);
    }
}
