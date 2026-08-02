ALTER TABLE clm_documents
    ADD COLUMN completion_message TEXT NULL AFTER last_event_rank,
    ADD COLUMN completion_message_source VARCHAR(30) NULL AFTER completion_message,
    ADD COLUMN completion_message_created_at DATETIME(6) NULL AFTER completion_message_source;
