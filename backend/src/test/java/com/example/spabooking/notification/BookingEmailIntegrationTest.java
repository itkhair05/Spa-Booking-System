package com.example.spabooking.notification;

import com.example.spabooking.booking.dto.CreateBookingRequest;
import com.example.spabooking.booking.dto.RescheduleBookingRequest;
import com.example.spabooking.booking.dto.UpdateBookingRequest;
import com.example.spabooking.booking.dto.UpdateBookingStatusRequest;
import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.booking.service.BookingService;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.notification.scheduler.AppointmentReminderScheduler;
import com.example.spabooking.notification.service.EmailService;
import com.example.spabooking.payment.config.VNPayConfig;
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.enums.PaymentMethod;
import com.example.spabooking.payment.enums.PaymentProvider;
import com.example.spabooking.payment.enums.PaymentStatus;
import com.example.spabooking.payment.repository.PaymentRepository;
import com.example.spabooking.payment.service.PaymentService;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.entity.ServiceCategory;
import com.example.spabooking.service.repository.ServiceCategoryRepository;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.mail.host=smtp.test.com",
        "spring.mail.port=587",
        "app.mail.enabled=true",
        "app.mail.from-address=noreply@tikeyspa.com",
        "app.mail.from-name=TIKEY SPA",
        "app.mail.reminder.scheduler-enabled=false"
})
class BookingEmailIntegrationTest {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AppointmentReminderScheduler reminderScheduler;

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.example.spabooking.notification.service.BookingNotificationService bookingNotificationService;

    @Autowired
    private VNPayConfig vnPayConfig;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Tenant tenant;
    private Staff staff;
    private Service service;
    private Customer customer;

    @BeforeEach
    void setUp() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        tenant = new Tenant();
        tenant.setName("TIKEY SPA Test");
        tenant.setSlug("tikey-email-test-" + System.currentTimeMillis());
        tenant.setIsActive(true);
        tenant = tenantRepository.save(tenant);

        TenantContext.setTenantId(tenant.getId());

        staff = new Staff();
        staff.setTenant(tenant);
        staff.setName("KTV Quynh");
        staff.setIsActive(true);
        staff.setIsDeleted(false);
        staff.setShowOnWebsite(true);
        staff = staffRepository.save(staff);

        ServiceCategory category = new ServiceCategory();
        category.setTenant(tenant);
        category.setName("Body Care");
        category = serviceCategoryRepository.save(category);

        service = new Service();
        service.setTenant(tenant);
        service.setCategory(category);
        service.setName("Massage Da Nang");
        service.setDurationMinutes(60);
        service.setPrice(new BigDecimal("400000.00"));
        service.setIsActive(true);
        service = serviceRepository.save(service);

        customer = new Customer();
        customer.setTenant(tenant);
        customer.setName("Lê Văn Nam");
        customer.setEmail("nam.le@example.com");
        customer.setPhone("0912345678");
        customer.setIsActive(true);
        customer = customerRepository.save(customer);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Created booking with PENDING status does NOT send confirmation email")
    void testCreatedBooking_noEmailSentWhenPending() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        Booking created = txTemplate.execute(status -> {
            CreateBookingRequest request = new CreateBookingRequest();
            request.setCustomerId(customer.getId());
            request.setServiceId(service.getId());
            request.setStaffId(staff.getId());
            request.setStartTime(LocalDateTime.now(VIETNAM_ZONE).plusDays(1).withHour(10).withMinute(0));
            request.setEndTime(request.getStartTime().plusMinutes(60));
            return bookingService.create(request);
        });

