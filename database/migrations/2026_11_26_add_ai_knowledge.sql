IF OBJECT_ID(N'dbo.ai_knowledge_categories', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ai_knowledge_categories
    (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code VARCHAR(80) NOT NULL,
        name NVARCHAR(255) NOT NULL,
        description NVARCHAR(1000) NULL,
        active BIT NOT NULL CONSTRAINT DF_ai_knowledge_categories_active DEFAULT 1,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_ai_knowledge_categories_created DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_ai_knowledge_categories_updated DEFAULT SYSDATETIME(),
        CONSTRAINT UQ_ai_knowledge_categories_code UNIQUE (code)
    );

    INSERT INTO dbo.ai_knowledge_categories (code, name, description, active)
    VALUES ('GENERAL', N'Kiến thức chung', N'Hướng dẫn, câu hỏi thường gặp và kiến thức dùng chung', 1);
END;

IF OBJECT_ID(N'dbo.ai_knowledge_documents', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ai_knowledge_documents
    (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NULL,
        category_id BIGINT NULL,
        title NVARCHAR(255) NOT NULL,
        document_type VARCHAR(16) NOT NULL,
        scope VARCHAR(16) NOT NULL,
        active BIT NOT NULL CONSTRAINT DF_ai_knowledge_documents_active DEFAULT 0,
        external_share_allowed BIT NOT NULL CONSTRAINT DF_ai_knowledge_documents_external DEFAULT 0,
        status VARCHAR(24) NOT NULL,
        current_version_id BIGINT NULL,
        created_by BIGINT NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_ai_knowledge_documents_created DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_ai_knowledge_documents_updated DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ai_knowledge_documents_category FOREIGN KEY (category_id)
            REFERENCES dbo.ai_knowledge_categories(id),
        CONSTRAINT CK_ai_knowledge_documents_scope CHECK
            ((scope = 'GLOBAL' AND company_id IS NULL) OR (scope = 'COMPANY' AND company_id IS NOT NULL)),
        CONSTRAINT CK_ai_knowledge_documents_type CHECK
            (document_type IN ('MD', 'TXT', 'PDF', 'DOCX', 'XLSX', 'XML', 'XSD')),
        CONSTRAINT CK_ai_knowledge_documents_status CHECK
            (status IN ('PROCESSING', 'READY', 'OCR_REQUIRED', 'FAILED'))
    );

    CREATE INDEX IX_ai_knowledge_documents_company
        ON dbo.ai_knowledge_documents (company_id, active);
    CREATE INDEX IX_ai_knowledge_documents_category
        ON dbo.ai_knowledge_documents (category_id);
END;

IF OBJECT_ID(N'dbo.ai_knowledge_document_versions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ai_knowledge_document_versions
    (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        document_id BIGINT NOT NULL,
        version_number INT NOT NULL,
        original_filename NVARCHAR(255) NOT NULL,
        mime_type VARCHAR(150) NULL,
        file_size BIGINT NOT NULL,
        checksum VARCHAR(64) NOT NULL,
        source_blob VARBINARY(MAX) NOT NULL,
        extracted_content NVARCHAR(MAX) NULL,
        status VARCHAR(24) NOT NULL,
        effective_from DATETIME2 NULL,
        effective_to DATETIME2 NULL,
        prompt_injection_detected BIT NOT NULL
            CONSTRAINT DF_ai_knowledge_versions_injection DEFAULT 0,
        error_message NVARCHAR(1000) NULL,
        created_by BIGINT NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_ai_knowledge_versions_created DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ai_knowledge_versions_document FOREIGN KEY (document_id)
            REFERENCES dbo.ai_knowledge_documents(id) ON DELETE CASCADE,
        CONSTRAINT UQ_ai_knowledge_versions_number UNIQUE (document_id, version_number),
        CONSTRAINT CK_ai_knowledge_versions_status CHECK
            (status IN ('PROCESSING', 'READY', 'OCR_REQUIRED', 'FAILED')),
        CONSTRAINT CK_ai_knowledge_versions_effective CHECK
            (effective_to IS NULL OR effective_from IS NULL OR effective_to > effective_from)
    );
END;

IF OBJECT_ID(N'dbo.ai_knowledge_chunks', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ai_knowledge_chunks
    (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        document_id BIGINT NOT NULL,
        version_id BIGINT NOT NULL,
        chunk_index INT NOT NULL,
        heading NVARCHAR(500) NULL,
        source_location NVARCHAR(500) NOT NULL,
        content NVARCHAR(MAX) NOT NULL,
        estimated_tokens INT NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_ai_knowledge_chunks_created DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ai_knowledge_chunks_document FOREIGN KEY (document_id)
            REFERENCES dbo.ai_knowledge_documents(id),
        CONSTRAINT FK_ai_knowledge_chunks_version FOREIGN KEY (version_id)
            REFERENCES dbo.ai_knowledge_document_versions(id) ON DELETE CASCADE,
        CONSTRAINT UQ_ai_knowledge_chunks_position UNIQUE (version_id, chunk_index)
    );

    CREATE INDEX IX_ai_knowledge_chunks_document
        ON dbo.ai_knowledge_chunks (document_id, version_id);
    CREATE INDEX IX_ai_knowledge_chunks_version
        ON dbo.ai_knowledge_chunks (version_id, chunk_index);
END;
