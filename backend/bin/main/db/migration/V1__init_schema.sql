-- =========================================================
-- Flyway Migration V1: 픽셀 케어 (Pixel Care) 데이터베이스 초판 스키마
-- =========================================================

-- 1. 봉사 & 기부 펀딩 테이블 (volunteers)
CREATE TABLE IF NOT EXISTS volunteers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'VOLUNTEER',
    location VARCHAR(255) NOT NULL,
    organizer VARCHAR(255) NOT NULL,
    target_amount BIGINT NULL,
    current_amount BIGINT DEFAULT 0,
    link1365 VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1-1. 봉사 태그 테이블 (volunteer_tags)
CREATE TABLE IF NOT EXISTS volunteer_tags (
    volunteer_id BIGINT NOT NULL,
    tag VARCHAR(255) NOT NULL,
    FOREIGN KEY (volunteer_id) REFERENCES volunteers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 커뮤니티 게시판 테이블 (posts)
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    likes INT NOT NULL DEFAULT 0,
    views INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
