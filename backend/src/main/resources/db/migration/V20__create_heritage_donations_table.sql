-- Flyway Migration V20: 유산기부 및 문화유산(유네스코) 후원 DB 구축
-- V18은 활동 다이어리가 이미 쓰고 있어 다음 빈 번호로 옮겼다.
CREATE TABLE heritage_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_code VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL, -- UNESCO, NATIONAL_HERITAGE, LOCAL_HERITAGE, BEQUEST
    heritage_name VARCHAR(255) NOT NULL,
    organizer_name VARCHAR(100) NOT NULL,
    target_amount BIGINT NOT NULL DEFAULT 100000000,
    current_amount BIGINT NOT NULL DEFAULT 35000000,
    description TEXT NOT NULL,
    image_url VARCHAR(1000) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL,
    CONSTRAINT uk_heritage_projects_code UNIQUE (project_code)
);

CREATE INDEX idx_heritage_projects_category ON heritage_projects(category, is_active);

-- 문화유산 & 유네스코 후원 및 유산기부 시드 데이터
INSERT INTO heritage_projects (
    project_code, title, category, heritage_name, organizer_name, target_amount, current_amount, description, image_url
) VALUES 
('HERITAGE_BEOMEOSA', '🏛️ [유네스코/국보] 부산 범어사 삼층석탑 영구 보존 후원', 'NATIONAL_HERITAGE', '부산 범어사 삼층석탑 (보물 제250호)', '범어사 문화유산 보존회', 100000000, 42000000, '신라 문무왕 연간 양식의 국보급 석탑 구조 보강 및 풍화 방지 영구 보존 기금 약정', 'https://images.unsplash.com/photo-1548013146-72479768bada?auto=format&fit=crop&w=600&q=80'),

('HERITAGE_TRIPITAKA', '📜 [유네스코 세계기록유산] 팔만대장경 디지털 아카이빙', 'UNESCO', '합천 해인사 대장경판 (유네스코 세계기록유산)', '국가유산청 & 해인사', 500000000, 185000000, '팔만대장경 81,258판의 고화질 3D 디지털 정밀 아카이빙 및 글로벌 가상 전시관 구축 약정', 'https://images.unsplash.com/photo-1579783902614-a3fb3927b675?auto=format&fit=crop&w=600&q=80'),

('HERITAGE_PRESIDENTIAL', '🏡 [부산 근대문화유산] 임시수도기념관 지킴이 후원', 'LOCAL_HERITAGE', '부산 임시수도 대통령관저 (사적 제546호)', '부산광역시 박물관', 50000000, 21000000, '한국전쟁기 임시수도 부산의 역사적 대통령관저 목조건축물 보존 및 전시 아카이브 약정', 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=600&q=80'),

('HERITAGE_BEQUEST_ART', '🎨 [유산기부·유증] 부산 근현대 미술품 및 문화재 현물 기증', 'BEQUEST', '부산 지역 근현대 지정 문화재 및 소장 미술품', '부산시립미술관 & 문화유산 재단', 1000000000, 650000000, '소장 문화재 및 예술품의 사후 유증 기부 공증 및 법적 유산기부 체결 약정', 'https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=600&q=80');
