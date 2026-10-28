-- OPTIONAL one-time migration for a database created by an earlier project version.
-- BACK UP medicine_system first. Run only if reminders exists and BOTH notified_at and
-- completed_at columns are missing, and idx_reminder_due does not already exist.
-- MySQL versions differ in support for conditional ALTER TABLE, so inspect the schema first.
USE medicine_system;
ALTER TABLE reminders ADD COLUMN notified_at DATETIME NULL;
ALTER TABLE reminders ADD COLUMN completed_at DATETIME NULL;
CREATE INDEX idx_reminder_due ON reminders(status, reminder_time);
-- If the migration partially ran previously, do not rerun blindly: inspect SHOW COLUMNS
-- FROM reminders and SHOW INDEX FROM reminders, then execute only missing statements.
