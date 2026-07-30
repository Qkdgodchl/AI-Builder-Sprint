CREATE TABLE access_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_access_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_access_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_access_tokens_user_expires
    ON access_tokens(user_id, expires_at);

ALTER TABLE manager_applications
    ADD COLUMN position VARCHAR(100) NULL AFTER organization_name,
    ADD COLUMN contact VARCHAR(30) NULL AFTER position,
    ADD COLUMN organization_type VARCHAR(50) NULL AFTER contact,
    ADD COLUMN planned_center_name VARCHAR(255) NULL AFTER business_registration_number;

ALTER TABLE organizations
    ADD COLUMN homepage_url VARCHAR(500) NULL AFTER description,
    ADD COLUMN can_issue_donation_receipt BOOLEAN NOT NULL DEFAULT FALSE AFTER homepage_url;

ALTER TABLE opportunities
    ADD COLUMN category VARCHAR(50) NULL AFTER opportunity_type,
    ADD COLUMN region VARCHAR(100) NULL AFTER description,
    ADD COLUMN participation_mode VARCHAR(30) NOT NULL DEFAULT 'OFFLINE' AFTER location,
    ADD COLUMN eligibility TEXT NULL AFTER activity_end_at,
    ADD COLUMN cancellation_policy TEXT NULL AFTER current_amount;

ALTER TABLE applications
    ADD COLUMN participation_date DATE NULL AFTER consultation_id,
    ADD COLUMN special_conditions TEXT NULL AFTER participation_date,
    ADD COLUMN privacy_consent BOOLEAN NOT NULL DEFAULT FALSE AFTER answers_json,
    ADD COLUMN third_party_consent BOOLEAN NOT NULL DEFAULT FALSE AFTER privacy_consent,
    ADD COLUMN portrait_consent BOOLEAN NOT NULL DEFAULT FALSE AFTER third_party_consent;
