-- =========================================================
-- Flyway Migration V9: CLM (전자서명 계약/신청 서류) 스키마 생성
-- 모두싸인(Modusign) API 연동 문서 보존
-- =========================================================

CREATE TABLE IF NOT EXISTS clm_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    volunteer_id BIGINT NOT NULL,
    volunteer_title VARCHAR(255) NOT NULL,
    applicant_user_id BIGINT NULL,
    applicant_name VARCHAR(100) NOT NULL,
    applicant_email VARCHAR(255) NOT NULL,
    applicant_phone VARCHAR(50) NULL,
    modusign_document_id VARCHAR(255) NULL,
    signing_url TEXT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_SIGNATURE',
    signed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_clm_documents_volunteer_id ON clm_documents(volunteer_id);
CREATE INDEX idx_clm_documents_applicant_email ON clm_documents(applicant_email);
