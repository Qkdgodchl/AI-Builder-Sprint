ALTER TABLE ai_consultations
    ADD COLUMN external_ai_consent_at DATETIME(6) NULL AFTER completed_at,
    ADD COLUMN external_ai_provider VARCHAR(50) NULL AFTER external_ai_consent_at;
