-- 참여 1건에 대해 센터와 참여자가 각각 남기는 활동 기록.
-- 같은 표를 쓰되 author_type으로 누가 남긴 기록인지 구분한다.
CREATE TABLE activity_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    application_id BIGINT NOT NULL,
    author_type VARCHAR(20) NOT NULL,
    author_user_id BIGINT NOT NULL,
    activity_date DATE NOT NULL,
    content TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_activity_notes_public_id UNIQUE (public_id),
    CONSTRAINT fk_activity_notes_application
        FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_activity_notes_author
        FOREIGN KEY (author_user_id) REFERENCES users(id)
);

CREATE INDEX idx_activity_notes_application ON activity_notes(application_id, activity_date);
CREATE INDEX idx_activity_notes_author ON activity_notes(author_user_id, activity_date);

CREATE TABLE activity_note_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_note_id BIGINT NOT NULL,
    stored_file_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_activity_note_files_note
        FOREIGN KEY (activity_note_id) REFERENCES activity_notes(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_note_files_file
        FOREIGN KEY (stored_file_id) REFERENCES stored_files(id)
);

CREATE INDEX idx_activity_note_files_note ON activity_note_files(activity_note_id, display_order);
