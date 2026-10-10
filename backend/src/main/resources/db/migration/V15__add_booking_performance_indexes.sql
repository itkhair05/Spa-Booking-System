-- ============================================================================
-- V15: Performance Optimization Indexes for Bookings & Availability Lookup
-- ============================================================================

-- Index for tenant status queries and dashboard status aggregation
CREATE INDEX idx_bookings_tenant_status ON bookings (tenant_id, status);

-- Composite index for staff schedule, blocking bookings, and overlap validation
CREATE INDEX idx_bookings_tenant_staff_status_times ON bookings (tenant_id, staff_id, status, start_time, end_time);

-- Composite index for customer overlap checks
CREATE INDEX idx_bookings_tenant_customer_status_times ON bookings (tenant_id, customer_id, status, start_time, end_time);
