
ALTER TABLE document DROP COLUMN `intake_at`;
ALTER TABLE document ADD COLUMN `intake_at` CHAR(10) NULL DEFAULT NULL AFTER `user_id`;
