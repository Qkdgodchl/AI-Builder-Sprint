-- =========================================================
-- Flyway Migration V2: CLM 기반 선행 플랫폼 전체 스키마
-- =========================================================
-- V1의 레거시 테이블은 유지하고, 현재 JPA 엔티티와 신규 도메인
-- 스키마를 함께 사용할 수 있도록 확장한다.

-- ---------------------------------------------------------
-- 0. V1 레거시 posts를 현재 JPA Post 엔티티와 호환
-- ---------------------------------------------------------
ALTER TABLE posts ALTER COLUMN author SET DEFAULT '';
ALTER TABLE posts ADD COLUMN author_nickname VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE posts ADD COLUMN author_badge VARCHAR(100) NULL;
ALTER TABLE posts ADD COLUMN image_url VARCHAR(1000) NULL;
ALTER TABLE posts ADD COLUMN like_count INT NOT NULL DEFAULT 0;
ALTER TABLE posts ADD COLUMN comment_count INT NOT NULL DEFAULT 0;
ALTER TABLE posts ADD COLUMN view_count INT NOT NULL DEFAULT 0;
ALTER TABLE posts ADD COLUMN updated_at DATETIME(6) NULL;
ALTER TABLE posts ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE posts ADD COLUMN deleted_at DATETIME(6) NULL;
ALTER TABLE posts ADD COLUMN deleted_by VARCHAR(255) NULL;

CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_nickname VARCHAR(100) NOT NULL,
    author_badge VARCHAR(100) NULL,
    content TEXT NOT NULL,
    parent_comment_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(255) NULL,
    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE SET NULL
);

CREATE INDEX idx_comments_post_created
    ON comments(post_id, created_at);

-- ---------------------------------------------------------
-- 1. 사용자·인증
-- ---------------------------------------------------------
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NULL,
    nickname VARCHAR(100) NOT NULL,
    name VARCHAR(100) NULL,
    phone VARCHAR(30) NULL,
    birth_date DATE NULL,
    region VARCHAR(100) NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    temperature DOUBLE NOT NULL DEFAULT 36.5,
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    privacy_consent_at DATETIME(6) NULL,
    last_login_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(255) NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_status ON users(account_status);

CREATE TABLE user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_interests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    interest_code VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_interests_user_code UNIQUE (user_id, interest_code),
    CONSTRAINT fk_user_interests_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_expires
    ON refresh_tokens(user_id, expires_at);

CREATE TABLE stored_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(128) NULL,
    file_purpose VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT uk_stored_files_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_stored_files_owner
        FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 현재 AI 채팅 엔티티 호환용
CREATE TABLE chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    sender VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    recommended_missions_json TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(255) NULL
);

CREATE INDEX idx_chat_messages_user_created
    ON chat_messages(user_id, created_at);

-- ---------------------------------------------------------
-- 2. 기관·관리자 승인
-- ---------------------------------------------------------
CREATE TABLE manager_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_user_id BIGINT NOT NULL,
    organization_name VARCHAR(255) NOT NULL,
    business_registration_number VARCHAR(50) NULL,
    proof_file_id BIGINT NULL,
    reason TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    rejection_reason TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_manager_applications_user
        FOREIGN KEY (applicant_user_id) REFERENCES users(id),
    CONSTRAINT fk_manager_applications_file
        FOREIGN KEY (proof_file_id) REFERENCES stored_files(id) ON DELETE SET NULL,
    CONSTRAINT fk_manager_applications_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_manager_applications_status_created
    ON manager_applications(status, created_at);

CREATE TABLE organization_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    organization_type VARCHAR(50) NOT NULL,
    registration_number VARCHAR(50) NULL,
    representative_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    description TEXT NULL,
    proof_file_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    rejection_reason TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_org_applications_user
        FOREIGN KEY (applicant_user_id) REFERENCES users(id),
    CONSTRAINT fk_org_applications_file
        FOREIGN KEY (proof_file_id) REFERENCES stored_files(id) ON DELETE SET NULL,
    CONSTRAINT fk_org_applications_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_org_applications_status_created
    ON organization_applications(status, created_at);

