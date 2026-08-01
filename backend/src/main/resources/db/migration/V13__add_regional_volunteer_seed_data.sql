-- 광역 지역 필터 시연용 봉사 프로그램
-- 기존 부산 데이터와 함께 서울·대구·광주 지역을 비교할 수 있도록 구성한다.

INSERT INTO volunteers (title, category, location, organizer, target_amount, current_amount)
SELECT '🌳 서울 한강공원 주말 플로깅 봉사', 'VOLUNTEER',
       '서울 영등포구 여의도 한강공원', '서울 그린웨이 봉사단', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM volunteers WHERE title = '🌳 서울 한강공원 주말 플로깅 봉사'
);

INSERT INTO volunteers (title, category, location, organizer, target_amount, current_amount)
SELECT '📱 서울 종로구 어르신 디지털 길잡이', 'VOLUNTEER',
       '서울 종로구 노인종합복지관', '서울 디지털 동행단', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM volunteers WHERE title = '📱 서울 종로구 어르신 디지털 길잡이'
);

INSERT INTO volunteers (title, category, location, organizer, target_amount, current_amount)
SELECT '🐾 서울 마포구 유기동물 입양센터 돌봄', 'VOLUNTEER',
       '서울 마포구 동물복지지원센터', '서울 온기 발자국', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM volunteers WHERE title = '🐾 서울 마포구 유기동물 입양센터 돌봄'
);

INSERT INTO volunteers (title, category, location, organizer, target_amount, current_amount)
SELECT '🌿 대구 수성못 생태 환경 정화 활동', 'VOLUNTEER',
       '대구 수성구 수성못 일대', '대구 초록물결 봉사단', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM volunteers WHERE title = '🌿 대구 수성못 생태 환경 정화 활동'
);

INSERT INTO volunteers (title, category, location, organizer, target_amount, current_amount)
SELECT '🍚 대구 서구 무료급식소 온기 배식 봉사', 'VOLUNTEER',
       '대구 서구 사랑나눔 무료급식소', '대구 한끼나눔 네트워크', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM volunteers WHERE title = '🍚 대구 서구 무료급식소 온기 배식 봉사'
);

INSERT INTO volunteers (title, category, location, organizer, target_amount, current_amount)
SELECT '⛰️ 광주 무등산 숲길 환경 정비 봉사', 'VOLUNTEER',
       '광주 동구 무등산국립공원', '광주 숲길 지킴이', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM volunteers WHERE title = '⛰️ 광주 무등산 숲길 환경 정비 봉사'
);

INSERT INTO volunteers (title, category, location, organizer, target_amount, current_amount)
SELECT '✏️ 광주 북구 지역아동센터 학습 멘토링', 'VOLUNTEER',
       '광주 북구 꿈나무 지역아동센터', '광주 청년 교육봉사단', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM volunteers WHERE title = '✏️ 광주 북구 지역아동센터 학습 멘토링'
);
