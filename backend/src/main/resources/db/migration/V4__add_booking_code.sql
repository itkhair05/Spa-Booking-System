ALTER TABLE bookings
ADD COLUMN booking_code VARCHAR(64);

UPDATE bookings
SET booking_code = CONCAT('BK-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 10)))
WHERE booking_code IS NULL;

ALTER TABLE bookings
MODIFY COLUMN booking_code VARCHAR(64) NOT NULL;

CREATE UNIQUE INDEX uq_bookings_booking_code ON bookings(booking_code);
CREATE INDEX idx_bookings_tenant_booking_code ON bookings(tenant_id, booking_code);
