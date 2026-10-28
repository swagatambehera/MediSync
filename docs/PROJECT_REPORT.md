# Medicine Reminder and Stock Monitoring System

**College Project Report — editable template**

> Replace bracketed fields with your actual details. Update the results section only after running the tests. Do not present sample or expected results as measured results.

## Title Page

**Project:** Medicine Reminder and Stock Monitoring System  
**Submitted by:** [Student name and registration number]  
**Department / Institution:** [Department and college]  
**Course:** [Course name]  
**Faculty guide:** [Faculty name]  
**Academic year:** [Academic year]

## Abstract

The Medicine Reminder and Stock Monitoring System is a Java and MySQL application designed to organize medicine inventory, schedule dose reminders, identify low-stock items, and flag expired or soon-to-expire medicines. Java handles the application workflow, JDBC provides database connectivity, and MySQL stores medicine records, reminder schedules, notification timestamps, and user-recorded outcomes. A background monitoring thread checks due reminders and atomically changes a reminder from PENDING to NOTIFIED before displaying an alert, preventing repeated alerts from the same reminder under normal operation. The project is evaluated through functional tests and a small software-level study of reminder delivery timing and recorded task completion. It is an educational software prototype, not a medical device, and does not replace professional medical advice.

## 1. Introduction

Medicine information and dose schedules can be difficult to track manually. Separate notes may not make low stock or expiry dates visible, and reminders may be overlooked. This application brings medicine details, stock thresholds, expiry checks, reminder scheduling, and reminder outcome history into one database-backed program.

## 2. Problem Statement

Manual medicine tracking can lead to incomplete records, unnoticed low stock, overlooked expiry dates, and difficulty reviewing whether scheduled reminder tasks were recorded as completed. The system provides a structured software tool to manage these records and evaluate its own reminder workflow.

## 3. Objectives

- Maintain medicine records with type, dosage, quantity, threshold, expiry date, and notes.
- Search and manage stored medicine information.
- Identify low-stock, expired, and soon-to-expire medicines.
- Schedule date-and-time reminders linked to medicines.
- Trigger due reminders and avoid duplicate notifications for the same scheduled occurrence.
- Record reminder outcomes as TAKEN, MISSED, or SKIPPED.
- Evaluate software reminder delivery and task-completion recording using reproducible tests.

## 4. Scope and Limitations

The prototype is a local Java console application connected to MySQL. It requires the application to remain running for live reminder checks. It does not provide guaranteed operating-system notifications when closed, cloud synchronization, multiple-user authentication, prescription advice, or clinical adherence verification. A TAKEN status means the user recorded the task as taken; it is not independent proof that medicine was consumed.

## 5. Technology Stack

- Java 17 or later
- MySQL 8.x (recommended)
- JDBC and MySQL Connector/J
- Maven for dependency management
- IntelliJ IDEA or another Java IDE

## 6. Functional Requirements

1. Add, list, search, and delete medicine records.
2. Update stock quantity with non-negative validation.
3. List medicines at or below their low-stock threshold.
4. List expired medicines and medicines expiring within a selected number of days.
5. Schedule a reminder for an existing medicine.
6. Check due reminders in the background and mark them notified before displaying an alert.
7. Record reminder outcome and display reminder history.
8. Persist data in MySQL between application sessions.

## 7. System Design

### Architecture

**Console interface → Java application and validation → DAO layer → JDBC → MySQL database**

A background `ReminderMonitorThread` polls the database every three seconds. `ReminderDAO.claimDueReminders()` uses a transaction and row locks to claim due PENDING reminders and change their status to NOTIFIED. Only reminders successfully claimed by the database are emitted as alerts.

### Database Entities

**medicines:** `medicine_id` (primary key), `name`, `type`, `dosage`, `quantity`, `low_stock_threshold`, `expiry_date`, `notes`, `created_at`.

**reminders:** `reminder_id` (primary key), `medicine_id` (foreign key), `reminder_time`, `status`, `notified_at`, `completed_at`, `created_at`.

Relationship: one medicine can have multiple reminder records. Deleting a medicine also removes its linked reminders.

### Reminder Lifecycle

`PENDING → NOTIFIED → TAKEN | MISSED | SKIPPED`

The user may record an outcome for an active reminder. A reminder notification is a software alert, not a medical instruction.

## 8. Implementation

