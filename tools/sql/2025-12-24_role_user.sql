-- Create role_user junction table for assigning work roles to users
-- Target: SQL Server

IF NOT EXISTS (
  SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[role_user]') AND type in (N'U')
)
BEGIN
  CREATE TABLE [dbo].[role_user] (
    [role_id] BIGINT NOT NULL,
    [user_id] BIGINT NOT NULL,
    [user_type] NVARCHAR(255) NOT NULL,
    CONSTRAINT [PK_role_user] PRIMARY KEY ([user_id], [role_id], [user_type]),
    CONSTRAINT [FK_role_user_role]
      FOREIGN KEY ([role_id]) REFERENCES [dbo].[roles] ([id])
      ON DELETE CASCADE
      ON UPDATE CASCADE
  );
END

-- Optional indexes to speed up lookups
IF NOT EXISTS (
  SELECT 1 FROM sys.indexes WHERE name = N'IX_role_user_role' AND object_id = OBJECT_ID(N'[dbo].[role_user]')
)
BEGIN
  CREATE INDEX [IX_role_user_role] ON [dbo].[role_user]([role_id]);
END
IF NOT EXISTS (
  SELECT 1 FROM sys.indexes WHERE name = N'IX_role_user_user' AND object_id = OBJECT_ID(N'[dbo].[role_user]')
)
BEGIN
  CREATE INDEX [IX_role_user_user] ON [dbo].[role_user]([user_id]);
END
