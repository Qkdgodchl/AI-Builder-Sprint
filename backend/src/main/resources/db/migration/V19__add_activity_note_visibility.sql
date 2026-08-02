-- 참여자가 남기는 기록 중 센터에 공유하지 않는 개인 메모를 구분한다.
ALTER TABLE activity_notes
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'SHARED' AFTER author_user_id;

CREATE INDEX idx_activity_notes_visibility ON activity_notes(application_id, visibility);