- `MainApp.java`: menu, input validation, and application lifecycle.
- `model/Medicine.java`: medicine data model.
- `model/Reminder.java`: reminder data model.
- `dao/MedicineDAO.java`: medicine queries and stock/expiry operations.
- `dao/ReminderDAO.java`: reminder persistence, due-reminder claiming, and outcome tracking.
- `threads/ReminderMonitorThread.java`: background due-reminder polling.
- `util/DBConnection.java`: database configuration and JDBC connection creation.
- `database/schema.sql`: initial database and table creation.

## 9. Testing Methodology

Run every test below and record the actual result, date, environment, and any defect found. Use a disposable test database and fictional medicine data.

| ID | Test | Procedure | Expected result | Actual result |
|---|---|---|---|---|
| T01 | Database connection | Launch with correct credentials | Application confirms connection | [Fill after test] |
| T02 | Add medicine | Add valid medicine details | Record is stored and visible | [Fill after test] |
| T03 | Input validation | Enter negative quantity | Input is rejected | [Fill after test] |
| T04 | Search | Search for a partial medicine name | Matching records appear | [Fill after test] |
| T05 | Stock alert | Set quantity at/below threshold | Medicine appears in low-stock list | [Fill after test] |
| T06 | Expiry alert | Add expired and future-expiry records | Correct expiry groups appear | [Fill after test] |
| T07 | Future reminder | Schedule reminder a minute ahead | No alert before due time | [Fill after test] |
| T08 | Due reminder | Keep app running until due time | One alert appears and status becomes NOTIFIED | [Fill after test] |
| T09 | Reminder outcome | Mark alert TAKEN, MISSED, or SKIPPED | Status and history update | [Fill after test] |
| T10 | Persistence | Restart app and view records | Saved data remains | [Fill after test] |
| T11 | Delete | Delete medicine with linked reminders | Medicine and linked reminders are removed | [Fill after test] |

## 10. Evaluation of Reminder Reliability and Task Completion

Use a controlled test with a predefined number of reminders. For each reminder, record the scheduled timestamp, alert timestamp, whether an alert occurred, and the user-recorded outcome. Use a documented tolerance, such as alerting within 10 seconds of the scheduled time, and report that tolerance explicitly.

**Delivery rate (%)** = reminders delivered within the chosen tolerance ÷ reminders scheduled for the test × 100.

**On-time delivery rate (%)** = reminders delivered within the chosen tolerance ÷ reminders whose test window completed × 100.

**Recorded task-completion rate (%)** = eligible test tasks marked TAKEN within the chosen follow-up window ÷ eligible scheduled tasks × 100.

Report the number of trials, machine and database setup, polling interval, tolerance, excluded trials, and raw counts. These measures evaluate this software prototype and user-recorded actions; they do not establish clinical effectiveness or prove medication ingestion.

### Results Table

| Metric | Numerator | Denominator | Measured result |
|---|---:|---:|---:|
| Reminder delivery rate | [actual count] | [actual count] | [calculate] |
| On-time delivery rate | [actual count] | [actual count] | [calculate] |
| Recorded task-completion rate | [actual count] | [actual count] | [calculate] |
| Mean alert delay (seconds) | [measured total delay] | [delivered reminders] | [calculate] |

## 11. Results and Discussion

[Insert the measured test results, screenshots, observed failures, and a discussion. Do not claim the expected results were achieved unless the tests demonstrate it.]

## 12. Security, Privacy, and Safety

Use a local test database and do not put real patient data in screenshots or the submitted archive. Configure database credentials through environment variables rather than committing secrets. The software is intended for educational recordkeeping only. Users should follow their prescription and consult qualified healthcare professionals for medication questions.

## 13. Future Enhancements

Possible improvements include a JavaFX graphical interface, operating-system notifications, configurable recurring schedules, authentication and role-based access, automated backups, and a web/mobile client. These are outside the current console prototype unless specifically required by the course.

## 14. Conclusion

[Summarize the functions that were actually implemented and tested. State the measured results and current limitations.]

## References

1. Oracle. *Java Documentation*. https://docs.oracle.com/en/java/
2. MySQL. *MySQL Reference Manual*. https://dev.mysql.com/doc/
3. MySQL. *Connector/J Developer Guide*. https://dev.mysql.com/doc/connector-j/en/
