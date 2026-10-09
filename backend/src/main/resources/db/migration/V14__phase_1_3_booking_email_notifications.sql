-- ============================================================================
-- Phase 1.3 — Booking Confirmation Email & Appointment Reminder Tracking
-- ============================================================================

ALTER TABLE bookings
    ADD COLUMN confirmation_email_sent_at TIMESTAMP NULL,
    ADD COLUMN reminded_at TIMESTAMP NULL;

CREATE INDEX idx_bookings_reminder_lookup ON bookings (status, is_reminded, start_time);
