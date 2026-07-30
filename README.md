# 👾 픽셀 케어 (Pixel Care)

> **레트로 픽셀 아트 기반 AI 기부 & 봉사 커뮤니티 플랫폼**  
> **AI Builder Sprint 2026** (부산대학교 APPTIVE 주최 / Upstage 후원)

---

## 🌟 프로젝트 소개

**픽셀 케어 (Pixel Care)**는 8-bit/16-bit 레트로 픽셀 아트 감성의 게이미피케이션(온도계 UI, LV1~LV5 픽셀 뱃지, 칩튠 사운드)과 **Upstage Solar LLM**을 결합하여, 사용자의 선행 의향을 대화로 파악하고 맞춤형 봉사·기부를 추천 및 이행하도록 돕는 선행 플랫폼입니다.

플랫폼은 **3단계 계정 역할 (`USER` / `CENTER_MANAGER` / `OPERATOR`)**과 **역할별 가변 하단 탭**, **센터 및 권한 승인 프로세스**, **소프트 삭제 & 감사 로그** 시스템을 구비하고 있습니다.

---

## 📱 역할별 하단 가변 탭 구조

1. **일반 사용자 (`USER`)** — 하단 4개 탭
   - 🏠 **홈**: Upstage AI 픽셀 큐레이터 대화 진입점
   - 🎁 **선행하기**: 봉사 공고 & 기부 프로젝트 카탈로그 및 상세 신청
   - 💬 **커뮤니티**: 선행 인증 후기 & 동행 모집 소통
   - 📜 **내 기록**: 신청 이력, 온기 온도계, 뱃지 도감, 센터 관리자 권한 신청
2. **센터 관리자 (`CENTER_MANAGER`)** — 하단 5개 탭
   - `[홈 | 선행하기 | 커뮤니티 | 내 기록]` + 🏢 **센터 관리** (센터 대시보드, 내 센터 관리, 모집글 작성, 신청자 승인/출석 처리)
3. **운영진 (`OPERATOR`)** — 하단 5개 탭
   - `[홈 | 선행하기 | 커뮤니티 | 내 기록]` + 🛡️ **운영 관리** (관리자/센터 승인/거절, 게시물 소프트 삭제, 감사 로그)

---

## 🛠️ 기술 스택 (Tech Stack)

- **Frontend**: React, TypeScript, Vite, Vanilla CSS System (Retro Pixel Aesthetic)
- **Backend**: Java 17, Spring Boot, Spring Data JPA, MySQL 8.4 LTS, Flyway
- **AI Integration**: Upstage Solar LLM API (대화형 선행 큐레이터 & 의향 JSON 파싱)
- **Design System**: 8-bit/16-bit Retro Pixel UI (`pixel-roadmap-design` 스킬)

---

## 📚 프로젝트 문서 목록 (`docs/`)

- [📜 서비스 기획 & 단계별 개발 로드맵 (PLAN.md)](docs/PLAN.md)
- [🏗️ 시스템 아키텍처 (ARCHITECTURE.md)](docs/ARCHITECTURE.md)
- [📡 REST API 명세서 (API_SPEC.md)](docs/API_SPEC.md)
- [🗄️ 데이터베이스 설계 & Flyway 관리 (DB_SCHEMA.md)](docs/DB_SCHEMA.md)
- [🤝 팀 협업 가이드라인 (TEAM_CONVENTIONS.md)](docs/TEAM_CONVENTIONS.md)

---

## 🚀 로컬 개발 실행

### 1. 환경변수 준비

저장소 루트에서 예시 파일을 복사합니다.

```bash
cp .env.example .env
```

`.env`에는 로컬 DB 비밀번호와 외부 API 키를 입력합니다. `.env`는 Git에 커밋되지 않습니다.

### 2. MySQL 실행

Docker Desktop을 실행한 뒤 저장소 루트에서 다음 명령을 사용합니다.

```bash
docker compose up -d mysql
docker compose ps
```

MySQL 데이터는 Docker의 `mysql_84_data` 볼륨에 저장되므로 컨테이너를 재시작해도 유지됩니다.

### 3. 백엔드 실행

```bash
cd backend
./gradlew bootRun
```

백엔드는 기본적으로 `127.0.0.1:3307/pixelcare`에 접속합니다. 로컬에 설치된 MySQL의 기본 포트 `3306`과 충돌하지 않도록 프로젝트 Docker DB는 `3307`을 사용합니다. 시작할 때 Flyway가 적용되지 않은 Migration을 순서대로 실행하고, Hibernate는 Entity와 스키마가 일치하는지 검증합니다.

### 4. 프론트엔드 실행

다른 터미널에서 실행합니다.

```bash
cd frontend
npm install
npm run dev
```

- 프론트엔드: `http://127.0.0.1:5173`
- 백엔드: `http://127.0.0.1:8080`

로컬 개발 부트스트랩 계정:

- 센터 관리자: `manager@pixelcare.demo` / `Manager123!`
- 운영진: `operator@pixelcare.local` / `Operator123!`

이 계정은 로컬 개발 전용이다. 비밀번호는 `.env`의
`APP_MANAGER_PASSWORD`, `APP_OPERATOR_PASSWORD`로 바꿀 수 있으며 운영 환경에서는
`APP_BOOTSTRAP_ENABLED=false`로 비활성화한다.

### 5. 테스트

```bash
cd backend
./gradlew test

cd ../frontend
npm run build
```

API별 구현·성공·실패 현황과 수동 검증 방법은
[`docs/API_SPEC.md`](docs/API_SPEC.md)의 체크표에서 관리한다.

### 자주 쓰는 DB 명령

```bash
# MySQL 로그
docker compose logs -f mysql

# MySQL만 중지
docker compose stop mysql

# 다시 실행
docker compose start mysql

# 컨테이너 제거 (데이터 볼륨은 유지)
docker compose down
```

데이터까지 완전히 지우는 `docker compose down -v`는 팀원이 명시적으로 로컬 DB 초기화를 원하는 경우에만 사용합니다.
