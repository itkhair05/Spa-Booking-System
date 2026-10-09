package com.example.spabooking.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailProperties {

    @Value("${resend.api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${resend.api-url:${RESEND_API_URL:https://api.resend.com/emails}}")
    private String resendApiUrl = "https://api.resend.com/emails";

    @Value("${resend.timeout-seconds:${RESEND_TIMEOUT_SECONDS:10}}")
    private int timeoutSeconds = 10;

    @Value("${app.mail.enabled:${APP_MAIL_ENABLED:true}}")
    private boolean enabled = true;

    @Value("${app.mail.from-address:${APP_MAIL_FROM_ADDRESS:}}")
    private String fromAddress;

    @Value("${app.mail.from-name:${APP_MAIL_FROM_NAME:TIKEY SPA}}")
    private String fromName = "TIKEY SPA";

    @Value("${app.mail.reminder.window-start-minutes:${APP_MAIL_REMINDER_WINDOW_START_MINUTES:15}}")
    private int reminderWindowStartMinutes = 15;

    @Value("${app.mail.reminder.window-end-minutes:${APP_MAIL_REMINDER_WINDOW_END_MINUTES:75}}")
    private int reminderWindowEndMinutes = 75;

    @Value("${app.mail.reminder.scheduler-enabled:${APP_MAIL_REMINDER_SCHEDULER_ENABLED:true}}")
    private boolean reminderSchedulerEnabled = true;

    public boolean isConfigured() {
        return enabled
                && resendApiKey != null && !resendApiKey.trim().isEmpty()
                && fromAddress != null && !fromAddress.trim().isEmpty();
    }

    public String getResendApiKey() { return resendApiKey; }
    public void setResendApiKey(String resendApiKey) { this.resendApiKey = resendApiKey; }

    public String getResendApiUrl() { return resendApiUrl; }
    public void setResendApiUrl(String resendApiUrl) { this.resendApiUrl = resendApiUrl; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public int getReminderWindowStartMinutes() { return reminderWindowStartMinutes; }
    public void setReminderWindowStartMinutes(int reminderWindowStartMinutes) {
        this.reminderWindowStartMinutes = reminderWindowStartMinutes;
    }

    public int getReminderWindowEndMinutes() { return reminderWindowEndMinutes; }
    public void setReminderWindowEndMinutes(int reminderWindowEndMinutes) {
        this.reminderWindowEndMinutes = reminderWindowEndMinutes;
    }

    public boolean isReminderSchedulerEnabled() { return reminderSchedulerEnabled; }
    public void setReminderSchedulerEnabled(boolean reminderSchedulerEnabled) {
        this.reminderSchedulerEnabled = reminderSchedulerEnabled;
    }
}
