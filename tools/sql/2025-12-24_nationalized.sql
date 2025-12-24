-- Ensure Unicode (NVARCHAR) columns for Vietnamese text
-- Run on SQL Server (hoadon_database)

-- Roles
IF EXISTS (SELECT 1 FROM sys.columns WHERE Name = N'description' AND Object_ID = Object_ID(N'dbo.roles'))
BEGIN
  ALTER TABLE dbo.roles ALTER COLUMN description NVARCHAR(255) NULL;
END
IF EXISTS (SELECT 1 FROM sys.columns WHERE Name = N'display_name' AND Object_ID = Object_ID(N'dbo.roles'))
BEGIN
  ALTER TABLE dbo.roles ALTER COLUMN display_name NVARCHAR(255) NULL;
END
IF EXISTS (SELECT 1 FROM sys.columns WHERE Name = N'name' AND Object_ID = Object_ID(N'dbo.roles'))
BEGIN
  ALTER TABLE dbo.roles ALTER COLUMN name NVARCHAR(255) NOT NULL;
END

-- Permissions
IF EXISTS (SELECT 1 FROM sys.columns WHERE Name = N'description' AND Object_ID = Object_ID(N'dbo.permissions'))
BEGIN
  ALTER TABLE dbo.permissions ALTER COLUMN description NVARCHAR(255) NULL;
END
IF EXISTS (SELECT 1 FROM sys.columns WHERE Name = N'display_name' AND Object_ID = Object_ID(N'dbo.permissions'))
BEGIN
  ALTER TABLE dbo.permissions ALTER COLUMN display_name NVARCHAR(255) NULL;
END
IF EXISTS (SELECT 1 FROM sys.columns WHERE Name = N'name' AND Object_ID = Object_ID(N'dbo.permissions'))
BEGIN
  ALTER TABLE dbo.permissions ALTER COLUMN name NVARCHAR(255) NOT NULL;
END

-- Permission categories
IF EXISTS (SELECT 1 FROM sys.columns WHERE Name = N'name' AND Object_ID = Object_ID(N'dbo.permission_categories'))
BEGIN
  ALTER TABLE dbo.permission_categories ALTER COLUMN name NVARCHAR(255) NOT NULL;
END

-- Users (optional but recommended)
IF EXISTS (SELECT 1 FROM sys.columns WHERE Name = N'name' AND Object_ID = Object_ID(N'dbo.users'))
BEGIN
  ALTER TABLE dbo.users ALTER COLUMN name NVARCHAR(255) NULL;
END
IF EXISTS (SELECT 1 FROM sys.columns WHERE Name = N'email' AND Object_ID = Object_ID(N'dbo.users'))
BEGIN
  ALTER TABLE dbo.users ALTER COLUMN email NVARCHAR(255) NULL;
END
IF EXISTS (SELECT 1 FROM sys.columns WHERE Name = N'username' AND Object_ID = Object_ID(N'dbo.users'))
BEGIN
  -- username is ASCII in most cases; keep as VARCHAR if desired. If you want Unicode usernames, uncomment next line
  -- ALTER TABLE dbo.users ALTER COLUMN username NVARCHAR(32) NOT NULL;
END
