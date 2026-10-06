USE CustomerSupportDB;
GO

-- Required by ReportDao CRUD in the Helpify revision.
IF COL_LENGTH('saved_reports','updated_at') IS NULL
BEGIN
    ALTER TABLE saved_reports ADD updated_at DATETIME2 NOT NULL CONSTRAINT DF_saved_reports_updated_at DEFAULT SYSDATETIME();
END
GO


-- Ticket screenshot attachment support. Fresh installations already create this table in CustomerSupportDB.sql.
IF OBJECT_ID('ticket_attachments','U') IS NULL
BEGIN
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
    );
END
GO
