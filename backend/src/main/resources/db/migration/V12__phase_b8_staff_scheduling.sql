-- ============================================================================
-- V12: Phase B.8 — Staff Scheduling (Working Hours, Days Off, Availability)
-- ============================================================================

CREATE TABLE IF NOT EXISTS staff_working_hours (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_swh_staff FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE,
    CONSTRAINT fk_swh_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT uk_staff_day_of_week UNIQUE (tenant_id, staff_id, day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_swh_staff_tenant ON staff_working_hours (tenant_id, staff_id);

CREATE TABLE IF NOT EXISTS staff_days_off (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    date DATE NOT NULL,
    reason VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sdo_staff FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE,
    CONSTRAINT fk_sdo_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT uk_staff_date UNIQUE (tenant_id, staff_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sdo_staff_tenant ON staff_days_off (tenant_id, staff_id, date);

-- Initialize default 7-day schedule for existing staff
INSERT IGNORE INTO staff_working_hours (staff_id, tenant_id, day_of_week, start_time, end_time, is_active, created_at, updated_at)
SELECT s.id, s.tenant_id, d.day_name, '09:00:00', '18:00:00',
       CASE WHEN d.day_name = 'SUNDAY' THEN FALSE ELSE TRUE END,
       NOW(), NOW()
FROM staff s
CROSS JOIN (
    SELECT 'MONDAY' AS day_name UNION ALL
    SELECT 'TUESDAY' UNION ALL
    SELECT 'WEDNESDAY' UNION ALL
    SELECT 'THURSDAY' UNION ALL
    SELECT 'FRIDAY' UNION ALL
    SELECT 'SATURDAY' UNION ALL
    SELECT 'SUNDAY'
) d;
