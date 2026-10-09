package com.example.spabooking.notification;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.notification.config.EmailProperties;
import com.example.spabooking.notification.service.EmailService;
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.enums.PaymentMethod;
import com.example.spabooking.payment.enums.PaymentStatus;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.tenant.entity.Tenant;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailProperties emailProperties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setEnabled(true);
        emailProperties.setHost("smtp.example.com");
        emailProperties.setPort(587);
        emailProperties.setFromAddress("contact@tikeyspa.com");
        emailProperties.setFromName("TIKEY SPA");

        emailService = new EmailService(mailSender, emailProperties);
    }

    private Booking createSampleBooking() {
        Tenant tenant = new Tenant();
        tenant.setName("TIKEY SPA Đà Nẵng");
        tenant.setAddress("123 Nguyễn Văn Linh, Đà Nẵng");
        tenant.setPhone("0905123456");

        Customer customer = new Customer();
        customer.setName("Trần Thị Hoa");
        customer.setEmail("hoa.tran@example.com");
        customer.setPhone("0988112233");

        Service service = new Service();
        service.setName("Massage Cổ Vai Gáy");
        service.setDurationMinutes(60);
        service.setPrice(new BigDecimal("350000.00"));

        Staff staff = new Staff();
        staff.setName("KTV Mai");

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setTenant(tenant);
        booking.setCustomer(customer);
        booking.setService(service);
        booking.setStaff(staff);
        booking.setBookingCode("BK-TEST12345");
        booking.setStartTime(LocalDateTime.of(2026, 10, 10, 14, 30));
        booking.setEndTime(LocalDateTime.of(2026, 10, 10, 15, 30));
        booking.setPrice(new BigDecimal("350000.00"));

        return booking;
    }

    @Test
    @DisplayName("isConfigured returns false when mailSender is null")
    void testIsConfigured_nullMailSender() {
        EmailService service = new EmailService(null, emailProperties);
        assertFalse(service.isConfigured());
    }

    @Test
    @DisplayName("isConfigured returns false when host or fromAddress is missing or enabled is false")
    void testIsConfigured_missingProperties() {
        emailProperties.setHost("");
        assertFalse(emailService.isConfigured());

        emailProperties.setHost("smtp.example.com");
        emailProperties.setFromAddress("");
        assertFalse(emailService.isConfigured());

        emailProperties.setFromAddress("contact@tikeyspa.com");
        emailProperties.setEnabled(false);
        assertFalse(emailService.isConfigured());

        emailProperties.setEnabled(true);
        assertTrue(emailService.isConfigured());
    }

    @Test
    @DisplayName("sendBookingConfirmation skips and returns false when SMTP is not configured")
    void testSendBookingConfirmation_notConfigured() {
        emailProperties.setEnabled(false);
        Booking booking = createSampleBooking();

        boolean sent = emailService.sendBookingConfirmation(booking, null);

        assertFalse(sent);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendBookingConfirmation skips when customer email is missing or invalid")
    void testSendBookingConfirmation_invalidCustomerEmail() {
        Booking booking = createSampleBooking();
        booking.getCustomer().setEmail("");

        boolean sent = emailService.sendBookingConfirmation(booking, null);

        assertFalse(sent);
        verify(mailSender, never()).send(any(MimeMessage.class));

        booking.getCustomer().setEmail("invalid-email-no-at");
        sent = emailService.sendBookingConfirmation(booking, null);

        assertFalse(sent);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendBookingConfirmation sends email successfully with Vietnamese content")
    void testSendBookingConfirmation_success() {
        Booking booking = createSampleBooking();
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentMethod(PaymentMethod.VNPAY);

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        boolean sent = emailService.sendBookingConfirmation(booking, payment);

        assertTrue(sent);
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("sendBookingConfirmation handles SMTP exception gracefully without rethrowing")
    void testSendBookingConfirmation_smtpFailure() {
        Booking booking = createSampleBooking();

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP server connection timeout")).when(mailSender).send(mimeMessage);

        boolean sent = emailService.sendBookingConfirmation(booking, null);

        assertFalse(sent);
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("sendAppointmentReminder sends reminder successfully")
    void testSendAppointmentReminder_success() {
        Booking booking = createSampleBooking();

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        boolean sent = emailService.sendAppointmentReminder(booking);

        assertTrue(sent);
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("sendAppointmentReminder handles SMTP failure gracefully")
    void testSendAppointmentReminder_smtpFailure() {
        Booking booking = createSampleBooking();

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(mimeMessage);

        boolean sent = emailService.sendAppointmentReminder(booking);

        assertFalse(sent);
        verify(mailSender, times(1)).send(mimeMessage);
    }
}
