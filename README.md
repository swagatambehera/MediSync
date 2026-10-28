# MediSync 
### Medicine Reminder & Stock Monitoring System

MediSync is a Java-based console application that helps users manage medicines, monitor stock levels, track expiry dates, and schedule medicine reminders. It uses MySQL to store medicine and reminder records.

## Features
- **Medicine Management:** Add, view, search, update stock, and delete medicines.
- **Stock Monitoring:** Identify medicines with low stock.
- **Expiry Alerts:** Check medicines approaching or past their expiry dates.
- **Medicine Reminders:** Schedule reminders and view active reminders.
- **Reminder Tracking:** Record outcomes such as Taken, Missed, or Skipped.
- **Reminder History:** Review previously scheduled reminders and their statuses.

## Tech Stack
- **Language:** Java 17
- **Database:** MySQL 8.0+
- **Connectivity:** JDBC (MySQL Connector/J)
- **Build Tool:** Apache Maven
- **IDE:** Visual Studio Code (optional)

## Prerequisites
Install Java JDK 17+, Apache Maven, and MySQL Server. MySQL Workbench is optional.

## Setup and Run

**1. Clone the repository**
```bash
git clone <REPOSITORY_URL>
cd MediSync
```

**2. Create the database**

Execute `database/schema.sql` in MySQL Workbench to create the required database and tables.

**3. Configure database credentials**

Set these environment variables in your terminal, replacing the password with your actual MySQL password:

**PowerShell**
```powershell
$env:MEDICINE_DB_URL="jjdbc:mysql://localhost:3306/<DATABASE_NAME>?serverTimezone=UTC"
$env:MEDICINE_DB_USER="root"
$env:MEDICINE_DB_PASSWORD="your_mysql_password"
```

**4. Build and run**
```bash
mvn clean package
mvn exec:java
```

Keep the environment variables configured in the same terminal session when running the application.

## Project Structure
```text
MediSync/
├── src/          # Java source code
├── database/     # Database schema and migrations
├── pom.xml       # Maven configuration and dependencies
└── README.md     # Project documentation
```

## Database
The `medicine_system` database stores medicine details and reminder records in MySQL tables. Java accesses the database through JDBC.
