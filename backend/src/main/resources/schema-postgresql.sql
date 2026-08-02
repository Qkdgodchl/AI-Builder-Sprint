-- =========================================================
-- PostgreSQL Database Schema Initialization (Spring Boot Native)
-- =========================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    nickname VARCHAR(100) NOT NULL,
    name VARCHAR(100) NULL,
    phone VARCHAR(30) NULL,
    birth_date DATE NULL,
    region VARCHAR(100) NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    temperature DOUBLE PRECISION NOT NULL DEFAULT 36.5,
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    privacy_consent_at TIMESTAMP NULL,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    deleted_by VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_interests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    interest VARCHAR(100) NULL,
    interest_code VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS access_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS stored_files (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NULL,
    storage_key VARCHAR(500) NULL,
    original_name VARCHAR(255) NULL,
    original_filename VARCHAR(255) NULL,
    stored_filename VARCHAR(255) NULL,
    file_path VARCHAR(500) NULL,
    size_bytes BIGINT NULL,
    file_size BIGINT NULL,
    content_type VARCHAR(100) NULL,
    mime_type VARCHAR(100) NULL,
    checksum VARCHAR(128) NULL,
    checksum_sha256 VARCHAR(128) NULL,
    file_purpose VARCHAR(50) NULL,
    category VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL,
    sender VARCHAR(30) NULL,
    message TEXT NULL,
    recommended_missions_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    deleted_by VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS volunteers (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    organizer VARCHAR(255) NULL,
    category VARCHAR(100) NULL,
    region VARCHAR(100) NULL,
    location VARCHAR(255) NULL,
    target_amount BIGINT NULL,
    current_amount BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS volunteer_tags (
    volunteer_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(255) NULL,
    author_nickname VARCHAR(100) NULL,
    author_badge VARCHAR(100) NULL,
    image_url VARCHAR(1000) NULL,
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    deleted_by VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_nickname VARCHAR(100) NULL,
    author_badge VARCHAR(100) NULL,
    content TEXT NOT NULL,
    parent_comment_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    deleted_by VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS post_likes (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 온기 상승분 컬럼 이름은 delta다. WarmthEvent 엔티티와 WarmthService가 함께 쓴다.
CREATE TABLE IF NOT EXISTS warmth_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    delta NUMERIC(4,2) NOT NULL,
    reason VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 이전 스키마로 만들어진 데이터베이스에는 쓰지 않는 amount 컬럼이 NOT NULL로 남아 있다.
-- 그대로 두면 온기 적립 INSERT가 매번 실패하고, PostgreSQL은 실패한 트랜잭션의
-- 나머지 명령을 모두 거부해 댓글·응원·요청 등록까지 함께 무너진다.
ALTER TABLE warmth_events DROP COLUMN IF EXISTS amount;

CREATE TABLE IF NOT EXISTS clm_documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NULL,
    commitment_id BIGINT NULL,
    volunteer_id BIGINT NULL,
    volunteer_title VARCHAR(255) NULL,
    applicant_user_id BIGINT NULL,
    applicant_name VARCHAR(100) NULL,
    applicant_email VARCHAR(255) NULL,
    applicant_phone VARCHAR(50) NULL,
    modusign_document_id VARCHAR(255) NULL,
    signing_url TEXT NULL,
    signing_method VARCHAR(50) NULL,
    status VARCHAR(50) NULL DEFAULT 'PENDING_SIGNATURE',
    signed_at TIMESTAMP NULL,
    last_event_type VARCHAR(100) NULL,
    last_event_rank INT DEFAULT 0,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    deleted_by VARCHAR(255) NULL
);

-- 컬럼 이름은 ClmDocumentFile 엔티티를 따른다.
CREATE TABLE IF NOT EXISTS clm_document_files (
    id BIGSERIAL PRIMARY KEY,
    clm_document_id BIGINT NOT NULL,
    file_type VARCHAR(40) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    deleted_by VARCHAR(100) NULL
);

-- 이전 스키마가 남긴 컬럼은 엔티티가 채우지 않는다.
-- NOT NULL로 남아 있으면 체결본 보관이 매번 실패한다.
ALTER TABLE clm_document_files DROP COLUMN IF EXISTS document_id;
ALTER TABLE clm_document_files DROP COLUMN IF EXISTS file_path;

CREATE TABLE IF NOT EXISTS organization_applications (
    id BIGSERIAL PRIMARY KEY,
    applicant_user_id BIGINT NULL,
    name VARCHAR(255) NULL,
    organization_type VARCHAR(50) NULL,
    registration_number VARCHAR(50) NULL,
    representative_name VARCHAR(100) NULL,
    phone VARCHAR(50) NULL,
    phone_number VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    description TEXT NULL,
    proof_file_id BIGINT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    rejection_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    source_application_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    organization_type VARCHAR(50) NULL,
    registration_number VARCHAR(50) NULL,
    representative_name VARCHAR(100) NULL,
    phone VARCHAR(50) NULL,
    phone_number VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    description TEXT NULL,
    logo_file_id BIGINT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'VERIFIED',
    organization_status VARCHAR(50) DEFAULT 'APPROVED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    deleted_by VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS organization_managers (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    manager_role VARCHAR(50) NOT NULL DEFAULT 'PRIMARY',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS manager_applications (
    id BIGSERIAL PRIMARY KEY,
    applicant_user_id BIGINT NULL,
    user_id BIGINT NULL,
    organization_id BIGINT NULL,
    organization_name VARCHAR(255) NULL,
    business_registration_number VARCHAR(50) NULL,
    contact_number VARCHAR(50) NULL,
    phone VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    proof_file_id BIGINT NULL,
    reason TEXT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    application_status VARCHAR(50) DEFAULT 'PENDING',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    rejection_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS manager_application_files (
    manager_application_id BIGINT NULL,
    application_id BIGINT NULL,
    file_id BIGINT NULL,
    stored_file_id BIGINT NULL,
    file_purpose VARCHAR(50) NULL
);

CREATE TABLE IF NOT EXISTS contract_templates (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NULL,
    opportunity_type VARCHAR(50) NULL,
    name VARCHAR(255) NULL,
    title VARCHAR(255) NULL,
    template_code VARCHAR(100) NULL,
    description TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS contract_template_versions (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NULL,
    version_no INT NOT NULL DEFAULT 1,
    schema_json TEXT NULL,
    body_template TEXT NULL,
    content TEXT NULL,
    change_note VARCHAR(500) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS opportunities (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(100) NULL,
    organization_id BIGINT NULL,
    template_id BIGINT NULL,
    opportunity_type VARCHAR(50) NULL,
    type VARCHAR(50) NULL,
    category VARCHAR(50) NULL,
    organizer VARCHAR(100) NULL,
    title VARCHAR(255) NULL,
    summary VARCHAR(1000) NULL,
    description TEXT NULL,
    region VARCHAR(50) NULL,
    location VARCHAR(500) NULL,
    participation_mode VARCHAR(50) DEFAULT 'OFFLINE',
    recruitment_start_at TIMESTAMP NULL,
    recruitment_end_at TIMESTAMP NULL,
    activity_start_at TIMESTAMP NULL,
    activity_end_at TIMESTAMP NULL,
    capacity INT NULL,
    target_amount BIGINT NULL,
    current_amount BIGINT NOT NULL DEFAULT 0,
    thumbnail_file_id BIGINT NULL,
    external_url VARCHAR(1000) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
    created_by BIGINT NULL,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    deleted_by VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS opportunity_required_documents (
    id BIGSERIAL PRIMARY KEY,
    opportunity_id BIGINT NULL,
    document_code VARCHAR(50) NULL,
    document_name VARCHAR(255) NULL,
    description VARCHAR(1000) NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS applications (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(100) NULL,
    opportunity_id BIGINT NULL,
    applicant_user_id BIGINT NULL,
    applicant_id BIGINT NULL,
    consultation_id BIGINT NULL,
    answers_json TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'APPROVED',
    application_status VARCHAR(30) DEFAULT 'APPROVED',
    participation_date DATE NULL,
    notes TEXT NULL,
    submitted_at TIMESTAMP NULL,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    rejection_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS commitments (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(100) NULL,
    application_id BIGINT NULL,
    opportunity_id BIGINT NULL,
    user_id BIGINT NULL,
    organization_id BIGINT NULL,
    current_version_no INT NOT NULL DEFAULT 1,
    commitment_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    status VARCHAR(50) DEFAULT 'ACTIVE',
    commitment_type VARCHAR(50) NULL,
    recurring_amount BIGINT NULL,
    pledge_amount DECIMAL(15, 2) NULL,
    pledge_frequency VARCHAR(30) NULL,
    renewal_due_at TIMESTAMP NULL,
    title VARCHAR(255) NULL,
    effective_from DATE NULL,
    effective_to DATE NULL,
    signed_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS commitment_versions (
    id BIGSERIAL PRIMARY KEY,
    commitment_id BIGINT NULL,
    version_no INT NOT NULL DEFAULT 1,
    version INT DEFAULT 1,
    template_version_id BIGINT NULL,
    terms_json TEXT NULL,
    rendered_content TEXT NULL,
    content TEXT NULL,
    change_summary VARCHAR(1000) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS consents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL,
    commitment_id BIGINT NULL,
    commitment_version_id BIGINT NULL,
    consent_type VARCHAR(50) NULL,
    policy_version VARCHAR(50) NULL,
    is_consented BOOLEAN DEFAULT TRUE,
    is_agreed BOOLEAN DEFAULT TRUE,
    consented_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    agreed_at TIMESTAMP NULL,
    withdrawn_at TIMESTAMP NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS contract_documents (
    id BIGSERIAL PRIMARY KEY,
    commitment_id BIGINT NULL,
    commitment_version_id BIGINT NULL,
    file_id BIGINT NULL,
    stored_file_id BIGINT NULL,
    file_path VARCHAR(500) NULL,
    document_type VARCHAR(50) NULL,
    document_status VARCHAR(30) DEFAULT 'GENERATED',
    checksum VARCHAR(128) NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS signature_requests (
    id BIGSERIAL PRIMARY KEY,
    commitment_id BIGINT NULL,
    commitment_version_id BIGINT NULL,
    requester_id BIGINT NULL,
    signer_user_id BIGINT NULL,
    provider VARCHAR(50) NULL,
    provider_request_id VARCHAR(255) NULL,
    signer_email VARCHAR(255) NULL,
    external_tx_id VARCHAR(255) NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    signature_status VARCHAR(30) DEFAULT 'PENDING',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    signed_at TIMESTAMP NULL,
    failed_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS signature_request_documents (
    signature_request_id BIGINT NULL,
    contract_document_id BIGINT NULL,
    document_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS processed_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NULL,
    external_event_id VARCHAR(255) NULL,
    event_id VARCHAR(255) NULL,
    event_type VARCHAR(100) NULL,
    payload_json TEXT NULL,
    processing_status VARCHAR(30) DEFAULT 'RECEIVED',
    processed_at TIMESTAMP NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS commitment_change_requests (
    id BIGSERIAL PRIMARY KEY,
    commitment_id BIGINT NULL,
    requested_by BIGINT NULL,
    requester_user_id BIGINT NULL,
    request_type VARCHAR(50) NULL,
    requested_changes_json TEXT NULL,
    reason TEXT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    decision_reason TEXT NULL,
    resulting_version_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS activity_records (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NULL,
    commitment_id BIGINT NULL,
    recorded_by BIGINT NULL,
    user_id BIGINT NULL,
    organization_id BIGINT NULL,
    opportunity_id BIGINT NULL,
    activity_type VARCHAR(50) NULL,
    activity_date DATE NULL,
    quantity DECIMAL(15, 2) NULL,
    unit VARCHAR(30) NULL,
    amount BIGINT NULL,
    hours INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'COMPLETED',
    description TEXT NULL,
    notes TEXT NULL,
    evidence_file_id BIGINT NULL,
    verification_status VARCHAR(30) DEFAULT 'PENDING',
    verified_by BIGINT NULL,
    verified_at TIMESTAMP NULL,
    rejection_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS community_posts (
    id BIGSERIAL PRIMARY KEY,
    author_user_id BIGINT NULL,
    author_id BIGINT NULL,
    organization_id BIGINT NULL,
    category VARCHAR(50) NULL,
    title VARCHAR(255) NULL,
    content TEXT NULL,
    visibility VARCHAR(30) DEFAULT 'PUBLIC',
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    deleted_by VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS community_post_images (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NULL,
    file_id BIGINT NULL,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS community_comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NULL,
    author_user_id BIGINT NULL,
    author_id BIGINT NULL,
    parent_comment_id BIGINT NULL,
    content TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    deleted_by VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS post_reactions (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NULL,
    user_id BIGINT NULL,
    reaction_type VARCHAR(50) DEFAULT 'LIKE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_user_id BIGINT NULL,
    target_type VARCHAR(50) NULL,
    target_id BIGINT NULL,
    reason_code VARCHAR(50) NULL,
    reason TEXT NULL,
    description TEXT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    handled_by BIGINT NULL,
    handled_at TIMESTAMP NULL,
    resolution_note TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NULL,
    admin_user_id BIGINT NULL,
    action_type VARCHAR(100) NULL,
    target_type VARCHAR(100) NULL,
    target_id VARCHAR(100) NULL,
    request_id VARCHAR(100) NULL,
    before_json TEXT NULL,
    after_json TEXT NULL,
    ip_address VARCHAR(45) NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL,
    notification_type VARCHAR(50) NULL,
    title VARCHAR(255) NULL,
    content TEXT NULL,
    message TEXT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_consultations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL,
    title VARCHAR(255) NULL,
    consultation_status VARCHAR(50) DEFAULT 'IN_PROGRESS',
    intent_summary TEXT NULL,
    extracted_preferences_json TEXT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    ended_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS ai_messages (
    id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NULL,
    sender_type VARCHAR(50) NULL,
    content TEXT NULL,
    metadata_json TEXT NULL,
    sequence_no INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS activity_notes (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NULL,
    user_id BIGINT NULL,
    note_text TEXT NULL,
    visibility VARCHAR(50) DEFAULT 'PRIVATE',
    is_private BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS activity_note_files (
    activity_note_id BIGINT NULL,
    stored_file_id BIGINT NULL,
    display_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS heritage_projects (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NULL,
    description TEXT NULL,
    target_amount BIGINT NULL,
    current_amount BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Safe column additions for pre-existing tables on PostgreSQL
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS phone VARCHAR(50);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS phone_number VARCHAR(50);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS verification_status VARCHAR(30) DEFAULT 'VERIFIED';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS source_application_id BIGINT;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS logo_file_id BIGINT;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255);

ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS id BIGSERIAL PRIMARY KEY;
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE stored_files ADD COLUMN IF NOT EXISTS storage_key VARCHAR(500);
ALTER TABLE stored_files ADD COLUMN IF NOT EXISTS original_name VARCHAR(255);
ALTER TABLE stored_files ADD COLUMN IF NOT EXISTS original_filename VARCHAR(255);
ALTER TABLE stored_files ADD COLUMN IF NOT EXISTS stored_filename VARCHAR(255);
ALTER TABLE stored_files ADD COLUMN IF NOT EXISTS file_path VARCHAR(500);
ALTER TABLE stored_files ADD COLUMN IF NOT EXISTS size_bytes BIGINT;
ALTER TABLE stored_files ADD COLUMN IF NOT EXISTS file_size BIGINT;
ALTER TABLE stored_files ADD COLUMN IF NOT EXISTS content_type VARCHAR(100);
ALTER TABLE stored_files ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100);
ALTER TABLE stored_files ADD COLUMN IF NOT EXISTS checksum VARCHAR(128);
ALTER TABLE stored_files ADD COLUMN IF NOT EXISTS file_purpose VARCHAR(50);

ALTER TABLE manager_applications ADD COLUMN IF NOT EXISTS applicant_user_id BIGINT;
ALTER TABLE manager_applications ADD COLUMN IF NOT EXISTS organization_id BIGINT;
ALTER TABLE manager_applications ADD COLUMN IF NOT EXISTS proof_file_id BIGINT;
ALTER TABLE manager_applications ADD COLUMN IF NOT EXISTS reason TEXT;

ALTER TABLE organization_applications ADD COLUMN IF NOT EXISTS applicant_user_id BIGINT;
ALTER TABLE organization_applications ADD COLUMN IF NOT EXISTS phone VARCHAR(50);
ALTER TABLE organization_applications ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE organization_applications ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE organization_applications ADD COLUMN IF NOT EXISTS proof_file_id BIGINT;

ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS public_id VARCHAR(100);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS template_id BIGINT;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS opportunity_type VARCHAR(50);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS summary VARCHAR(1000);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS participation_mode VARCHAR(50) DEFAULT 'OFFLINE';
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS recruitment_start_at TIMESTAMP;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS recruitment_end_at TIMESTAMP;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS activity_start_at TIMESTAMP;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS activity_end_at TIMESTAMP;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS thumbnail_file_id BIGINT;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS external_url VARCHAR(1000);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE applications ADD COLUMN IF NOT EXISTS public_id VARCHAR(100);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS applicant_user_id BIGINT;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS applicant_id BIGINT;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS consultation_id BIGINT;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS answers_json TEXT;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS participation_date DATE;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS reviewed_by BIGINT;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

ALTER TABLE commitments ADD COLUMN IF NOT EXISTS public_id VARCHAR(100);
ALTER TABLE commitments ADD COLUMN IF NOT EXISTS opportunity_id BIGINT;
ALTER TABLE commitments ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE commitments ADD COLUMN IF NOT EXISTS organization_id BIGINT;
ALTER TABLE commitments ADD COLUMN IF NOT EXISTS current_version_no INT DEFAULT 1;
ALTER TABLE commitments ADD COLUMN IF NOT EXISTS commitment_status VARCHAR(30) DEFAULT 'ACTIVE';
ALTER TABLE commitments ADD COLUMN IF NOT EXISTS effective_from DATE;
ALTER TABLE commitments ADD COLUMN IF NOT EXISTS effective_to DATE;
ALTER TABLE commitments ADD COLUMN IF NOT EXISTS pledge_amount DECIMAL(15, 2);
ALTER TABLE commitments ADD COLUMN IF NOT EXISTS pledge_frequency VARCHAR(30);
ALTER TABLE commitments ADD COLUMN IF NOT EXISTS renewal_due_at TIMESTAMP;

ALTER TABLE commitment_versions ADD COLUMN IF NOT EXISTS version_no INT DEFAULT 1;
ALTER TABLE commitment_versions ADD COLUMN IF NOT EXISTS terms_json TEXT;
ALTER TABLE commitment_versions ADD COLUMN IF NOT EXISTS rendered_content TEXT;
ALTER TABLE commitment_versions ADD COLUMN IF NOT EXISTS created_by BIGINT;

ALTER TABLE clm_documents ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE clm_documents ADD COLUMN IF NOT EXISTS commitment_id BIGINT;
ALTER TABLE clm_documents ADD COLUMN IF NOT EXISTS volunteer_id BIGINT;
ALTER TABLE clm_documents ADD COLUMN IF NOT EXISTS volunteer_title VARCHAR(255);
ALTER TABLE clm_documents ADD COLUMN IF NOT EXISTS applicant_user_id BIGINT;
ALTER TABLE clm_documents ADD COLUMN IF NOT EXISTS applicant_name VARCHAR(100);
ALTER TABLE clm_documents ADD COLUMN IF NOT EXISTS applicant_email VARCHAR(255);
ALTER TABLE clm_documents ADD COLUMN IF NOT EXISTS modusign_document_id VARCHAR(255);
ALTER TABLE clm_documents ADD COLUMN IF NOT EXISTS signing_method VARCHAR(50);
ALTER TABLE clm_documents ADD COLUMN IF NOT EXISTS signed_at TIMESTAMP;
ALTER TABLE clm_documents ADD COLUMN IF NOT EXISTS last_event_type VARCHAR(100);
ALTER TABLE clm_documents ADD COLUMN IF NOT EXISTS last_event_rank INT DEFAULT 0;


-- =========================================================
-- MySQL 스키마와 대조해 빠져 있던 컬럼 보정
-- 코드가 읽는데 표에 없으면 "column does not exist"로 조회가 통째로 실패한다.
-- 이미 있으면 넘어가므로 여러 번 실행해도 안전하다.
-- =========================================================

-- 신청: 동의 항목과 특별 조건
ALTER TABLE applications ADD COLUMN IF NOT EXISTS special_conditions TEXT;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS privacy_consent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS third_party_consent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS portrait_consent BOOLEAN NOT NULL DEFAULT FALSE;

-- 약정: AI가 정리한 의사 스냅샷
ALTER TABLE commitments ADD COLUMN IF NOT EXISTS intent_snapshot TEXT;

-- 프로그램: 신청 자격과 취소 규정
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS eligibility TEXT;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS cancellation_policy TEXT;

-- 기관: 홈페이지와 기부금 영수증 발급 가능 여부
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS homepage_url VARCHAR(500);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS can_issue_donation_receipt BOOLEAN NOT NULL DEFAULT FALSE;

-- AI 상담: 외부 AI 사용 동의 기록
ALTER TABLE ai_consultations ADD COLUMN IF NOT EXISTS external_ai_provider VARCHAR(50);
ALTER TABLE ai_consultations ADD COLUMN IF NOT EXISTS external_ai_consent_at TIMESTAMP;

-- 센터 관리자 신청
ALTER TABLE manager_applications ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE manager_applications ADD COLUMN IF NOT EXISTS position VARCHAR(100);
ALTER TABLE manager_applications ADD COLUMN IF NOT EXISTS contact VARCHAR(30);
ALTER TABLE manager_applications ADD COLUMN IF NOT EXISTS organization_type VARCHAR(50);
ALTER TABLE manager_applications ADD COLUMN IF NOT EXISTS planned_center_name VARCHAR(255);
ALTER TABLE manager_application_files ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE manager_application_files ADD COLUMN IF NOT EXISTS document_type VARCHAR(50) NOT NULL DEFAULT 'EVIDENCE';
ALTER TABLE manager_application_files ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 공개 식별자 (URL에 노출되는 값이라 조회 조건으로 쓰인다)
ALTER TABLE organization_applications ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE signature_requests ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE contract_documents ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE community_posts ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);

-- 활동 다이어리
ALTER TABLE activity_notes ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE activity_notes ADD COLUMN IF NOT EXISTS author_type VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE activity_notes ADD COLUMN IF NOT EXISTS author_user_id BIGINT;
ALTER TABLE activity_notes ADD COLUMN IF NOT EXISTS activity_date DATE;
ALTER TABLE activity_notes ADD COLUMN IF NOT EXISTS content TEXT;
ALTER TABLE activity_notes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE activity_notes ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE activity_note_files ADD COLUMN IF NOT EXISTS id BIGSERIAL;

-- 유산·문화유산 후원 프로그램
ALTER TABLE heritage_projects ADD COLUMN IF NOT EXISTS project_code VARCHAR(100);
ALTER TABLE heritage_projects ADD COLUMN IF NOT EXISTS category VARCHAR(50);
ALTER TABLE heritage_projects ADD COLUMN IF NOT EXISTS heritage_name VARCHAR(255);
ALTER TABLE heritage_projects ADD COLUMN IF NOT EXISTS organizer_name VARCHAR(100);
ALTER TABLE heritage_projects ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000);
ALTER TABLE heritage_projects ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE heritage_projects ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
