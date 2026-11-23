IF COL_LENGTH('dbo.invoice_imports', 'category') IS NULL
BEGIN
    ALTER TABLE dbo.invoice_imports ADD category tinyint NULL;
END;

UPDATE dbo.invoice_imports
SET category = 1
WHERE category IS NULL;
