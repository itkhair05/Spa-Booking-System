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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private EmailProperties emailProperties;
    private ObjectMapper objectMapper;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setEnabled(true);
        emailProperties.setResendApiKey("re_test_dummy_key_12345");
        emailProperties.setResendApiUrl("https://api.resend.com/emails");
        emailProperties.setTimeoutSeconds(10);
        emailProperties.setFromAddress("booking@tikeyspa.com");
        emailProperties.setFromName("TIKEY SPA");

        objectMapper = new ObjectMapper();
        emailService = new EmailService(emailProperties, objectMapper, httpClient);
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
    @DisplayName("isConfigured returns false when API key or fromAddress is missing or enabled is false")
    void testIsConfigured_propertiesValidation() {
        emailProperties.setResendApiKey("");
        assertFalse(emailService.isConfigured());

        emailProperties.setResendApiKey("re_test_key");
        emailProperties.setFromAddress("");
        assertFalse(emailService.isConfigured());

        emailProperties.setFromAddress("booking@tikeyspa.com");
        emailProperties.setEnabled(false);
        assertFalse(emailService.isConfigured());

        emailProperties.setEnabled(true);
        assertTrue(emailService.isConfigured());
    }

    @Test
    @DisplayName("sendBookingConfirmation skips and returns false when Resend is not configured")
    void testSendBookingConfirmation_notConfigured() throws Exception {
        emailProperties.setEnabled(false);
        Booking booking = createSampleBooking();

        boolean sent = emailService.sendBookingConfirmation(booking, null);

        assertFalse(sent);
        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("sendBookingConfirmation skips when customer email is missing or invalid")
    void testSendBookingConfirmation_invalidCustomerEmail() throws Exception {
        Booking booking = createSampleBooking();
        booking.getCustomer().setEmail("");

        boolean sent = emailService.sendBookingConfirmation(booking, null);

        assertFalse(sent);
        verify(httpClient, never()).send(any(HttpRequest.class), any());

        booking.getCustomer().setEmail("invalid-email-without-at");
        sent = emailService.sendBookingConfirmation(booking, null);

        assertFalse(sent);
        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("sendBookingConfirmation sends email successfully via Resend HTTPS API")
    void testSendBookingConfirmation_success() throws Exception {
        Booking booking = createSampleBooking();
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentMethod(PaymentMethod.VNPAY);

        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        boolean sent = emailService.sendBookingConfirmation(booking, payment);

        assertTrue(sent);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(1)).send(requestCaptor.capture(), any());

        HttpRequest sentRequest = requestCaptor.getValue();
        assertEquals("POST", sentRequest.method());
        assertEquals("https://api.resend.com/emails", sentRequest.uri().toString());
        assertTrue(sentRequest.headers().firstValue("Authorization").orElse("").contains("Bearer re_test_dummy_key_12345"));
        assertEquals("application/json", sentRequest.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    @DisplayName("sendBookingConfirmation handles non-2xx HTTP status from Resend API gracefully")
    void testSendBookingConfirmation_apiError() throws Exception {
        Booking booking = createSampleBooking();

        when(httpResponse.statusCode()).thenReturn(422);
        when(httpResponse.body()).thenReturn("{\"statusCode\":422,\"message\":\"Domain not verified\"}");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        boolean sent = emailService.sendBookingConfirmation(booking, null);

        assertFalse(sent);
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("sendBookingConfirmation handles HTTP timeout gracefully without throwing")
    void testSendBookingConfirmation_timeout() throws Exception {
        Booking booking = createSampleBooking();

        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new HttpTimeoutException("Connection timed out after 10 seconds"));

        boolean sent = emailService.sendBookingConfirmation(booking, null);

        assertFalse(sent);
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("sendBookingConfirmation handles IOException gracefully without throwing")
    void testSendBookingConfirmation_ioException() throws Exception {
        Booking booking = createSampleBooking();

        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("Network unreachable"));

        boolean sent = emailService.sendBookingConfirmation(booking, null);

        assertFalse(sent);
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("sendAppointmentReminder sends reminder successfully via Resend HTTPS API")
    void testSendAppointmentReminder_success() throws Exception {
        Booking booking = createSampleBooking();

        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        boolean sent = emailService.sendAppointmentReminder(booking);

        assertTrue(sent);
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("sendAppointmentReminder handles Resend API 500 error gracefully")
    void testSendAppointmentReminder_serverError() throws Exception {
        Booking booking = createSampleBooking();

        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("{\"message\":\"Internal server error\"}");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        boolean sent = emailService.sendAppointmentReminder(booking);

        assertFalse(sent);
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }
}
