package com.example.spabooking.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailProperties {

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    @Value("${app.mail.from-address:}")
    private String fromAddress;

    @Value("${app.mail.from-name:TIKEY SPA}")
    private String fromName;

    @Value("${app.mail.reminder.window-start-minutes:15}")
    private int reminderWindowStartMinutes;

    @Value("${app.mail.reminder.window-end-minutes:75}")
    private int reminderWindowEndMinutes;

    @Value("${app.mail.reminder.scheduler-enabled:true}")
    private boolean reminderSchedulerEnabled;

    public boolean isConfigured() {
        return enabled
                && host != null && !host.trim().isEmpty()
                && fromAddress != null && !fromAddress.trim().isEmpty();
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

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
