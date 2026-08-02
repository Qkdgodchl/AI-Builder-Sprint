-- 온기가 오른 내역. 하루 상한을 계산하고 "왜 올랐는지"를 되짚기 위해 남긴다.
CREATE TABLE warmth_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reason VARCHAR(40) NOT NULL,
    delta DECIMAL(4,2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_warmth_events_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_warmth_events_user_day ON warmth_events(user_id, created_at);
