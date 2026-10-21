IF COL_LENGTH('dbo.register_invoices', 'id_attr') IS NULL
BEGIN
    ALTER TABLE [dbo].[register_invoices]
    ADD [id_attr] VARCHAR(100) NULL;
END;
