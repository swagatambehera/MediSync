# MediSync
### Medicine Reminder & Stock Monitoring System

MediSync is a Java-based desktop application for managing medicine records, monitoring stock levels and expiry dates, and scheduling medicine reminders. It provides a graphical interface built with Java Swing and stores application data in MySQL through JDBC.

## Features

- **Dashboard:** View summary information about medicine inventory, low-stock items, expiry information, and reminders.
- **Medicine Management:** Add, view, search, update, and delete medicine records.
- **Stock Monitoring:** Identify medicines whose available quantity is at or below the configured low-stock threshold.
- **Expiry Monitoring:** Review medicines that are expired or approaching their expiry date.
- **Medicine Reminders:** Schedule a reminder for a selected medicine with a date and time.
- **Reminder Notifications:** Display an in-application notification when a reminder becomes due while MediSync is running.
- **Reminder Outcome Tracking:** Record a reminder outcome as `TAKEN`, `MISSED`, or `SKIPPED`.
- **Reminder History:** Review saved reminders and their recorded statuses.
- **MySQL Persistence:** Store medicine and reminder records in a MySQL database.

> **Reminder behaviour:** The desktop application must be running for its in-application reminder checker to display notifications. A notification indicates that a reminder was triggered; it does not verify that a medicine was consumed. MediSync is an inventory and reminder aid, not a medical diagnosis or prescription system.

## Technology Stack

| Component | Technology |
|---|---|
| Programming language | Java 17+ |
| Desktop GUI | Java Swing |
| Database | MySQL 8.0+ |
| Database connectivity | JDBC |
| MySQL driver | MySQL Connector/J |
| Build and dependency management | Apache Maven |
| IDE | Visual Studio Code (optional) |

## Prerequisites

Install the following before running the application:

- Java Development Kit (JDK) 17 or later
- Apache Maven
- MySQL Server 8.0 or later
- MySQL Workbench (optional, for database administration)

## Setup and Run

### 1. Clone the repository

```bash
git clone https://github.com/swagatambehera/MediSync.git
cd MediSync
```

### 2. Create the database

Open MySQL Workbench and execute the project schema:

```text
database/schema.sql
```

The script creates the `medicine_system` database and the tables required by the application.

### 3. Configure database credentials

The application reads its database configuration from environment variables. Set them in the terminal that you will use to run MediSync.

**Windows PowerShell:**

```powershell
$env:MEDICINE_DB_URL="jdbc:mysql://localhost:3306/medicine_system?serverTimezone=UTC"
$env:MEDICINE_DB_USER="root"
$env:MEDICINE_DB_PASSWORD="your_mysql_password"
```

Replace `your_mysql_password` with your local MySQL password. Do not commit real credentials or passwords to GitHub.

### 4. Build the project

```bash
mvn clean package
```

### 5. Run the desktop application

```powershell
mvn exec:java "-Dexec.mainClass=gui.MediSyncGUI"
```

Run the command from the project root, in the same terminal session where the database environment variables were configured. Ensure that the MySQL server is running.

> If the Maven `exec` plugin is already configured with the GUI main class in `pom.xml`, `mvn exec:java` may also be sufficient.

## Project Structure

```text
MediSync/
├── src/                              # Java application source code
├── database/
│   └── schema.sql                    # Database and table definitions
├── docs/
│   ├── report/
│   │   ├── Medisync.tex              # LaTeX project report
│   │   └── MediSync_Final_Report_With_Diagrams.pdf
│   └── medisync_assets/
│       ├── screenshots/
│       │   ├── 01_dashboard.png
│       │   ├── 02_medicine_inventory.png
│       │   ├── 03_stock_expiry.png
│       │   ├── 04_reminder_notification.png
│       │   ├── 05_reminder_state.png
│       │   ├── 06_reminder_history.png
│       │   ├── 07_sql_reminders.png
│       │   └── 08_sql_medicines.png
│       ├── MediSync_Architecture.png
│       └── MediSync_Reminder_Workflow.png
├── pom.xml                           # Maven configuration
├── README.md                         # Project documentation
└── .gitignore
```

The tree above describes the intended documentation layout. Keep the filenames in this section synchronized with the files actually committed to the repository.

## Database Overview

MediSync uses the `medicine_system` database, with two related tables:

- **`medicines`:** Stores medicine details such as name, type, dosage, quantity, low-stock threshold, expiry date, notes, and creation timestamp.
- **`reminders`:** Stores the associated medicine ID, scheduled reminder date/time, reminder status, notification timestamp, completion timestamp, and creation timestamp.

Each reminder references a medicine through a foreign key. The schema defines the relationship and allowed reminder statuses.

## Documentation

The `docs/` directory contains supporting project material:

- **Project report:** Describes the project background, objectives, architecture, implementation, database design, testing, results, and future enhancements.
- **Architecture diagram:** Illustrates the major application components and their interactions.
- **Reminder workflow diagram:** Illustrates the reminder lifecycle from scheduling through notification and outcome recording.
- **Screenshots:** Shows the application interface and MySQL Workbench records.

## Notes

- Keep the MySQL server running before launching the application.
- Configure the database environment variables in the same terminal session used to run MediSync.
- Do not upload database passwords, private credentials, or local environment files to the repository.
- The application is intended for local medicine inventory and reminder management; it does not provide medical advice.
