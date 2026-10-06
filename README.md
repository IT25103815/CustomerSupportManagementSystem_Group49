# Helpify - Web-Based Customer Support Management System

Helpify is a Java 17 / JSP / Servlet web application using a DAO-based architecture and Microsoft SQL Server through JDBC.

## Architecture

`JSP UI -> Servlet -> DAO -> JDBC -> Microsoft SQL Server`

Core DAO classes expose the SQL used for CRUD operations so the data flow is easy to demonstrate during a viva.

## Main modules

1. FAQ Management - `FaqDao`
2. Ticket Management - `TicketDao`
3. Feedback Management - `FeedbackDao`
4. User & Access Management - `UserDao`
5. Communication & Notifications - `MessageDao` and `NotificationDao`
6. Dashboard, Analytics & Report Management - `ReportDao` plus dashboard `SELECT` queries in `TicketDao`

## Requirements

- JDK 17
- Maven
- Tomcat 10.1+ / Jakarta Servlet 6 compatible server
- Microsoft SQL Server

## Database setup

1. Open SQL Server Management Studio.
2. Run `database/CustomerSupportDB.sql` for a fresh demo database.
3. If upgrading an existing project database, run `database/HelpifyMigration.sql`.
4. Edit `src/main/resources/database.properties` and set your SQL Server `sa` password, or use the environment variables `CSMS_DB_URL`, `CSMS_DB_USERNAME`, `CSMS_DB_PASSWORD`.

Never commit real database passwords to a public repository.

## Demo accounts

The fresh SQL script creates the usernames `customer`, `relations`, `senior`, `operations`, `manager`, `itsupport`, and `qa`.
The demonstration password is `Demo@123`.

## Password policy

Passwords require at least 8 characters including uppercase, lowercase, a number, and a special character. Passwords are stored using PBKDF2-HMAC-SHA256 with a random salt.

## Running in IntelliJ

1. Open the folder containing `pom.xml` as a Maven project.
2. Set Project SDK to JDK 17.
3. Configure Tomcat 10.1+ in Run/Debug Configurations.
4. Deploy the WAR exploded artifact.
5. Start the server and open the application context URL.

## Evaluation-focused functions

- Database-backed CRUD in the main modules.
- Customer ticket editing/cancellation while a ticket is still OPEN and unassigned.
- Employee profile editing and secure password changes.
- Automatic in-system notification when a ticket is assigned to an employee.
- Saved report CRUD through `ReportDao`.
- Server-side validation for important forms.
- Role-based access and CSRF protection.
