IF OBJECT_ID(N'dbo.ai_usage_logs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ai_usage_logs
    (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        request_id VARCHAR(36) NOT NULL,
        company_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        feature VARCHAR(50) NOT NULL,
        provider VARCHAR(30) NOT NULL,
        model VARCHAR(100) NULL,
        status VARCHAR(20) NOT NULL,
        error_code VARCHAR(60) NULL,
        input_characters INT NOT NULL CONSTRAINT DF_ai_usage_logs_input_characters DEFAULT 0,
        input_tokens BIGINT NOT NULL CONSTRAINT DF_ai_usage_logs_input_tokens DEFAULT 0,
        output_tokens BIGINT NOT NULL CONSTRAINT DF_ai_usage_logs_output_tokens DEFAULT 0,
        total_tokens BIGINT NOT NULL CONSTRAINT DF_ai_usage_logs_total_tokens DEFAULT 0,
        duration_ms BIGINT NOT NULL CONSTRAINT DF_ai_usage_logs_duration_ms DEFAULT 0,
        attempt_count INT NOT NULL CONSTRAINT DF_ai_usage_logs_attempt_count DEFAULT 0,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_ai_usage_logs_created_at DEFAULT SYSDATETIME(),
        CONSTRAINT UQ_ai_usage_logs_request_id UNIQUE (request_id)
    );

    CREATE INDEX IX_ai_usage_logs_company_created
        ON dbo.ai_usage_logs (company_id, created_at DESC);
END;
