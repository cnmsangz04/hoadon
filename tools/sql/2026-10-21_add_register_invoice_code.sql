IF COL_LENGTH('dbo.register_invoices', 'code') IS NULL
BEGIN
    ALTER TABLE [dbo].[register_invoices]
    ADD [code] VARCHAR(100) NULL;
END;
