CREATE TABLE clm_document_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clm_document_id BIGINT NOT NULL,
    file_type VARCHAR(40) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(255) NULL,
    CONSTRAINT fk_clm_document_files_document
        FOREIGN KEY (clm_document_id) REFERENCES clm_documents(id),
    CONSTRAINT uk_clm_document_files_document_type
        UNIQUE (clm_document_id, file_type),
    CONSTRAINT uk_clm_document_files_storage_key
        UNIQUE (storage_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_clm_document_files_document_id
    ON clm_document_files(clm_document_id);
