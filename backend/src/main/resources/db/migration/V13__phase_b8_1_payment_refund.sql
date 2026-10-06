-- ============================================================================
-- V13: Phase B.8.1 — Payment Refund & Cancellation Policy
-- ============================================================================

CREATE TABLE IF NOT EXISTS refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    refund_request_id VARCHAR(64) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    original_amount DECIMAL(12, 2) NOT NULL,
    refund_amount DECIMAL(12, 2) NOT NULL,
    cancellation_fee DECIMAL(12, 2) NOT NULL,
    policy_percentage INT NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    provider_response_code VARCHAR(32),
    provider_response_message VARCHAR(255),
    provider_transaction_reference VARCHAR(64),
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_refunds_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT fk_refunds_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT uk_refund_request_id UNIQUE (refund_request_id),
    CONSTRAINT uk_refund_payment_id UNIQUE (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refunds_booking ON refunds (tenant_id, booking_id);
CREATE INDEX idx_refunds_payment ON refunds (tenant_id, payment_id);
