-- Add is_deleted column to staff table for archiving deleted staff while preserving historical bookings
ALTER TABLE staff ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