CREATE TABLE organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_application_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    organization_type VARCHAR(50) NOT NULL,
    registration_number VARCHAR(50) NULL,
    representative_name VARCHAR(100) NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    description TEXT NULL,
    logo_file_id BIGINT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'VERIFIED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(255) NULL,
    CONSTRAINT uk_organizations_registration_number UNIQUE (registration_number),
    CONSTRAINT fk_organizations_source_application
        FOREIGN KEY (source_application_id) REFERENCES organization_applications(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_organizations_logo
        FOREIGN KEY (logo_file_id) REFERENCES stored_files(id) ON DELETE SET NULL
);

CREATE TABLE organization_managers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    manager_role VARCHAR(30) NOT NULL DEFAULT 'MANAGER',
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at DATETIME(6) NULL,
    CONSTRAINT uk_organization_managers_org_user
        UNIQUE (organization_id, user_id),
    CONSTRAINT fk_organization_managers_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_organization_managers_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------
-- 3. AI 상담·계약 템플릿
-- ---------------------------------------------------------
CREATE TABLE ai_consultations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NULL,
    consultation_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    intent_summary TEXT NULL,
    extracted_preferences_json TEXT NULL,
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_ai_consultations_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_consultations_user_created
    ON ai_consultations(user_id, created_at);

CREATE TABLE ai_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    sender_type VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    metadata_json TEXT NULL,
    sequence_no INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_messages_consultation_sequence
        UNIQUE (consultation_id, sequence_no),
    CONSTRAINT fk_ai_messages_consultation
        FOREIGN KEY (consultation_id) REFERENCES ai_consultations(id)
        ON DELETE CASCADE
);

CREATE TABLE contract_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NULL,
    opportunity_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_contract_templates_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL,
    CONSTRAINT fk_contract_templates_creator
        FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE contract_template_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    schema_json TEXT NOT NULL,
    body_template TEXT NOT NULL,
    change_note VARCHAR(500) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_contract_template_versions_template_version
        UNIQUE (template_id, version_no),
    CONSTRAINT fk_contract_template_versions_template
        FOREIGN KEY (template_id) REFERENCES contract_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_contract_template_versions_creator
        FOREIGN KEY (created_by) REFERENCES users(id)
);

-- ---------------------------------------------------------
-- 4. 선행 기회·신청
-- ---------------------------------------------------------
CREATE TABLE opportunities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    template_id BIGINT NULL,
    opportunity_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(1000) NULL,
    description TEXT NOT NULL,
    location VARCHAR(500) NULL,
    recruitment_start_at DATETIME(6) NULL,
    recruitment_end_at DATETIME(6) NULL,
    activity_start_at DATETIME(6) NULL,
    activity_end_at DATETIME(6) NULL,
    capacity INT NULL,
    target_amount BIGINT NULL,
    current_amount BIGINT NOT NULL DEFAULT 0,
    thumbnail_file_id BIGINT NULL,
    external_url VARCHAR(1000) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(255) NULL,
    CONSTRAINT fk_opportunities_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_opportunities_template
        FOREIGN KEY (template_id) REFERENCES contract_templates(id) ON DELETE SET NULL,
    CONSTRAINT fk_opportunities_thumbnail
        FOREIGN KEY (thumbnail_file_id) REFERENCES stored_files(id) ON DELETE SET NULL,
    CONSTRAINT fk_opportunities_creator
        FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_opportunities_type_status_created
    ON opportunities(opportunity_type, status, created_at);
CREATE INDEX idx_opportunities_org_status
    ON opportunities(organization_id, status);

CREATE TABLE opportunity_required_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    opportunity_id BIGINT NOT NULL,
    document_code VARCHAR(50) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_opportunity_required_docs_code
        UNIQUE (opportunity_id, document_code),
    CONSTRAINT fk_opportunity_required_docs_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES opportunities(id)
        ON DELETE CASCADE
);

CREATE TABLE applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    opportunity_id BIGINT NOT NULL,
    applicant_user_id BIGINT NOT NULL,
    consultation_id BIGINT NULL,
    answers_json TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME(6) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    rejection_reason TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    CONSTRAINT uk_applications_opportunity_applicant
        UNIQUE (opportunity_id, applicant_user_id),
    CONSTRAINT fk_applications_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES opportunities(id),
    CONSTRAINT fk_applications_applicant
        FOREIGN KEY (applicant_user_id) REFERENCES users(id),
    CONSTRAINT fk_applications_consultation
        FOREIGN KEY (consultation_id) REFERENCES ai_consultations(id) ON DELETE SET NULL,
    CONSTRAINT fk_applications_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_applications_user_status
    ON applications(applicant_user_id, status);
