-- Thêm quốc tịch của người đại diện pháp luật.
IF COL_LENGTH('dbo.legal_representatives', 'nationality_code') IS NULL
BEGIN
    ALTER TABLE [dbo].[legal_representatives]
    ADD [nationality_code] NVARCHAR(2) NULL;
END;

IF COL_LENGTH('dbo.legal_representatives', 'nationality_name') IS NULL
BEGIN
    ALTER TABLE [dbo].[legal_representatives]
    ADD [nationality_name] NVARCHAR(50) NULL;
END;

