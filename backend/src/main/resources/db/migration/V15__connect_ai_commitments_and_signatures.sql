ALTER TABLE commitments
    ADD COLUMN commitment_type VARCHAR(50) NULL AFTER title,
    ADD COLUMN pledge_amount DECIMAL(15, 2) NULL AFTER commitment_type,
    ADD COLUMN pledge_frequency VARCHAR(30) NULL AFTER pledge_amount,
    ADD COLUMN renewal_due_at DATE NULL AFTER pledge_frequency,
    ADD COLUMN intent_snapshot TEXT NULL AFTER renewal_due_at;

ALTER TABLE clm_documents
    ADD COLUMN commitment_id BIGINT NULL AFTER id,
    ADD COLUMN signature_request_id BIGINT NULL AFTER commitment_id,
    ADD COLUMN last_event_rank INT NOT NULL DEFAULT 0 AFTER last_event_type,
    ADD CONSTRAINT fk_clm_documents_commitment
        FOREIGN KEY (commitment_id) REFERENCES commitments(id),
    ADD CONSTRAINT fk_clm_documents_signature_request
        FOREIGN KEY (signature_request_id) REFERENCES signature_requests(id);

CREATE UNIQUE INDEX uk_clm_documents_commitment_id
    ON clm_documents(commitment_id);
CREATE INDEX idx_commitments_renewal_due_at
    ON commitments(renewal_due_at, commitment_status);
