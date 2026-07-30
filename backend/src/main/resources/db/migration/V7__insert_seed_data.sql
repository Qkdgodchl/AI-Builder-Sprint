-- =========================================================
-- Flyway Migration V4: 시연 및 Git 공유용 초기 회원 & 데이터베이스 보존 SQL
-- =========================================================

-- 1. 초기 시연 및 테스트 계정 데이터 보존
INSERT INTO users (email, password_hash, nickname, role, temperature, account_status)
VALUES 
  ('user@pixelcare.com', '$2a$10$abcdefghijklmnopqrstuv', '픽셀용사', 'USER', 36.5, 'ACTIVE'),
  ('center@pixelcare.com', '$2a$10$abcdefghijklmnopqrstuv', '행복복지센터', 'CENTER_MANAGER', 42.0, 'ACTIVE'),
  ('operator@pixelcare.com', '$2a$10$abcdefghijklmnopqrstuv', '총괄운영자', 'OPERATOR', 99.9, 'ACTIVE'),
  ('manager@pixelcare.demo', '$2a$10$abcdefghijklmnopqrstuv', '데모 센터 관리자', 'CENTER_MANAGER', 45.5, 'ACTIVE');

-- 2. 봉사 및 기부 데이터 보존
INSERT INTO volunteers (title, category, location, organizer, target_amount, current_amount, link1365)
VALUES
  ('🐕 부산 북구 유기견 보육원 주말 봉사', 'VOLUNTEER', '부산 북구 동물보호센터', '부산 동네 온기 봉사단', NULL, 0, NULL),
  ('🌊 해운대 해변 픽셀 플로깅 정화 활동', 'VOLUNTEER', '부산 해운대 구남로 광장', '그린 픽셀 에코 클럽', NULL, 0, NULL),
  ('🍲 금정구 독거어르신 온기 도시락 배달', 'VOLUNTEER', '부산 금정구 종합복지관', '사랑의 픽셀 이웃', NULL, 0, NULL),
  ('📚 사상구 꿈나무 차상위 아동 학습 지도', 'VOLUNTEER', '부산 사상구 지역아동센터', '픽셀 에듀 봉사단', NULL, 0, NULL),
  ('👵 남구 노인복지관 주말 말벗 및 장기 도우미', 'VOLUNTEER', '부산 남구 노인복지관', '부산 따뜻한 온기 동행', NULL, 0, NULL),
  ('🌱 동래구 수영강 환경 생태 보존 시민 봉사', 'VOLUNTEER', '부산 동래구 수영강 변 산책로', '수영강 생태 지킴이', NULL, 0, NULL),
  ('❤️ 저소득층 희귀질환 환아 치료비 정기후원', 'GENERAL', '전국', '픽셀케어 파트너 재단', 10000000, 4250000, NULL),
  ('🍲 결식아동 주말 온기 밥상 지원 펀딩', 'GENERAL', '부산광역시 전역', '사랑의 나눔 기금', 5000000, 3180000, NULL),
  ('🏠 주거 취약계층 온기 방한용품 나눔 후원', 'GENERAL', '부산 동구 초량동', '동구 희망나눔 센터', 3000000, 2100000, NULL),
  ('📜 나눔을 이어가는 미래세대 유산기부 전문상담', 'LEGACY', '전국', '유산기부 전문 상담센터', NULL, 0, NULL),
  ('🏛️ 세대를 넘어 전달되는 아동복지 유산 약정', 'LEGACY', '전국', '한국 아동복지 유산재단', NULL, 0, NULL),
  ('🌏 기후위기 대응 긴급 구호 유네스코 펀딩', 'UNESCO', '전 세계', '세계유산 보존 파트너', 20000000, 15400000, NULL),
  ('📚 분쟁지역 어린이 평화 교육 유네스코 사업', 'UNESCO', '전 세계', '유네스코 평화 교육위원회', 15000000, 8900000, NULL),
  ('🏛️ 부산 범어사 목조 아미타여래좌상 보존 후원', 'HERITAGE', '부산 금정구 범어사', '지역 문화유산 센터', 8000000, 5200000, NULL),
  ('🏯 영도 동삼동 패총 유적지 보존 및 교육 후원', 'HERITAGE', '부산 영도구 동삼동', '부산 선사문화 보존회', 5000000, 3400000, NULL),
  ('🌾 부산광역시 고향사랑기부제 (지역 상생 답례품)', 'HOMETOWN', '부산광역시', '지역 상생 기부 안내센터', NULL, 0, NULL),
  ('⚓ 부산 영도구 고향사랑기부 (해양 환경 상생 기금)', 'HOMETOWN', '부산 영도구', '영도구 고향사랑 기부팀', NULL, 0, NULL);

-- 3. 기본 시연 커뮤니티 데이터 보존
INSERT INTO community_posts (public_id, author_user_id, category, title, content, visibility, like_count, comment_count, view_count)
VALUES 
  (LOWER(UUID()), 1, 'REVIEW', '🌊 해운대 플로깅 봉사 함께 다녀왔어요!', '오늘 주말에 해운대 해변 플로깅 봉사를 신청해서 다녀왔습니다! 쓰레기 3kg이나 줍고 바다가 깨끗해져서 뿌듯하네요. 같이 봉사하신 용사님들 모두 수고 많으셨습니다!', 'PUBLIC', 5, 2, 42),
  (LOWER(UUID()), 2, 'REVIEW', '🍲 금정구 어르신 도시락 배달 후기 및 꿀팁 공유', '어르신들께 도시락 전달해드리면서 따뜻한 말씀 나누고 오니 마음까지 따뜻해지네요. 엘리베이터 없는 건물 계단 오르내릴 때 편한 운동화 필수입니다!', 'PUBLIC', 12, 4, 88),
  (LOWER(UUID()), 1, 'RECRUIT', '🐕 이번 주말 유기견 보육원 봉사 같이 가실 분 계신가요?', '이번 주 토요일 오전 부산 북구 유기견 보육원 봉사 같이 카풀해서 가실 분 구합니다! 댓글 남겨주세요~', 'PUBLIC', 3, 1, 35);
