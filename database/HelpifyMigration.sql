USE CustomerSupportDB;
GO

-- Required by ReportDao CRUD in the Helpify revision.
IF COL_LENGTH('saved_reports','updated_at') IS NULL
BEGIN
    ALTER TABLE saved_reports ADD updated_at DATETIME2 NOT NULL CONSTRAINT DF_saved_reports_updated_at DEFAULT SYSDATETIME();
END
GO
