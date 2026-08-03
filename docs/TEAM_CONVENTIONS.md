# 🤝 잇다 ITDA 팀 협업 가이드라인 (TEAM_CONVENTIONS.md)

이 가이드라인은 팀원 3명(**권윤재, 이영민, 전동훈**)이 풀스택 개발자로서 **3단계 계정 역할 (`USER` / `CENTER_MANAGER` / `OPERATOR`) 승인 및 AI 선행 플랫폼**을 효율적으로 구현하기 위한 협업 규약입니다.

---

## 👥 1. 팀원 명단 및 역할 분담 (Team Ownership & Full Stack R&R)

| 팀원 | 역할 (R&R) | 주요 담당 업무 (FE UI + BE Controller/Service + DB) |
| :---: | :---: | :--- |
| **권윤재** | **Full Stack Developer** | • React 18 + TypeScript 기반 Editorial Bento Grid 디자인 시스템 구축<br>• 3단계 계정 역할별 가변 하단 탭 내비게이션 라우팅 및 전단 UI 개발<br>• CONNECT(온기 잇다) 역제안 프론트엔드 및 백엔드 서비스/컨트롤러 연동 |
| **이영민** | **Full Stack Developer** | • Upstage Solar LLM 프롬프트 페르소나 설계 및 JSON 의향 파싱 엔진 구축<br>• 로컬 Smart Failover Engine 개발 및 외부 AI 전송 동의(Opt-in) 제어<br>• 센터 관리자 대시보드 및 실시간 플랫폼 통계 기능 개발 |
| **전동훈** | **Full Stack Developer** | • Spring Boot 백엔드 아키텍처 설계 및 PostgreSQL Native Initialization 구축<br>• Upstage Solar LLM 연동, CLM 전자서명 및 모두싸인 Webhook 파이프라인 개발<br>• 3단계 권한 승인 워크플로우, 소프트 삭제 및 감사 로그 시스템 구축 |

---

## 🌿 2. Git 브랜치 전략 (Git Branching Strategy)

- **`main`**: 프로덕션 배포용 메인 브랜치 (PR 검토 후 Merge)
- **`feature/connect-tab`**: 🤝 CONNECT (온기 잇다 — 선행 역제안 & 이웃 응원 / 센터 개설)
- **`feature/clm-signature`**: 📝 CLM 약정 자동화 및 모두싸인 Webhook / SHA-256 PDF 보관
- **`feature/ai-solar`**: 🤖 Upstage Solar LLM AI 파이프라인 (`Pixel AI Mate`) & Smart Failover
- **`feature/center-operator`**: 🏢 센터 관리자 대시보드 & 🛡️ 운영진 승인/소프트 삭제/감사로그
- **`feature/user-records`**: 📜 내 기록 (활동 이력, 온기 온도계, `warmth_events`, 뱃지 도감)

---

## 📝 3. Commit 메시지 컨벤션 (한글 작성 원칙)

커밋 메시지는 모든 팀원이 한눈에 이해할 수 있도록 **한글(Korean)**로 명확하게 작성합니다.

```text
feat(CONNECT): 선행 아이디어 역제안 작성 및 이웃 응원(Support), 센터 수락(Claim) 기능 구현
feat(CLM): 모두싸인 Webhook 수신을 통한 약정 상태 자동 동기화 및 PDF SHA-256 저장소 연동
feat(AI): Upstage Solar LLM 의향 JSON 구조화 파싱 및 Smart Failover 로컬 엔진 구현
feat(운영진): 센터 관리자 승인, 불법 게시글 소프트 삭제(isDeleted=true) 및 감사 로그 저장
docs: 해커톤 예선 제출물 체크리스트 기준 README.md 및 PLAN.md 업데이트
```

---

## 🗄️ 4. DB 스키마 & 플랫폼 동기화 규칙

팀원별 DB 서버와 데이터는 독립적으로 관리합니다.

### 관리 위치 및 도구
- 전체 ERD 및 스키마 설계: `docs/DB_SCHEMA.md`
- PostgreSQL 배포 및 초기화 스키마: `backend/src/main/resources/schema-postgresql.sql`
- Spring Boot Native SQL Initialization: `spring.sql.init.platform=postgresql` (`application.yml`)

### 작업 규칙
1. DB 테이블/컬럼 변경 전 `main` 브랜치의 `schema-postgresql.sql` 및 Migration SQL을 최신 상태로 `git pull` 합니다.
2. 엔티티(Entity) 변경과 스키마 SQL 수정은 반드시 같은 커밋/PR에 포함합니다.
3. 컬럼 추가 시 기존 시스템 데이터가 손상되지 않도록 `ADD COLUMN IF NOT EXISTS` 및 기본값(`DEFAULT`)을 항상 포함합니다.
4. 배포 환경(Render PostgreSQL)과 로컬 Docker PostgreSQL 양쪽 환경에서 모두 빌드가 통과하는지 검증합니다.
