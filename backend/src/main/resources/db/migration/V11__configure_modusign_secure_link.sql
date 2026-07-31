ALTER TABLE clm_documents
    ADD COLUMN modusign_participant_id VARCHAR(255) NULL AFTER modusign_document_id,
    ADD COLUMN modusign_template_id VARCHAR(255) NULL AFTER modusign_participant_id,
    ADD COLUMN signing_method VARCHAR(30) NOT NULL DEFAULT 'SECURE_LINK' AFTER modusign_template_id,
    ADD COLUMN signing_url_expires_at DATETIME(6) NULL AFTER signing_url,
    ADD COLUMN rejected_at DATETIME(6) NULL AFTER signed_at,
    ADD COLUMN last_event_type VARCHAR(100) NULL AFTER rejected_at;

CREATE UNIQUE INDEX uk_clm_documents_modusign_document_id
    ON clm_documents(modusign_document_id);
