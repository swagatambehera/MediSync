-- Medicine Reminder and Stock Monitoring System
-- Run this script in MySQL Workbench. It is safe to re-run for existing installations.
CREATE DATABASE IF NOT EXISTS medicine_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE medicine_system;

CREATE TABLE IF NOT EXISTS medicines (
    medicine_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 5,
    expiry_date DATE NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_medicine_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_low_stock_threshold CHECK (low_stock_threshold >= 0),
    INDEX idx_medicine_name (name),
    INDEX idx_medicine_expiry (expiry_date)
);

CREATE TABLE IF NOT EXISTS reminders (
    reminder_id INT PRIMARY KEY AUTO_INCREMENT,
    medicine_id INT NOT NULL,
    reminder_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notified_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reminder_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id) ON DELETE CASCADE,
    CONSTRAINT chk_reminder_status CHECK (status IN ('PENDING','NOTIFIED','TAKEN','MISSED','SKIPPED')),
    INDEX idx_reminder_due (status, reminder_time)
);

-- Upgrade helper for an existing database. Run only if these columns do not exist yet:
-- ALTER TABLE reminders ADD COLUMN notified_at DATETIME NULL;
-- ALTER TABLE reminders ADD COLUMN completed_at DATETIME NULL;
