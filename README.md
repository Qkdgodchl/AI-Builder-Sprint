# 👾 픽셀 케어 (Pixel Care / 잇다 ITDA)

> **VolunteerCatalog Editorial Art Magazine & Bento Grid 기반 AI 선행 & 봉사 커뮤니티 플랫폼**  
> **AI Builder Sprint 2026** (부산대학교 APPTIVE 주최 / Upstage 후원)

---

## 🌟 프로젝트 개요 (Overview)

**잇다 ITDA (Pixel Care)**는 **VolunteerCatalog Editorial Art Magazine & Bento Grid 디자인 시스템**과 **Upstage Solar LLM AI** 기술을 결합하여, 사용자의 선행 의향을 자연어로 파싱하고 봉사·기부 약정의 체결부터 갱신, 전자서명 보관까지 원스톱으로 관리하는 혁신적인 선행 커뮤니티 플랫폼입니다.

플랫폼은 **3단계 계정 역할 (`USER` / `CENTER_MANAGER` / `OPERATOR`)**에 따른 가변 하단 탭 내비게이션, 센터 관리자 권한 및 센터 승인 워크플로우, 모두싸인 API 연동 CLM 전자서명 파이프라인, 소프트 삭제(Soft Delete) 및 감사 로그(Audit Log) 시스템을 완벽히 구축하고 있습니다.

---

## ✨ 핵심 기능 (Key Features)

### 1. 🎨 VolunteerCatalog Editorial Art Magazine & Bento Grid UI
- **에디토리얼 벤토 그리드 디자인**: 세련된 카드 레이아웃과 높은 가독성의 매거진 스타일 카탈로그 UI.
- **실시간 온기 온도계 UI**: 기본 36.5°C에서 봉사/기부 참여 시 온도계 수치 실시간 상승 및 `warmth_events` 감사 로깅.
- **LV1 ~ LV5 활동 뱃지 도감**: 이력과 약정에 따른 레벨별 픽셀 뱃지 수집 및 로컬/DB 지속성(Persistence) 유지.
- **미담 뉴스 (`Good News`) 큐레이팅**: 따뜻하고 긍정적인 선행 뉴스를 실시간으로 수집하고 메인 화면에 큐레이션 제공.

### 2. 🤖 Upstage Solar LLM AI 파이프라인 (`Pixel AI Mate`)
- **자연어 선행 큐레이팅**: "주말에 부산 해운대에서 할 수 있는 봉사 추천해줘" 등 자연어 입력 분석 및 맞춤 미션 추천.
- **의향 구조화 (JSON Extraction)**: 사용자의 희망 지역, 활동 시간, 감정, 기부 주기, 금액, 답례품 희망 여부를 고정 JSON 스키마로 파싱.
- **외부 AI 전송 동의 (Opt-in Consent)**: 사용자 동의 시에만 외부 LLM 전송 및 동의 시각/제공자 감사 정보 보관.
- **Smart Failover Engine**: Upstage API 키 미설정 또는 네트워크 장애 시 외부 요청 없이 100% 정상 작동하는 로컬 폴백 엔진 내장.

### 3. 📝 CLM (Contract Lifecycle Management) & 모두싸인 전자서명
- **약정 자동 생성 & 버전 관리**: AI 큐레이팅 결과를 바탕으로 약정서(`Commitment`) 및 약정 버전(`CommitmentVersion`) 자동 생성.
- **모두싸인(Modusign) API 연동**: 전자서명 보안 링크 생성 및 카카오톡/이메일 서명 요청.
- **실시간 Webhook 상태 동기화**: 서명 완료/거절 Webhook 수신 시 약정 상태 자동 업데이트.
- **PDF 생성 & SHA-256 감사추적**: 완성된 전자 서명 PDF 문서 생성 및 감사추적 증명 자료의 SHA-256 해시 검증 보관 (`ClmDocumentArchiveService`).
- **약정 갱신 & 변경 요청**: 정기후원 만료 전 갱신(`renewalDueAt`, `renewalStatus`) 및 약정 조건 변경 요청(`CommitmentChangeRequest`) 관리.

