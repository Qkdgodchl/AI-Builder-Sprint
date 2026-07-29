# 🤝 Pixel Care 팀 기능별 협업 가이드라인 (TEAM_CONVENTIONS.md)

이 가이드라인은 팀원 3명이 **3단계 계정 역할 (`USER` / `CENTER_MANAGER` / `OPERATOR`) 승인 및 관리 시스템**을 포함하여 풀스택 개발을 완수하기 위한 규약입니다.

---

## 👥 1. 기능별 담당 분담표 (Feature Ownership)

| 팀원 | 담당 모듈 | 개발 영역 (FE UI + BE Controller/Service + DB) |
| :--- | :--- | :--- |
| **팀원 A (리더)** | **💬 커뮤니티 & 📜 내 기록 (`USER`)** | - BE: `com.pixelcare.community`, `com.pixelcare.user`<br>- FE: `components/community/`, `components/myrecords/` (내 이력, 온도계, 뱃지 도감, 센터 관리자 신청 폼) |
| **팀원 B** | **🏢 센터 관리 (`CENTER_MANAGER`) & 🛡️ 운영 관리 (`OPERATOR`)** | - BE: `com.pixelcare.organization`, `com.pixelcare.operator`<br>- FE: `components/center/` (센터 대시보드, 모집글 작성, 신청자 승인), `components/operator/` (권한/센터 승인 처리, 게시물 소프트 삭제, 감사 로그) |
| **팀원 C** | **🤖 Upstage AI 챗봇 & 🎁 선행하기 목록 (`USER`)** | - BE: `com.pixelcare.ai` (Upstage Solar LLM 연동), `com.pixelcare.volunteer`<br>- FE: `components/ai/` (PixelAiMate 챗봇), `components/volunteer/` (선행하기 카탈로그 및 상세페이지) |

---

## 🌿 2. Git 브랜치 전략

- **`main`**: 배포용 메인 브랜치 (PR 후 Merge)
- **`feature/volunteer-donation`**: 🎁 봉사 & 기부 카탈로그/상세 탭
- **`feature/center-manager`**: 🏢 센터 관리자 탭 (센터 대시보드, 모집글 작성, 신청자 승인)
- **`feature/operator`**: 🛡️ 운영진 탭 (관리자/센터 승인 처리, 게시물 소프트 삭제, 감사 로그)
- **`feature/ai-chat`**: 🤖 Upstage Solar AI 챗봇 탭 (`Pixel AI Mate`)
- **`feature/community`**: 💬 픽셀 커뮤니티 탭 (선행 인증 후기 & 동행 모집)
- **`feature/my-records`**: 📜 내 기록 탭 (내 이력, 온기 온도계, 뱃지 도감, 센터 관리자 신청)

---

## 📝 3. Commit 메시지 컨벤션 (⚠️ 한글 작성 원칙)

커밋 메시지는 모든 팀원이 한눈에 이해할 수 있도록 **한글(Korean)**로 작성합니다.

```
feat(유저): 일반 사용자 내 기록 조회 및 센터 관리자 권한 신청 API 구현
feat(센터): 승인된 센터의 봉사 모집글 작성 및 신청자 완료 승인 기능 구현
feat(운영진): 센터 관리자 및 센터 등록 요청 승인/거절 및 게시글 소프트 삭제 기능 구현
feat(AI): Upstage Solar LLM 파이프라인 의향 상담 및 맞춤 봉사 추천 연동
docs: 3-Role 계정 권한 및 역할별 가변 탭 아키텍처 문서 업데이트
```

---

## 🗄️ 4. DB 스키마 협업 규칙

팀원별 DB 서버와 데이터는 공유하지 않아도 됩니다. Git에 포함된 Flyway Migration을 각자 로컬 DB에 적용해 **스키마 버전만 동일하게 유지**합니다.

### 관리 위치

- 사람이 읽는 전체 설계: `docs/DB_SCHEMA.md`
- 실제 스키마 변경 SQL: `backend/src/main/resources/db/migration/`
- 적용 이력: 각 DB의 `flyway_schema_history`

### 작업 규칙

1. DB 작업 전 `main`의 최신 Migration 목록을 확인합니다.
2. 사용할 Migration 버전을 팀 채널에 먼저 공유합니다.
3. 기존 Migration은 수정하지 않고 새 파일을 추가합니다.
4. Entity 변경과 Migration SQL을 같은 PR에 포함합니다.
5. PR에서 MySQL과 테스트 환경의 Migration 성공 여부를 기록합니다.
6. Merge 후 다른 팀원은 `git pull`하고 백엔드를 실행해 새 Migration을 적용합니다.

파일명 예:

```text
V2__create_clm_schema.sql
V3__add_opportunity_search_indexes.sql
V4__create_opportunity_and_application_tables.sql
```

한 버전을 여러 브랜치에서 동시에 사용하지 않습니다. 이미 적용된 Migration을 변경해야 한다면 기존 파일을 고치지 않고 다음 번호의 보정 Migration을 추가합니다.
