-- ============================================================================
-- Phase B.6 — Payment Models & History
-- ============================================================================

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    txn_ref VARCHAR(64) NOT NULL UNIQUE,
    transaction_no VARCHAR(64),
    bank_code VARCHAR(32),
    card_type VARCHAR(32),
    response_code VARCHAR(32),
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE INDEX idx_payments_tenant ON payments(tenant_id);
CREATE INDEX idx_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_txn_ref ON payments(txn_ref);
CREATE INDEX idx_payments_status ON payments(status);

-- Backfill payments for existing bookings
INSERT INTO payments (tenant_id, booking_id, payment_method, provider, amount, status, txn_ref, paid_at, created_at, updated_at)
SELECT b.tenant_id, b.id, 'PAY_AT_SPA', 'LOCAL', b.price,
       CASE WHEN b.status = 'COMPLETED' THEN 'PAID' ELSE 'UNPAID' END,
       CONCAT('INIT-', b.booking_code),
       CASE WHEN b.status = 'COMPLETED' THEN b.created_at ELSE NULL END,
       b.created_at, b.updated_at
FROM bookings b
WHERE NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = b.id);
