CREATE TABLE manager_application_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    manager_application_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL DEFAULT 'EVIDENCE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_manager_application_files_application
        FOREIGN KEY (manager_application_id) REFERENCES manager_applications(id),
    CONSTRAINT fk_manager_application_files_file
        FOREIGN KEY (file_id) REFERENCES stored_files(id),
    CONSTRAINT uq_manager_application_files UNIQUE (manager_application_id, file_id)
);

CREATE INDEX idx_manager_application_files_application
    ON manager_application_files(manager_application_id);
