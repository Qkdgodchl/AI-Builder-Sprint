-- =========================================================
-- Flyway Migration V8: 커뮤니티 게시글 댓글(comments) 테이블 생성
-- =========================================================

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    author_nickname VARCHAR(100) NOT NULL,
    author_badge VARCHAR(100) NOT NULL DEFAULT 'LV1_SEED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(255) NULL,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_comments_post_id_created_at ON comments(post_id, created_at);
