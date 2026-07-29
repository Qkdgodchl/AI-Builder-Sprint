-- Public URL / API identifiers
-- BIGINT primary keys remain internal-only for joins and indexes.
-- Public content keeps its normal positive numeric id.
-- Private workflow resources use UUID public_id to prevent predictable enumeration.

ALTER TABLE manager_applications
    ADD COLUMN public_id CHAR(36) NULL AFTER id;

UPDATE manager_applications
SET public_id = LOWER(UUID())
WHERE public_id IS NULL;

ALTER TABLE manager_applications
    MODIFY COLUMN public_id CHAR(36) NOT NULL;
ALTER TABLE manager_applications
    ADD CONSTRAINT uk_manager_applications_public_id UNIQUE (public_id);

ALTER TABLE organization_applications
    ADD COLUMN public_id CHAR(36) NULL AFTER id;

UPDATE organization_applications
SET public_id = LOWER(UUID())
WHERE public_id IS NULL;

ALTER TABLE organization_applications
    MODIFY COLUMN public_id CHAR(36) NOT NULL;
ALTER TABLE organization_applications
    ADD CONSTRAINT uk_org_applications_public_id UNIQUE (public_id);

ALTER TABLE applications
    ADD COLUMN public_id CHAR(36) NULL AFTER id;

UPDATE applications
SET public_id = LOWER(UUID())
WHERE public_id IS NULL;

ALTER TABLE applications
    MODIFY COLUMN public_id CHAR(36) NOT NULL;
ALTER TABLE applications
    ADD CONSTRAINT uk_applications_public_id UNIQUE (public_id);

ALTER TABLE commitments
    ADD COLUMN public_id CHAR(36) NULL AFTER id;

UPDATE commitments
SET public_id = LOWER(UUID())
WHERE public_id IS NULL;

ALTER TABLE commitments
    MODIFY COLUMN public_id CHAR(36) NOT NULL;
ALTER TABLE commitments
    ADD CONSTRAINT uk_commitments_public_id UNIQUE (public_id);

ALTER TABLE contract_documents
    ADD COLUMN public_id CHAR(36) NULL AFTER id;

UPDATE contract_documents
SET public_id = LOWER(UUID())
WHERE public_id IS NULL;

ALTER TABLE contract_documents
    MODIFY COLUMN public_id CHAR(36) NOT NULL;
ALTER TABLE contract_documents
    ADD CONSTRAINT uk_contract_documents_public_id UNIQUE (public_id);

ALTER TABLE signature_requests
    ADD COLUMN public_id CHAR(36) NULL AFTER id;

UPDATE signature_requests
SET public_id = LOWER(UUID())
WHERE public_id IS NULL;

ALTER TABLE signature_requests
    MODIFY COLUMN public_id CHAR(36) NOT NULL;
ALTER TABLE signature_requests
    ADD CONSTRAINT uk_signature_requests_public_id UNIQUE (public_id);

ALTER TABLE community_posts
    ADD COLUMN public_id CHAR(36) NULL AFTER id;

UPDATE community_posts
SET public_id = LOWER(UUID())
WHERE public_id IS NULL;

ALTER TABLE community_posts
    MODIFY COLUMN public_id CHAR(36) NOT NULL;
ALTER TABLE community_posts
    ADD CONSTRAINT uk_community_posts_public_id UNIQUE (public_id);
