-- Phase B.1: Add avatar_url for staff and image_url for services
ALTER TABLE staff ADD COLUMN avatar_url VARCHAR(512) NULL;
ALTER TABLE services ADD COLUMN image_url VARCHAR(512) NULL;
