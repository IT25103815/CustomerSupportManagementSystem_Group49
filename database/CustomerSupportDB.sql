IF DB_ID('CustomerSupportDB') IS NULL CREATE DATABASE CustomerSupportDB
GO
USE CustomerSupportDB
GO 

IF OBJECT_ID('ticket_attachments','U') IS NOT NULL DROP TABLE ticket_attachments
IF OBJECT_ID('ticket_messages','U') IS NOT NULL DROP TABLE ticket_messages
IF OBJECT_ID('ticket_status_history','U') IS NOT NULL DROP TABLE ticket_status_history
IF OBJECT_ID('feedback','U') IS NOT NULL DROP TABLE feedback
IF OBJECT_ID('notifications','U') IS NOT NULL DROP TABLE notifications
IF OBJECT_ID('tickets','U') IS NOT NULL DROP TABLE tickets
IF OBJECT_ID('ticket_categories','U') IS NOT NULL DROP TABLE ticket_categories
IF OBJECT_ID('faqs','U') IS NOT NULL DROP TABLE faqs
IF OBJECT_ID('faq_categories','U') IS NOT NULL DROP TABLE faq_categories
IF OBJECT_ID('audit_logs','U') IS NOT NULL DROP TABLE audit_logs
IF OBJECT_ID('saved_reports','U') IS NOT NULL DROP TABLE saved_reports
IF OBJECT_ID('password_reset_tokens','U') IS NOT NULL DROP TABLE password_reset_tokens
IF OBJECT_ID('users','U') IS NOT NULL DROP TABLE users
GO

CREATE TABLE users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    full_name NVARCHAR(120) NOT NULL,
    email NVARCHAR(160) NOT NULL UNIQUE,
    username NVARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(500) NOT NULL,
    phone NVARCHAR(30),
    role VARCHAR(50) NOT NULL CHECK (role IN ('CUSTOMER','CUSTOMER_RELATIONS_OFFICER','SENIOR_CUSTOMER_SERVICE_OFFICER','OPERATIONS_EXECUTIVE','CUSTOMER_SUPPORT_MANAGER','IT_SUPPORT_COORDINATOR','QUALITY_ASSURANCE_SUPERVISOR')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','LOCKED')),
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
)

CREATE TABLE faq_categories (
    category_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(500)
)

CREATE TABLE faqs (
    faq_id INT IDENTITY(1,1) PRIMARY KEY,
    category_id INT NULL REFERENCES faq_categories(category_id),
    question NVARCHAR(300) NOT NULL,
    answer NVARCHAR(MAX) NOT NULL,
    published BIT NOT NULL DEFAULT 0,
    created_by INT NOT NULL REFERENCES users(user_id),
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
)

CREATE TABLE ticket_categories (
    category_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(500),
    sla_hours INT NOT NULL DEFAULT 48
)

CREATE TABLE tickets (
    ticket_id INT IDENTITY(1,1) PRIMARY KEY,
    ticket_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id INT NOT NULL REFERENCES users(user_id),
    category_id INT NOT NULL REFERENCES ticket_categories(category_id),
    assigned_to INT NULL REFERENCES users(user_id),
    subject NVARCHAR(220) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','ASSIGNED','IN_PROGRESS','WAITING_FOR_CUSTOMER','ESCALATED','RESOLVED','CLOSED','CANCELLED','REOPENED')),
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    resolved_at DATETIME2 NULL,
    closed_at DATETIME2 NULL
)

CREATE INDEX IX_tickets_status ON tickets(status)
CREATE INDEX IX_tickets_customer ON tickets(customer_id)
CREATE INDEX IX_tickets_assigned ON tickets(assigned_to)

CREATE TABLE ticket_status_history (
    history_id INT IDENTITY(1,1) PRIMARY KEY,
    ticket_id INT NOT NULL REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by INT NOT NULL REFERENCES users(user_id),
    note NVARCHAR(500),
    changed_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
)

CREATE TABLE ticket_messages (
    message_id INT IDENTITY(1,1) PRIMARY KEY,
    ticket_id INT NOT NULL REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    sender_id INT NOT NULL REFERENCES users(user_id),
    message NVARCHAR(MAX) NOT NULL,
    internal_note BIT NOT NULL DEFAULT 0,
    is_read BIT NOT NULL DEFAULT 0,
    sent_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
)

CREATE TABLE ticket_attachments (
    attachment_id INT IDENTITY(1,1) PRIMARY KEY,
    ticket_id INT NOT NULL REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    message_id INT NULL REFERENCES ticket_messages(message_id),
    original_name NVARCHAR(255) NOT NULL,
    stored_name NVARCHAR(255) NOT NULL,
    content_type NVARCHAR(100),
    file_size BIGINT NOT NULL,
    uploaded_by INT NOT NULL REFERENCES users(user_id),
    uploaded_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
)

