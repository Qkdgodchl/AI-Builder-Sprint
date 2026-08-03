# CLAUDE.md

잇다(ITDA) 저장소에서 Claude Code로 작업할 때의 기준 문서.

## 프로젝트 구조

- **백엔드**: Spring Boot 3.3.4 (Java 21), PostgreSQL 16 — `backend/`
- **프론트엔드**: React 19 + TypeScript + Vite — `frontend/`
- **배포**: Render(백엔드) · Vercel(프론트), `main` 푸시 시 자동 배포

## 자주 쓰는 명령

```bash
cd backend && ./gradlew test      # 백엔드 테스트 (JUnit 5)
cd frontend && npm run build      # 프론트 타입체크 + 빌드
cd frontend && npm run lint       # oxlint
docker compose up -d postgres     # 로컬 DB
```

## 작업 규칙

- 커밋 메시지는 한국어 서술형으로 "무엇을 왜 바꿨는지"를 남긴다.
- 배포 DB는 PostgreSQL이다. `UPDATE ... JOIN` 같은 MySQL 전용 문법을 쓰지 않는다.
  (로컬 개발은 MySQL도 허용되므로 양쪽에서 도는 표준 문법만 사용)
- 스키마의 원천은 `backend/src/main/resources/schema-postgresql.sql`이다.
  Flyway는 비활성 상태이며 `db/migration/`은 설계 이력 기록이다.
- 외부 API(모두싸인·Upstage) 호출은 실패를 전제로 설계한다 — 타임아웃 상한, 폴백,
  "조회 실패를 성공으로 간주하지 않기"를 함께 둔다.
- DB 트랜잭션을 잡은 채 외부 API를 오래 기다리지 않는다 (커넥션 풀 고갈 방지).
- 문서(`docs/`)와 코드가 어긋나면 문서를 코드에 맞춘다.

## 에이전트 지침 문서

- 개발 에이전트 공통 지침: [`docs/AGENTS.md`](docs/AGENTS.md)
- 디자인 시스템 커스텀 스킬: [`.agents/skills/pixel-roadmap-design/SKILL.md`](.agents/skills/pixel-roadmap-design/SKILL.md)
- 서비스 기획서: [`docs/workflow.md`](docs/workflow.md)
