package com.example.spabooking.notification.service;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.notification.config.EmailProperties;
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.enums.PaymentStatus;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VIETNAMESE_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm - 'ngày' dd/MM/yyyy").withZone(VIETNAM_ZONE);

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Autowired
    public EmailService(@Autowired(required = false) JavaMailSender mailSender,
                        EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    public boolean isConfigured() {
        return mailSender != null && emailProperties != null && emailProperties.isConfigured();
    }

    public boolean sendBookingConfirmation(Booking booking, Payment payment) {
        if (!isConfigured()) {
            log.warn("SMTP email service is not configured. Skipping confirmation email for booking code: {}",
                    booking != null ? booking.getBookingCode() : "UNKNOWN");
            return false;
        }

        if (booking == null || booking.getCustomer() == null) {
            log.warn("Booking or customer data is missing. Cannot send confirmation email.");
            return false;
        }

        String recipientEmail = booking.getCustomer().getEmail();
        if (recipientEmail == null || recipientEmail.trim().isEmpty() || !recipientEmail.contains("@")) {
            log.warn("Customer email is missing or invalid for booking code: {}. Skipping email delivery.",
                    booking.getBookingCode());
            return false;
        }

        String bookingCode = booking.getBookingCode();
        String customerName = booking.getCustomer().getName() != null ? booking.getCustomer().getName() : "Quý khách";
        String serviceName = booking.getService() != null ? booking.getService().getName() : "Dịch vụ Spa";
        int durationMinutes = booking.getService() != null ? booking.getService().getDurationMinutes() : 60;
        String staffName = booking.getStaff() != null ? booking.getStaff().getName() : "TIKEY SPA sẽ phân công KTV";
        String formattedTime = booking.getStartTime() != null ? booking.getStartTime().format(VIETNAMESE_DATE_TIME_FORMAT) : "Chưa xác định";
        String formattedPrice = formatCurrency(booking.getPrice());

        String paymentStatusText = resolvePaymentStatusText(payment);
        String spaName = (booking.getTenant() != null && booking.getTenant().getName() != null)
                ? booking.getTenant().getName() : "TIKEY SPA";
        String spaAddress = booking.getTenant() != null ? booking.getTenant().getAddress() : null;
        String spaPhone = booking.getTenant() != null ? booking.getTenant().getPhone() : null;

        String subject = "[" + spaName + "] Xác nhận đặt lịch thành công - Mã đặt lịch: " + bookingCode;

        String textContent = buildConfirmationTextContent(
                spaName, customerName, bookingCode, serviceName, durationMinutes,
                formattedTime, staffName, formattedPrice, paymentStatusText, spaAddress, spaPhone
        );

        String htmlContent = buildConfirmationHtmlContent(
                spaName, customerName, bookingCode, serviceName, durationMinutes,
                formattedTime, staffName, formattedPrice, paymentStatusText, spaAddress, spaPhone
        );

        return sendMimeEmail(recipientEmail, customerName, subject, textContent, htmlContent, bookingCode, "Confirmation");
    }

    public boolean sendAppointmentReminder(Booking booking) {
        if (!isConfigured()) {
            log.warn("SMTP email service is not configured. Skipping appointment reminder for booking code: {}",
                    booking != null ? booking.getBookingCode() : "UNKNOWN");
            return false;
        }

        if (booking == null || booking.getCustomer() == null) {
            log.warn("Booking or customer data is missing. Cannot send appointment reminder.");
            return false;
        }

        String recipientEmail = booking.getCustomer().getEmail();
        if (recipientEmail == null || recipientEmail.trim().isEmpty() || !recipientEmail.contains("@")) {
            log.warn("Customer email is missing or invalid for booking code: {}. Skipping reminder delivery.",
                    booking.getBookingCode());
            return false;
        }

        String bookingCode = booking.getBookingCode();
        String customerName = booking.getCustomer().getName() != null ? booking.getCustomer().getName() : "Quý khách";
        String serviceName = booking.getService() != null ? booking.getService().getName() : "Dịch vụ Spa";
        String staffName = booking.getStaff() != null ? booking.getStaff().getName() : "TIKEY SPA sẽ phân công KTV";
        String formattedTime = booking.getStartTime() != null ? booking.getStartTime().format(VIETNAMESE_DATE_TIME_FORMAT) : "Chưa xác định";

        String spaName = (booking.getTenant() != null && booking.getTenant().getName() != null)
                ? booking.getTenant().getName() : "TIKEY SPA";
        String spaAddress = booking.getTenant() != null ? booking.getTenant().getAddress() : null;
        String spaPhone = booking.getTenant() != null ? booking.getTenant().getPhone() : null;

        String subject = "[" + spaName + "] Nhắc hẹn - Lịch hẹn của bạn sẽ bắt đầu trong 1 giờ tới (" + bookingCode + ")";

        String textContent = buildReminderTextContent(
                spaName, customerName, bookingCode, serviceName, formattedTime, staffName, spaAddress, spaPhone
        );

        String htmlContent = buildReminderHtmlContent(
                spaName, customerName, bookingCode, serviceName, formattedTime, staffName, spaAddress, spaPhone
        );

        return sendMimeEmail(recipientEmail, customerName, subject, textContent, htmlContent, bookingCode, "Reminder");
    }

    private boolean sendMimeEmail(String toEmail, String toName, String subject,
                                  String textContent, String htmlContent,
                                  String bookingCode, String emailType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            try {
                helper.setFrom(new InternetAddress(emailProperties.getFromAddress(), emailProperties.getFromName(), StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(emailProperties.getFromAddress());
            }

            try {
                helper.setTo(new InternetAddress(toEmail, toName, StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                helper.setTo(toEmail);
            }

            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);

            mailSender.send(message);
            log.info("Successfully sent {} email to recipient for booking code: {}", emailType, bookingCode);
            return true;
        } catch (MailException ex) {
            log.error("SMTP error sending {} email for booking code {}: {}", emailType, bookingCode, ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.error("Unexpected error sending {} email for booking code {}: {}", emailType, bookingCode, ex.getMessage());
            return false;
        }
    }

    private String resolvePaymentStatusText(Payment payment) {
        if (payment == null) {
            return "Thanh toán tại quầy khi sử dụng dịch vụ";
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            return "Đã thanh toán (" + payment.getPaymentMethod().name() + ")";
        }
        if (payment.getStatus() == PaymentStatus.PENDING) {
            return "Chờ thanh toán (" + payment.getPaymentMethod().name() + ")";
        }
        return "Thanh toán tại quầy khi sử dụng dịch vụ";
    }

    private String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        return currencyFormat.format(amount) + " VNĐ";
    }

    private String buildConfirmationTextContent(String spaName, String customerName, String bookingCode,
                                                String serviceName, int durationMinutes, String formattedTime,
                                                String staffName, String formattedPrice, String paymentStatusText,
                                                String spaAddress, String spaPhone) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kính chào ").append(customerName).append(",\n\n");
        sb.append("Cảm ơn Quý khách đã đặt lịch dịch vụ tại ").append(spaName).append(".\n");
        sb.append("Lịch hẹn của Quý khách đã được xác nhận thành công với thông tin chi tiết như sau:\n\n");
        sb.append("- Mã đặt lịch: ").append(bookingCode).append("\n");
        sb.append("- Dịch vụ: ").append(serviceName).append(" (").append(durationMinutes).append(" phút)\n");
        sb.append("- Thời gian hẹn: ").append(formattedTime).append("\n");
        sb.append("- Kỹ thuật viên: ").append(staffName).append("\n");
        sb.append("- Tổng chi phí: ").append(formattedPrice).append("\n");
        sb.append("- Trạng thái thanh toán: ").append(paymentStatusText).append("\n");
        if (spaAddress != null && !spaAddress.isBlank()) {
            sb.append("- Địa chỉ spa: ").append(spaAddress).append("\n");
        }
        if (spaPhone != null && !spaPhone.isBlank()) {
            sb.append("- Hotline hỗ trợ: ").append(spaPhone).append("\n");
        }
        sb.append("\nLưu ý: Quý khách vui lòng đến trước giờ hẹn 10-15 phút để được phục vụ tốt nhất.\n");
        sb.append("Trân trọng cảm ơn,\n").append(spaName);
        return sb.toString();
    }

    private String buildConfirmationHtmlContent(String spaName, String customerName, String bookingCode,
                                                String serviceName, int durationMinutes, String formattedTime,
                                                String staffName, String formattedPrice, String paymentStatusText,
                                                String spaAddress, String spaPhone) {
        return "<!DOCTYPE html>"
                + "<html lang=\"vi\">"
                + "<head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333333; background-color: #f7f9fa; margin: 0; padding: 20px;\">"
                + "<div style=\"max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06);\">"
                + "  <div style=\"background-color: #0f766e; padding: 24px; text-align: center; color: #ffffff;\">"
                + "    <h1 style=\"margin: 0; font-size: 22px; font-weight: 600;\">" + escapeHtml(spaName) + "</h1>"
                + "    <p style=\"margin: 8px 0 0 0; font-size: 14px; opacity: 0.9;\">Xác nhận đặt lịch hẹn thành công</p>"
                + "  </div>"
                + "  <div style=\"padding: 24px;\">"
                + "    <p>Kính chào <strong>" + escapeHtml(customerName) + "</strong>,</p>"
                + "    <p>Cảm ơn Quý khách đã đặt lịch dịch vụ tại <strong>" + escapeHtml(spaName) + "</strong>. Lịch hẹn của Quý khách đã được xác nhận với thông tin chi tiết như sau:</p>"
                + "    <div style=\"background-color: #f0fdfa; border: 1px solid #ccfbf1; border-radius: 6px; padding: 16px; margin: 20px 0;\">"
                + "      <table style=\"width: 100%; border-collapse: collapse; font-size: 14px;\">"
                + "        <tr><td style=\"padding: 6px 0; color: #64748b; width: 140px;\">Mã đặt lịch:</td><td style=\"font-weight: bold; color: #0f766e;\">" + escapeHtml(bookingCode) + "</td></tr>"
                + "        <tr><td style=\"padding: 6px 0; color: #64748b;\">Dịch vụ:</td><td style=\"font-weight: 600;\">" + escapeHtml(serviceName) + " (" + durationMinutes + " phút)</td></tr>"
                + "        <tr><td style=\"padding: 6px 0; color: #64748b;\">Thời gian hẹn:</td><td style=\"font-weight: 600; color: #b45309;\">" + escapeHtml(formattedTime) + "</td></tr>"
                + "        <tr><td style=\"padding: 6px 0; color: #64748b;\">Kỹ thuật viên:</td><td>" + escapeHtml(staffName) + "</td></tr>"
                + "        <tr><td style=\"padding: 6px 0; color: #64748b;\">Tổng chi phí:</td><td style=\"font-weight: 600;\">" + escapeHtml(formattedPrice) + "</td></tr>"
                + "        <tr><td style=\"padding: 6px 0; color: #64748b;\">Thanh toán:</td><td>" + escapeHtml(paymentStatusText) + "</td></tr>"
                + (spaAddress != null && !spaAddress.isBlank() ? "<tr><td style=\"padding: 6px 0; color: #64748b;\">Địa chỉ:</td><td>" + escapeHtml(spaAddress) + "</td></tr>" : "")
                + (spaPhone != null && !spaPhone.isBlank() ? "<tr><td style=\"padding: 6px 0; color: #64748b;\">Hotline:</td><td>" + escapeHtml(spaPhone) + "</td></tr>" : "")
                + "      </table>"
                + "    </div>"
                + "    <p style=\"font-size: 13px; color: #64748b;\"><em>Lưu ý: Quý khách vui lòng đến trước giờ hẹn 10-15 phút để chuẩn bị dịch vụ chu đáo nhất.</em></p>"
                + "    <p style=\"margin-top: 24px;\">Trân trọng cảm ơn,<br><strong>" + escapeHtml(spaName) + "</strong></p>"
                + "  </div>"
                + "</div>"
                + "</body></html>";
    }

    private String buildReminderTextContent(String spaName, String customerName, String bookingCode,
                                            String serviceName, String formattedTime, String staffName,
                                            String spaAddress, String spaPhone) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kính chào ").append(customerName).append(",\n\n");
        sb.append(spaName).append(" xin nhắc Quý khách có lịch hẹn dịch vụ sắp diễn ra trong khoảng 1 giờ tới.\n\n");
        sb.append("- Mã đặt lịch: ").append(bookingCode).append("\n");
        sb.append("- Dịch vụ: ").append(serviceName).append("\n");
        sb.append("- Thời gian hẹn: ").append(formattedTime).append("\n");
        sb.append("- Kỹ thuật viên: ").append(staffName).append("\n");
        if (spaAddress != null && !spaAddress.isBlank()) {
            sb.append("- Địa chỉ spa: ").append(spaAddress).append("\n");
        }
        if (spaPhone != null && !spaPhone.isBlank()) {
            sb.append("- Hotline hỗ trợ: ").append(spaPhone).append("\n");
        }
        sb.append("\nKính mong Quý khách sắp xếp thời gian đến đúng giờ. Hân hạnh được đón tiếp Quý khách!\n\n");
        sb.append("Trân trọng,\n").append(spaName);
        return sb.toString();
    }

    private String buildReminderHtmlContent(String spaName, String customerName, String bookingCode,
                                            String serviceName, String formattedTime, String staffName,
                                            String spaAddress, String spaPhone) {
        return "<!DOCTYPE html>"
                + "<html lang=\"vi\">"
                + "<head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333333; background-color: #f7f9fa; margin: 0; padding: 20px;\">"
                + "<div style=\"max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06);\">"
                + "  <div style=\"background-color: #d97706; padding: 24px; text-align: center; color: #ffffff;\">"
                + "    <h1 style=\"margin: 0; font-size: 22px; font-weight: 600;\">" + escapeHtml(spaName) + "</h1>"
                + "    <p style=\"margin: 8px 0 0 0; font-size: 14px; opacity: 0.9;\">Nhắc nhở lịch hẹn sắp diễn ra (trong 1 giờ tới)</p>"
                + "  </div>"
                + "  <div style=\"padding: 24px;\">"
                + "    <p>Kính chào <strong>" + escapeHtml(customerName) + "</strong>,</p>"
                + "    <p>" + escapeHtml(spaName) + " xin nhắc Quý khách có lịch hẹn dịch vụ sắp diễn ra trong khoảng 1 giờ tới:</p>"
                + "    <div style=\"background-color: #fffbeb; border: 1px solid #fde68a; border-radius: 6px; padding: 16px; margin: 20px 0;\">"
                + "      <table style=\"width: 100%; border-collapse: collapse; font-size: 14px;\">"
                + "        <tr><td style=\"padding: 6px 0; color: #64748b; width: 140px;\">Mã đặt lịch:</td><td style=\"font-weight: bold; color: #b45309;\">" + escapeHtml(bookingCode) + "</td></tr>"
                + "        <tr><td style=\"padding: 6px 0; color: #64748b;\">Dịch vụ:</td><td style=\"font-weight: 600;\">" + escapeHtml(serviceName) + "</td></tr>"
                + "        <tr><td style=\"padding: 6px 0; color: #64748b;\">Thời gian hẹn:</td><td style=\"font-weight: 600; color: #dc2626;\">" + escapeHtml(formattedTime) + "</td></tr>"
                + "        <tr><td style=\"padding: 6px 0; color: #64748b;\">Kỹ thuật viên:</td><td>" + escapeHtml(staffName) + "</td></tr>"
                + (spaAddress != null && !spaAddress.isBlank() ? "<tr><td style=\"padding: 6px 0; color: #64748b;\">Địa chỉ:</td><td>" + escapeHtml(spaAddress) + "</td></tr>" : "")
                + (spaPhone != null && !spaPhone.isBlank() ? "<tr><td style=\"padding: 6px 0; color: #64748b;\">Hotline:</td><td>" + escapeHtml(spaPhone) + "</td></tr>" : "")
                + "      </table>"
                + "    </div>"
                + "    <p style=\"font-size: 13px; color: #64748b;\"><em>Kính mong Quý khách sắp xếp thời gian đến đúng giờ để có trải nghiệm dịch vụ trọn vẹn nhất.</em></p>"
                + "    <p style=\"margin-top: 24px;\">Trân trọng cảm ơn,<br><strong>" + escapeHtml(spaName) + "</strong></p>"
                + "  </div>"
                + "</div>"
                + "</body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