CREATE INDEX idx_applications_opportunity_status
    ON applications(opportunity_id, status);

-- ---------------------------------------------------------
-- 5. CLM 약정·동의·전자서명
-- ---------------------------------------------------------
CREATE TABLE commitments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    opportunity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    current_version_no INT NOT NULL DEFAULT 1,
    commitment_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    title VARCHAR(255) NOT NULL,
    effective_from DATE NULL,
    effective_to DATE NULL,
    signed_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    CONSTRAINT uk_commitments_application UNIQUE (application_id),
    CONSTRAINT fk_commitments_application
        FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_commitments_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES opportunities(id),
    CONSTRAINT fk_commitments_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_commitments_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE INDEX idx_commitments_user_status
    ON commitments(user_id, commitment_status);
CREATE INDEX idx_commitments_org_status
    ON commitments(organization_id, commitment_status);

CREATE TABLE commitment_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commitment_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    template_version_id BIGINT NULL,
    terms_json TEXT NOT NULL,
    rendered_content TEXT NOT NULL,
    change_summary VARCHAR(1000) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_commitment_versions_commitment_version
        UNIQUE (commitment_id, version_no),
    CONSTRAINT fk_commitment_versions_commitment
        FOREIGN KEY (commitment_id) REFERENCES commitments(id) ON DELETE CASCADE,
    CONSTRAINT fk_commitment_versions_template_version
        FOREIGN KEY (template_version_id) REFERENCES contract_template_versions(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_commitment_versions_creator
        FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE consents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    commitment_id BIGINT NULL,
    consent_type VARCHAR(50) NOT NULL,
    policy_version VARCHAR(50) NOT NULL,
    is_agreed BOOLEAN NOT NULL,
    agreed_at DATETIME(6) NULL,
    withdrawn_at DATETIME(6) NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_consents_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_consents_commitment
        FOREIGN KEY (commitment_id) REFERENCES commitments(id) ON DELETE SET NULL
);

CREATE INDEX idx_consents_user_type_created
    ON consents(user_id, consent_type, created_at);

CREATE TABLE contract_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commitment_id BIGINT NOT NULL,
    commitment_version_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_status VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    checksum VARCHAR(128) NULL,
    generated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_documents_commitment
        FOREIGN KEY (commitment_id) REFERENCES commitments(id) ON DELETE CASCADE,
    CONSTRAINT fk_contract_documents_version
        FOREIGN KEY (commitment_version_id) REFERENCES commitment_versions(id),
    CONSTRAINT fk_contract_documents_file
        FOREIGN KEY (file_id) REFERENCES stored_files(id)
);

CREATE TABLE signature_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commitment_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_request_id VARCHAR(255) NULL,
    signer_user_id BIGINT NOT NULL,
    signer_email VARCHAR(255) NOT NULL,
    signature_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    requested_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME(6) NULL,
    signed_at DATETIME(6) NULL,
    failed_reason TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    CONSTRAINT uk_signature_requests_provider_id
        UNIQUE (provider, provider_request_id),
    CONSTRAINT fk_signature_requests_commitment
        FOREIGN KEY (commitment_id) REFERENCES commitments(id) ON DELETE CASCADE,
    CONSTRAINT fk_signature_requests_signer
        FOREIGN KEY (signer_user_id) REFERENCES users(id)
);

CREATE INDEX idx_signature_requests_commitment_status
    ON signature_requests(commitment_id, signature_status);

CREATE TABLE signature_request_documents (
    signature_request_id BIGINT NOT NULL,
    contract_document_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (signature_request_id, contract_document_id),
    CONSTRAINT fk_signature_request_docs_request
        FOREIGN KEY (signature_request_id) REFERENCES signature_requests(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_signature_request_docs_document
        FOREIGN KEY (contract_document_id) REFERENCES contract_documents(id)
        ON DELETE CASCADE
);

CREATE TABLE processed_webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    external_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    processed_at DATETIME(6) NULL,
    error_message TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_webhook_events_provider_external
        UNIQUE (provider, external_event_id)
);

