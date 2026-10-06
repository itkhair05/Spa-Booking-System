-- ============================================================================
-- Phase B.7 — Booking Operations & Lifecycle Timestamps
-- ============================================================================

ALTER TABLE bookings
    ADD COLUMN confirmed_at TIMESTAMP NULL,
    ADD COLUMN checked_in_at TIMESTAMP NULL,
    ADD COLUMN started_at TIMESTAMP NULL,
    ADD COLUMN completed_at TIMESTAMP NULL,
    ADD COLUMN cancelled_at TIMESTAMP NULL,
    ADD COLUMN no_show_at TIMESTAMP NULL,
    ADD COLUMN cancellation_reason VARCHAR(255) NULL;