        assertNotNull(created);
        assertEquals(BookingStatus.PENDING, created.getStatus());
        assertNull(created.getConfirmationEmailSentAt());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Confirming booking via bookingService sends email and avoids duplicate on re-confirm")
    void testConfirmBooking_sendsEmailAndPreventsDuplicate() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        Booking booking = txTemplate.execute(status -> {
            CreateBookingRequest request = new CreateBookingRequest();
            request.setCustomerId(customer.getId());
            request.setServiceId(service.getId());
            request.setStaffId(staff.getId());
            request.setStartTime(LocalDateTime.now(VIETNAM_ZONE).plusDays(2).withHour(14).withMinute(0));
            request.setEndTime(request.getStartTime().plusMinutes(60));
            return bookingService.create(request);
        });

        assertNotNull(booking);

        // Confirm booking in transaction
        txTemplate.executeWithoutResult(status -> {
            bookingService.confirm(booking.getId());
        });

        // Verify email was sent and delivery state recorded
        verify(mailSender, times(1)).send(any(MimeMessage.class));

        Booking confirmed = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus());
        assertNotNull(confirmed.getConfirmationEmailSentAt());

        // Call confirm again (idempotent call)
        txTemplate.executeWithoutResult(status -> {
            bookingService.confirm(booking.getId());
        });

        // Ensure mail was NOT sent again
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("VNPay callback marks payment PAID, confirms booking, sends email, and ignores duplicate IPN")
    void testVNPayCallbackAndIpn_sendsEmailOnce() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        Booking booking = txTemplate.execute(status -> {
            CreateBookingRequest request = new CreateBookingRequest();
            request.setCustomerId(customer.getId());
            request.setServiceId(service.getId());
            request.setStaffId(staff.getId());
            request.setStartTime(LocalDateTime.now(VIETNAM_ZONE).plusDays(3).withHour(15).withMinute(0));
            request.setEndTime(request.getStartTime().plusMinutes(60));
            return bookingService.create(request);
        });

        assertNotNull(booking);

        Payment payment = txTemplate.execute(status ->
                paymentService.createPaymentForBooking(booking, PaymentMethod.VNPAY, PaymentProvider.VNPAY)
        );
        assertNotNull(payment);
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(BookingStatus.PENDING, booking.getStatus());

        // Prepare VNPay callback parameters
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_TxnRef", payment.getTxnRef());
        vnpParams.put("vnp_Amount", "40000000"); // 400,000 * 100
        vnpParams.put("vnp_ResponseCode", "00");
        vnpParams.put("vnp_TransactionNo", "12345678");
        vnpParams.put("vnp_BankCode", "NCB");
        vnpParams.put("vnp_CardType", "ATM");
        vnpParams.put("vnp_OrderInfo", "Thanh toan lich hen");
        vnpParams.put("vnp_PayDate", "20261010150000");

        String secureHash = calculateVNPayHash(vnpParams, vnPayConfig.getHashSecret());
        vnpParams.put("vnp_SecureHash", secureHash);

        // Process callback in transaction
        txTemplate.executeWithoutResult(status -> {
            paymentService.processVNPayCallback(vnpParams);
        });

        // Verify payment is PAID and booking is CONFIRMED
        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updatedPayment.getStatus());

        Booking confirmedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, confirmedBooking.getStatus());
        assertNotNull(confirmedBooking.getConfirmationEmailSentAt());

        // Email should have been sent once
        verify(mailSender, times(1)).send(any(MimeMessage.class));

        // Now simulate duplicate IPN arriving
        txTemplate.executeWithoutResult(status -> {
            paymentService.processVNPayIpn(vnpParams);
        });

        // Email should still only have been sent once!
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Reminder scheduler sends reminder to confirmed booking within 1h window and skips cancelled/completed")
    void testReminderScheduler_eligibleBookingsOnly() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        LocalDateTime nowInVn = LocalDateTime.now(VIETNAM_ZONE);

        // Booking 1: CONFIRMED, starts in 50 minutes (within 15-75m window) -> should receive reminder
        Booking eligibleBooking = txTemplate.execute(status -> {
            Booking b = new Booking();
            b.setTenant(tenant);
            b.setCustomer(customer);
            b.setStaff(staff);
            b.setService(service);
            b.setStatus(BookingStatus.CONFIRMED);
            b.setPrice(new BigDecimal("400000.00"));
            b.setBookingCode("BK-REMIND-" + System.currentTimeMillis());
            b.setStartTime(nowInVn.plusMinutes(50));
            b.setEndTime(nowInVn.plusMinutes(110));
            b.setIsReminded(false);
            return bookingRepository.save(b);
        });

        // Booking 2: CANCELLED, starts in 50 minutes -> should NOT receive reminder
        Booking cancelledBooking = txTemplate.execute(status -> {
            Booking b = new Booking();
            b.setTenant(tenant);
            b.setCustomer(customer);
            b.setStaff(staff);
            b.setService(service);
            b.setStatus(BookingStatus.CANCELLED);
            b.setPrice(new BigDecimal("400000.00"));
            b.setBookingCode("BK-CANCEL-" + System.currentTimeMillis());
            b.setStartTime(nowInVn.plusMinutes(50));
            b.setEndTime(nowInVn.plusMinutes(110));
            b.setIsReminded(false);
            return bookingRepository.save(b);
        });

        // Booking 3: COMPLETED, starts in 50 minutes -> should NOT receive reminder
        Booking completedBooking = txTemplate.execute(status -> {
            Booking b = new Booking();
            b.setTenant(tenant);
            b.setCustomer(customer);
            b.setStaff(staff);
            b.setService(service);
            b.setStatus(BookingStatus.COMPLETED);
            b.setPrice(new BigDecimal("400000.00"));
            b.setBookingCode("BK-COMPL-" + System.currentTimeMillis());
            b.setStartTime(nowInVn.plusMinutes(50));
            b.setEndTime(nowInVn.plusMinutes(110));
            b.setIsReminded(false);
            return bookingRepository.save(b);
        });

        // Reset mock invocation count from previous setup
        clearInvocations(mailSender);

        // Run scheduler
        reminderScheduler.sendUpcomingAppointmentReminders();

        // Verify email was sent only for eligible booking
        verify(mailSender, times(1)).send(any(MimeMessage.class));

        Booking refreshedEligible = bookingRepository.findById(eligibleBooking.getId()).orElseThrow();
        assertTrue(refreshedEligible.getIsReminded());
        assertNotNull(refreshedEligible.getRemindedAt());

        Booking refreshedCancelled = bookingRepository.findById(cancelledBooking.getId()).orElseThrow();
        assertFalse(refreshedCancelled.getIsReminded());

        Booking refreshedCompleted = bookingRepository.findById(completedBooking.getId()).orElseThrow();
        assertFalse(refreshedCompleted.getIsReminded());

        // Run scheduler a 2nd time -> verify no duplicate reminder is sent
        reminderScheduler.sendUpcomingAppointmentReminders();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Reminder scheduler retries on SMTP failure without premature mark")
    void testReminderScheduler_retryOnFailure() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        LocalDateTime nowInVn = LocalDateTime.now(VIETNAM_ZONE);

        Booking booking = txTemplate.execute(status -> {
            Booking b = new Booking();
            b.setTenant(tenant);
            b.setCustomer(customer);
            b.setStaff(staff);
            b.setService(service);
            b.setStatus(BookingStatus.CONFIRMED);
            b.setPrice(new BigDecimal("400000.00"));
            b.setBookingCode("BK-RETRY-" + System.currentTimeMillis());
            b.setStartTime(nowInVn.plusMinutes(55));
            b.setEndTime(nowInVn.plusMinutes(115));
            b.setIsReminded(false);
            return bookingRepository.save(b);
        });

        clearInvocations(mailSender);

        // Simulate SMTP failure on first try
        doThrow(new MailSendException("SMTP temporary network issue")).when(mailSender).send(any(MimeMessage.class));

        reminderScheduler.sendUpcomingAppointmentReminders();

        Booking check1 = bookingRepository.findById(booking.getId()).orElseThrow();
        assertFalse(check1.getIsReminded(), "isReminded must NOT be set prematurely on SMTP failure");
        assertNull(check1.getRemindedAt());

        // On second run, SMTP recovers
        doNothing().when(mailSender).send(any(MimeMessage.class));

        reminderScheduler.sendUpcomingAppointmentReminders();

        Booking check2 = bookingRepository.findById(booking.getId()).orElseThrow();
        assertTrue(check2.getIsReminded(), "Retry must succeed and set isReminded=true");
        assertNotNull(check2.getRemindedAt());
    }

    @Test
    @DisplayName("Updating booking status to CONFIRMED via updateStatus sends confirmation email")
    void testUpdateStatusConfirmed_sendsEmail() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        Booking booking = txTemplate.execute(status -> {
            CreateBookingRequest request = new CreateBookingRequest();
            request.setCustomerId(customer.getId());
            request.setServiceId(service.getId());
            request.setStaffId(staff.getId());
            request.setStartTime(LocalDateTime.now(VIETNAM_ZONE).plusDays(4).withHour(11).withMinute(0));
            request.setEndTime(request.getStartTime().plusMinutes(60));
            return bookingService.create(request);
        });

        assertNotNull(booking);
        clearInvocations(mailSender);

        txTemplate.executeWithoutResult(status -> {
            UpdateBookingStatusRequest statusReq = new UpdateBookingStatusRequest();
            statusReq.setStatus(BookingStatus.CONFIRMED);
            bookingService.updateStatus(booking.getId(), statusReq);
        });

        verify(mailSender, times(1)).send(any(MimeMessage.class));

        Booking confirmed = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus());
        assertNotNull(confirmed.getConfirmationEmailSentAt());
    }

    @Test
    @DisplayName("Rescheduling or updating appointment time resets isReminded and remindedAt")
    void testRescheduleAndUpdate_resetsReminderState() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        LocalDateTime baseTime = LocalDateTime.now(VIETNAM_ZONE).plusDays(5).withHour(9).withMinute(0);

        Booking booking = txTemplate.execute(status -> {
            Booking b = new Booking();
            b.setTenant(tenant);
            b.setCustomer(customer);
            b.setStaff(staff);
            b.setService(service);
            b.setStatus(BookingStatus.CONFIRMED);
            b.setPrice(new BigDecimal("400000.00"));
            b.setBookingCode("BK-RESET-" + System.currentTimeMillis());
            b.setStartTime(baseTime);
            b.setEndTime(baseTime.plusMinutes(60));
            b.setIsReminded(true);
            b.setRemindedAt(LocalDateTime.now(VIETNAM_ZONE).minusHours(1));
            return bookingRepository.save(b);
        });

        assertNotNull(booking);
        assertTrue(booking.getIsReminded());
        assertNotNull(booking.getRemindedAt());

        // Reschedule to a new start time
        txTemplate.executeWithoutResult(status -> {
            RescheduleBookingRequest req = new RescheduleBookingRequest();
            req.setStartTime(baseTime.plusHours(2));
            req.setStaffId(staff.getId());
            bookingService.reschedule(booking.getId(), req);
        });

        Booking rescheduled = bookingRepository.findById(booking.getId()).orElseThrow();
        assertFalse(rescheduled.getIsReminded(), "isReminded must be reset to false after reschedule");
        assertNull(rescheduled.getRemindedAt(), "remindedAt must be reset to null after reschedule");

        // Simulate reminded again
        txTemplate.executeWithoutResult(status -> {
            rescheduled.setIsReminded(true);
            rescheduled.setRemindedAt(LocalDateTime.now(VIETNAM_ZONE));
            bookingRepository.save(rescheduled);
        });

        // Update booking time via update method
        txTemplate.executeWithoutResult(status -> {
            UpdateBookingRequest updateReq = new UpdateBookingRequest();
            updateReq.setCustomerId(customer.getId());
            updateReq.setStaffId(staff.getId());
            updateReq.setServiceId(service.getId());
            updateReq.setStartTime(baseTime.plusHours(4));
            bookingService.update(booking.getId(), updateReq);
        });

        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertFalse(updated.getIsReminded(), "isReminded must be reset to false after time update");
        assertNull(updated.getRemindedAt(), "remindedAt must be reset to null after time update");

        // Simulate reminded again
        txTemplate.executeWithoutResult(status -> {
            updated.setIsReminded(true);
            updated.setRemindedAt(LocalDateTime.now(VIETNAM_ZONE));
            bookingRepository.save(updated);
        });

        // Reschedule with SAME start time -> isReminded must REMAIN true (not reset)
        txTemplate.executeWithoutResult(status -> {
            RescheduleBookingRequest req = new RescheduleBookingRequest();
            req.setStartTime(updated.getStartTime());
            req.setStaffId(staff.getId());
            bookingService.reschedule(booking.getId(), req);
        });

        Booking sameTimeRescheduled = bookingRepository.findById(booking.getId()).orElseThrow();
        assertTrue(sameTimeRescheduled.getIsReminded(), "isReminded must remain true when start time is unchanged in reschedule");
        assertNotNull(sameTimeRescheduled.getRemindedAt());

        // Update with SAME start time -> isReminded must REMAIN true (not reset)
        txTemplate.executeWithoutResult(status -> {
            UpdateBookingRequest updateReq = new UpdateBookingRequest();
            updateReq.setCustomerId(customer.getId());
            updateReq.setStaffId(staff.getId());
            updateReq.setServiceId(service.getId());
            updateReq.setStartTime(sameTimeRescheduled.getStartTime());
            bookingService.update(booking.getId(), updateReq);
        });

        Booking sameTimeUpdated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertTrue(sameTimeUpdated.getIsReminded(), "isReminded must remain true when start time is unchanged in update");
        assertNotNull(sameTimeUpdated.getRemindedAt());
    }

    @Test
    @DisplayName("Database-backed claim prevents duplicate reminder emails across concurrent calls")
    void testConcurrentReminderClaim_preventsDuplicateEmail() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        LocalDateTime nowInVn = LocalDateTime.now(VIETNAM_ZONE);

        Booking booking = txTemplate.execute(status -> {
            Booking b = new Booking();
            b.setTenant(tenant);
            b.setCustomer(customer);
            b.setStaff(staff);
            b.setService(service);
            b.setStatus(BookingStatus.CONFIRMED);
            b.setPrice(new BigDecimal("400000.00"));
            b.setBookingCode("BK-CONCUR-" + System.currentTimeMillis());
            b.setStartTime(nowInVn.plusMinutes(45));
            b.setEndTime(nowInVn.plusMinutes(105));
            b.setIsReminded(false);
            return bookingRepository.save(b);
        });

        assertNotNull(booking);
        clearInvocations(mailSender);

        // Instance 1 processes reminder
        bookingNotificationService.processAppointmentReminder(booking.getId());

        // Verify sent once
        verify(mailSender, times(1)).send(any(MimeMessage.class));

        Booking afterFirst = bookingRepository.findById(booking.getId()).orElseThrow();
        assertTrue(afterFirst.getIsReminded());
        assertNotNull(afterFirst.getRemindedAt());

        // Instance 2 (or second concurrent run) processes the same booking
        bookingNotificationService.processAppointmentReminder(booking.getId());

        // Verify mailSender was NOT called a second time
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    private String calculateVNPayHash(Map<String, String> fields, String secret) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        try {
            for (String fieldName : fieldNames) {
                String fieldValue = fields.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                }
            }
            javax.crypto.Mac hmac512 = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(hashData.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
