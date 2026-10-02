ALTER TABLE bookings
ADD COLUMN price DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

CREATE INDEX idx_bookings_tenant_staff ON bookings(tenant_id, staff_id);
CREATE INDEX idx_bookings_tenant_customer ON bookings(tenant_id, customer_id);
CREATE INDEX idx_bookings_tenant_start_time ON bookings(tenant_id, start_time);