### 4. 🛡️ 3단계 계정 역할 & 역할별 가변 하단 탭 (Dynamic Navigation)
- **일반 사용자 (`USER`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 ]` (4대 탭)
- **센터 관리자 (`CENTER_MANAGER`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 | 🏢 센터 관리 ]` (5대 탭)
- **운영진 (`OPERATOR`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 | 🛡️ 운영 관리 ]` (5대 탭)

### 5. 🏢 센터 관리 & 승인 워크플로우
- **센터 관리자 권한 신청**: `USER`가 내 기록 탭에서 기관 증빙서류와 함께 권한 신청 (`ManagerApplication`).
- **운영진 승인 & 롤 변경**: `OPERATOR`가 운영 관리 탭에서 검토 후 승인 ➔ `CENTER_MANAGER` 권한 활성화 및 5탭 라우팅 전환.
- **센터 등록 신청 & 승인**: `CENTER_MANAGER`가 센터 정보 등록 (`OrganizationApplication`) ➔ 운영진 승인 시 `APPROVED`.
- **모집글 게시 & 카탈로그 노출**: `CENTER_MANAGER`가 봉사/기부 모집글(`Opportunity`) 작성 및 공개 ➔ 유저 `선행하기` 탭 실시간 노출.
- **신청자 관리 & 출석 승인**: 센터 관리자의 신청자 검토, 출석 완료 처리 ➔ 유저 온기 체온 상승(+0.5°C) 및 뱃지 지급.
- **센터 대시보드**: 실시간 모집 현황, 출석률 통계, 신청자 관리 대시보드 제공.

### 6. 💬 커뮤니티 & 소프트 삭제 & 감사 로그
- **선행 인증 후기 & 소통**: 봉사/기부 활동 사진 인증 후기 작성, 댓글 소통, 좋아요 반응.
- **소프트 삭제 (Soft Delete)**: 운영진이 허위/불법 게시물 삭제 시 `isDeleted = true`, `deletedBy`, `deletedAt`, `deletionReason` 안전 보관.
- **운영진 감사 로그 (`AdminAuditLog`)**: 승인/거절/삭제 등 모든 운영진 행위에 대한 감사 로그 자동 기록.

### 7. 📊 실시간 플랫폼 종합 통계 (Platform Stats)
- **실시간 통계 집계**: 누적 온기 온도, 이번 달 생성된 봉사/기부 약정 건수, 누적 후원 금액, 이번 달 참여 인원 수 대시보드 제공.

---

## 🏗️ 시스템 아키텍처 (System Architecture)

```text
[ Client: React + TypeScript + Vite (Editorial Bento Grid System) ]
                            │
               ┌────────────┴────────────┐
               ▼                         ▼
   [ REST API Controller ]      [ Upstage Solar LLM API ]
               │                         │
               ▼                         ▼
   [ Service & Domain Logic ]   [ Smart Failover Engine ]
               │                         │
               ▼                         ▼
   [ CLM & 모두싸인 Webhook ]     [ PDF & SHA-256 Archive ]
               │
               ▼
   [ Spring Data JPA / JdbcTemplate ]
               │
               ▼
   [ Database: PostgreSQL (Render Production) / MySQL 8.4 (Local Docker) ]
   (Spring Boot Native SQL Initialization: schema-postgresql.sql)
```

---

## 🛠️ 기술 스택 (Tech Stack)

| 구분 | 사용 기술 |
| :--- | :--- |
| **Frontend** | React 18, TypeScript, Vite, Editorial Bento Grid CSS |
| **Backend** | Java 21, Spring Boot 3.3.4, Spring Data JPA, JdbcTemplate |
| **Database** | PostgreSQL (Render Deployment), MySQL 8.4 LTS (Local Docker) |
| **AI LLM** | Upstage Solar LLM API (`solar-1-mini-chat`) |
| **E-Signature** | 모두싸인 (Modusign) Webhook API & PDF SHA-256 Archive |
| **DB Migration** | Spring Boot Native SQL Initialization (`spring.sql.init.platform=postgresql`) |
| **Testing** | JUnit 5, Gradle Test Runner |

---

## 🔄 센터 승인 및 권한 워크플로우

1. **센터 관리자 권한 신청**: `USER`가 내 기록 탭에서 기관 증빙서류와 함께 권한 신청 (`ManagerApplication`).
2. **운영진 승인**: `OPERATOR`가 운영 관리 탭에서 검토 후 승인 ➔ `CENTER_MANAGER` 권한 활성화 및 `센터 관리` 탭 오픈.
3. **센터 등록 신청**: `CENTER_MANAGER`가 센터 정보 등록 (`OrganizationApplication`).
4. **운영진 승인 & 모집글 게시**: 운영진 승인 후 `CENTER_MANAGER`가 봉사/기부 모집글(`Opportunity`)을 작성 및 공개(`PUBLISHED`)하면 일반 사용자의 `선행하기` 탭에 실시간 노출.

---

## 🚀 로컬 개발 및 배포 가이드

### 1. 환경 변수 설정 (`.env`)

저장소 루트에서 `.env.example`을 복사하여 `.env`를 생성합니다.

```bash
cp .env.example .env
```

### 2. MySQL 실행 (Local Docker)

```bash
docker compose up -d mysql
```

### 3. 백엔드 실행 (Spring Boot)

```bash
cd backend
./gradlew bootRun
```

- 백엔드 주소: `http://localhost:8080` (또는 지정 포트)
- 시작 시 `spring.sql.init.platform=postgresql` / `schema.sql` 기반으로 DB 스키마 자동 동기화.

### 4. 프론트엔드 실행 (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

- 프론트엔드 주소: `http://localhost:5173`

### 5. 테스트 실행

```bash
cd backend
./gradlew test
```

---

## 📚 프로젝트 상세 문서 (`docs/`)

- [📜 서비스 기획 & 개발 로드맵 (PLAN.md)](docs/PLAN.md)
- [🏗️ 시스템 아키텍처 명세서 (ARCHITECTURE.md)](docs/ARCHITECTURE.md)
- [📡 REST API 명세서 (API_SPEC.md)](docs/API_SPEC.md)
- [🗄️ 데이터베이스 스키마 명세서 (DB_SCHEMA.md)](docs/DB_SCHEMA.md)
- [🤝 팀 개발 가이드라인 (TEAM_CONVENTIONS.md)](docs/TEAM_CONVENTIONS.md)

---

> **AI Builder Sprint 2026** — 잇다 ITDA (Pixel Care)