CREATE TABLE commitment_change_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commitment_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    request_type VARCHAR(30) NOT NULL,
    requested_changes_json TEXT NOT NULL,
    reason TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    decision_reason TEXT NULL,
    resulting_version_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_commitment_changes_commitment
        FOREIGN KEY (commitment_id) REFERENCES commitments(id) ON DELETE CASCADE,
    CONSTRAINT fk_commitment_changes_requester
        FOREIGN KEY (requested_by) REFERENCES users(id),
    CONSTRAINT fk_commitment_changes_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_commitment_changes_result_version
        FOREIGN KEY (resulting_version_id) REFERENCES commitment_versions(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_commitment_changes_commitment_status
    ON commitment_change_requests(commitment_id, status);

-- ---------------------------------------------------------
-- 6. 이행 기록
-- ---------------------------------------------------------
CREATE TABLE activity_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commitment_id BIGINT NOT NULL,
    recorded_by BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    activity_date DATE NOT NULL,
    quantity DECIMAL(15, 2) NULL,
    unit VARCHAR(30) NULL,
    amount BIGINT NULL,
    description TEXT NULL,
    evidence_file_id BIGINT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verified_by BIGINT NULL,
    verified_at DATETIME(6) NULL,
    rejection_reason TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_activity_records_commitment
        FOREIGN KEY (commitment_id) REFERENCES commitments(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_records_recorder
        FOREIGN KEY (recorded_by) REFERENCES users(id),
    CONSTRAINT fk_activity_records_evidence
        FOREIGN KEY (evidence_file_id) REFERENCES stored_files(id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_records_verifier
        FOREIGN KEY (verified_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_activity_records_commitment_date
    ON activity_records(commitment_id, activity_date);

-- ---------------------------------------------------------
-- 7. 신규 커뮤니티
-- ---------------------------------------------------------
CREATE TABLE community_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_user_id BIGINT NOT NULL,
    organization_id BIGINT NULL,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(255) NULL,
    CONSTRAINT fk_community_posts_author
        FOREIGN KEY (author_user_id) REFERENCES users(id),
    CONSTRAINT fk_community_posts_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL
);

CREATE INDEX idx_community_posts_category_created
    ON community_posts(category, created_at);

CREATE TABLE community_post_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_community_post_images_order UNIQUE (post_id, display_order),
    CONSTRAINT fk_community_post_images_post
        FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_post_images_file
        FOREIGN KEY (file_id) REFERENCES stored_files(id)
);

CREATE TABLE community_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_user_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(255) NULL,
    CONSTRAINT fk_community_comments_post
        FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_comments_author
        FOREIGN KEY (author_user_id) REFERENCES users(id),
    CONSTRAINT fk_community_comments_parent
        FOREIGN KEY (parent_comment_id) REFERENCES community_comments(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_community_comments_post_created
    ON community_comments(post_id, created_at);

CREATE TABLE post_reactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(30) NOT NULL DEFAULT 'LIKE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_post_reactions_post_user_type
        UNIQUE (post_id, user_id, reaction_type),
    CONSTRAINT fk_post_reactions_post
        FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_reactions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_user_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    description TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    handled_by BIGINT NULL,
    handled_at DATETIME(6) NULL,
    resolution_note TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reports_reporter
        FOREIGN KEY (reporter_user_id) REFERENCES users(id),
    CONSTRAINT fk_reports_handler
        FOREIGN KEY (handled_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_reports_status_created
    ON reports(status, created_at);

-- ---------------------------------------------------------
-- 8. 운영 감사·알림
-- ---------------------------------------------------------
CREATE TABLE admin_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NULL,
    action_type VARCHAR(100) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id VARCHAR(100) NULL,
    request_id VARCHAR(100) NULL,
    before_json TEXT NULL,
    after_json TEXT NULL,
    ip_address VARCHAR(45) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_audit_logs_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_admin_audit_logs_target_created
    ON admin_audit_logs(target_type, target_id, created_at);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_read_created
    ON notifications(user_id, is_read, created_at);
