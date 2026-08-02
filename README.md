# 👾 픽셀 케어 (Pixel Care / 잇다 ITDA)

> **VolunteerCatalog Editorial Art Magazine & Bento Grid 기반 AI 선행 & 봉사 커뮤니티 플랫폼**  
> **AI Builder Sprint 2026** (부산대학교 APPTIVE 주최 / Upstage 후원)

---

## 🌟 프로젝트 개요 (Overview)

**픽셀 케어 (Pixel Care / 잇다 ITDA)**는 **VolunteerCatalog Editorial Art Magazine & Bento Grid 디자인 시스템**과 **Upstage Solar LLM AI** 기술을 결합하여, 사용자의 선행 의향을 자연어로 파싱하고 봉사·기부 약정의 체결부터 갱신까지 원스톱으로 관리하는 혁신적인 선행 커뮤니티 플랫폼입니다.

플랫폼은 **3단계 계정 역할 (`USER` / `CENTER_MANAGER` / `OPERATOR`)**에 따른 가변 하단 탭 내비게이션, 센터 관리자 권한 및 센터 승인 워크플로우, 데이터 무결성을 보장하는 소프트 삭제(Soft Delete) 및 감사 로그(Audit Log) 시스템을 완벽히 구축하고 있습니다.

---

## ✨ 핵심 기능 (Key Features)

### 1. 🎨 VolunteerCatalog Editorial Art Magazine & Bento Grid UI
- **에디토리얼 벤토 그리드 디자인**: 세련된 카드 레이아웃과 높은 가독성의 매거진 스타일 카탈로그 UI.
- **실시간 온기 온도계**: 기본 36.5°C에서 봉사/기부 참여 시 온도계 수치 실시간 상승.
- **활동 뱃지 도감**: 이력과 약정에 따른 뱃지 수집 및 지속성(Persistence) 유지.

### 2. 🤖 Upstage Solar LLM AI 파이프라인 (`Pixel AI Mate`)
- **자연어 선행 큐레이팅**: "주말에 부산 해운대에서 할 수 있는 봉사 추천해줘" 등 자연어 입력 분석.
- **의향 구조화 (JSON Extraction)**: 사용자의 희망 지역, 활동 시간, 감정, 기부 주기를 고정 JSON 스키마로 파싱.
- **Smart Failover Engine**: Upstage API 키 미설정 또는 네트워크 단락 시 외부 요청 없이 100% 정상 작동하는 로컬 폴백 엔진 내장.

### 3. 🛡️ 3단계 계정 역할 & 역할별 가변 하단 탭 (Dynamic Navigation)
- **일반 사용자 (`USER`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 ]` (4대 탭)
- **센터 관리자 (`CENTER_MANAGER`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 | 🏢 센터 관리 ]` (5대 탭)
- **운영진 (`OPERATOR`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 | 🛡️ 운영 관리 ]` (5대 탭)

### 4. 📝 CLM & 약정 자동화 워크플로우
- **약정 생성 및 서명 파이프라인**: AI 큐레이팅 결과를 바탕으로 약정서(Commitment) 자동 생성.
- **모두싸인(Modusign) 보안 연동**: 전자서명 보안 링크 생성 및 Webhook 상태 동기화, SHA-256 감사추적 및 버전 관리.

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
| **DB Migration** | Spring Boot Native SQL Initialization (`schema-postgresql.sql`) |
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

> **AI Builder Sprint 2026** — 픽셀 케어 (Pixel Care / 잇다 ITDA)
