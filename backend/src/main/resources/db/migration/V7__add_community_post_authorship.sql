ALTER TABLE posts
    ADD COLUMN author_user_id BIGINT NULL AFTER id;

ALTER TABLE posts
    ADD CONSTRAINT fk_posts_author_user
        FOREIGN KEY (author_user_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_posts_author_user_deleted
    ON posts(author_user_id, is_deleted);