CREATE TABLE feedback (
    feedback_id INT IDENTITY(1,1) PRIMARY KEY,
    ticket_id INT NOT NULL UNIQUE REFERENCES tickets(ticket_id),
    customer_id INT NOT NULL REFERENCES users(user_id),
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments NVARCHAR(1000),
    response NVARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW','REVIEWED','RESPONDED','REMOVED')),
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
)

CREATE TABLE notifications (
    notification_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title NVARCHAR(160) NOT NULL,
    message NVARCHAR(500) NOT NULL,
    link NVARCHAR(255),
    is_read BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
)

CREATE TABLE saved_reports (
    report_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(150) NOT NULL,
    report_type VARCHAR(60) NOT NULL,
    filters NVARCHAR(MAX),
    created_by INT NOT NULL REFERENCES users(user_id),
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
)

CREATE TABLE audit_logs (
    log_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NULL REFERENCES users(user_id),
    action NVARCHAR(120) NOT NULL,
    entity_type NVARCHAR(80),
    entity_id NVARCHAR(80),
    details NVARCHAR(1000),
    ip_address VARCHAR(50),
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
)

CREATE TABLE password_reset_tokens (
    token_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    used BIT NOT NULL DEFAULT 0
)
GO

INSERT INTO faq_categories(name,description) VALUES
('Accounts','Login, password and profile help'),
('Technical Support','Common technical troubleshooting'),
('Services','Questions about services and requests')

INSERT INTO ticket_categories(name,description,sla_hours) VALUES
('Account Access','Login and account access problems',24),
('Technical Issue','Application or service technical problems',24),
('Service Request','General service requests',48),
('Complaint','Customer complaints requiring review',24),
('Billing Question','Questions about invoices or charges',48)
GO

-- All demonstration website accounts use password Demo@123.
-- The value below is PBKDF2-HMAC-SHA256 in iterations:salt:hash format.
DECLARE @DemoHash VARCHAR(500) = '120000:4f70656e41495f47726f75703439:a74e1a5a5838ba1f838bb595c5469f779ef706758001c0fb479288f113cb1ba6'
INSERT INTO users(full_name,email,username,password_hash,phone,role,status) VALUES
('Demo Customer','customer@helpify.local','customer',@DemoHash,'0770000001','CUSTOMER','ACTIVE'),
('Relations Officer','relations@helpify.local','relations',@DemoHash,'0770000002','CUSTOMER_RELATIONS_OFFICER','ACTIVE'),
('Senior Service Officer','senior@helpify.local','senior',@DemoHash,'0770000003','SENIOR_CUSTOMER_SERVICE_OFFICER','ACTIVE'),
('Operations Executive','operations@helpify.local','operations',@DemoHash,'0770000004','OPERATIONS_EXECUTIVE','ACTIVE'),
('Support Manager','manager@helpify.local','manager',@DemoHash,'0770000005','CUSTOMER_SUPPORT_MANAGER','ACTIVE'),
('IT Support Coordinator','itsupport@helpify.local','itsupport',@DemoHash,'0770000006','IT_SUPPORT_COORDINATOR','ACTIVE'),
('QA Supervisor','qa@helpify.local','qa',@DemoHash,'0770000007','QUALITY_ASSURANCE_SUPERVISOR','ACTIVE')
GO

INSERT INTO faqs(category_id,question,answer,published,created_by) VALUES
(1,'How do I reset my password?','Use the Forgot Password option on the login page or contact IT Support if your account is locked.',1,2),
(2,'How can I track my support request?','Sign in, open My Tickets, and select the ticket number to see its current status and activity history.',1,2),
(3,'When will I receive a response?','Response time depends on priority and category. Urgent issues are reviewed first and the ticket page displays every update.',1,2)

INSERT INTO tickets(ticket_number,customer_id,category_id,assigned_to,subject,description,priority,status) VALUES
('TKT-2026-00001',1,2,3,'Cannot open the customer portal','The portal displays an error after I sign in.','HIGH','IN_PROGRESS'),
('TKT-2026-00002',1,3,NULL,'Request account statement','Please provide the latest service statement.','MEDIUM','OPEN')

INSERT INTO ticket_messages(ticket_id,sender_id,message,internal_note,is_read) VALUES
(1,1,'The problem began this morning after the browser update.',0,1),
(1,3,'Thank you. I am checking the browser compatibility and will update you shortly.',0,0)

INSERT INTO notifications(user_id,title,message,link) VALUES
(1,'Ticket update','TKT-2026-00001 is now In Progress.','/tickets?action=view&id=1'),
(3,'New assignment','TKT-2026-00001 has been assigned to you.','/tickets?action=view&id=1')
GO
