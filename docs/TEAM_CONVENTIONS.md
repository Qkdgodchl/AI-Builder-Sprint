# 🤝 Pixel Care 팀 기능별 협업 가이드라인 (TEAM_CONVENTIONS.md)

이 가이드라인은 팀원 3명이 **기능별(Vertical Slice / Feature-Driven) 풀스택 개발** 방식으로 효율적으로 협업하기 위한 규칙 체계입니다.

---

## 👥 1. 기능별 담당 분담표 (Feature Ownership)

각 팀원은 자신이 맡은 모듈의 **[FE UI + BE Controller/Service + DB 스키마 + REST API]** 전체를 책임지고 개발합니다.

| 팀원 구분 | 담당 기능 모듈 | 주요 소스코드 범위 (FE + BE) |
| :--- | :--- | :--- |
| **팀원 A (리더)** | **💬 커뮤니티 & 📖 일기 모듈** | - BE: `com.pixelcare.community` (`Post`, `Comment`, REST API)<br>- FE: `components/diary/`, `services/communityApi.ts` |
| **팀원 B** | **🤝 봉사/기부 & 1365 공공 API** | - BE: `com.pixelcare.volunteer` (`Volunteer`, 1365 연동, REST API)<br>- FE: `components/volunteer/`, `services/volunteerApi.ts` |
| **팀원 C** | **🤖 Upstage AI 챗봇 & 🗺️ 성장의 길** | - BE: `com.pixelcare.ai` (Upstage Solar LLM 연동)<br>- FE: `components/ai/`, `components/roadmap/` |

---

## 🌿 2. Git 브랜치 전략 (Feature Branch Workflow)

- **`main`**: 최종 발표 및 배포용 안정 브랜치 (직접 Push 금지, PR 필수)
- **`feature/community`**: 커뮤니티 게시판 & 댓글 풀스택 개발 (팀원 A)
- **`feature/volunteer`**: 봉사/기부 카탈로그 & 1365 API 풀스택 개발 (팀원 B)
- **`feature/ai-mate`**: Upstage AI 챗봇 & 로드맵 풀스택 개발 (팀원 C)

---

## 📝 3. Commit 메시지 컨벤션

커밋 메시지 상단에 자신이 속한 모듈 이름(Scope)을 명시합니다.

```
feat(community): 게시글 댓글 작성 및 삭제 REST API 구현
feat(volunteer): 1365 공공데이터 XML 파싱 및 봉사 목록 연동
feat(ai): Upstage Solar LLM 파이프라인 프롬프트 작성
fix(community): 댓글 응원 하트 수 상승 로직 오류 수정
style(common): 픽셀 아트 레트로 CSS 색상 토큰 수정
docs: TEAM_CONVENTIONS 및 API_SPEC 마크다운 업데이트
```

---

## 🔒 4. 충돌(Conflict) 방지 & Flyway 버저닝 규칙

1. **타 영역 소스코드 직접 수정 금지**:
   - 본인이 담당한 도메인 패키지/폴더 내에서 집중 작업합니다.
   - 공통 파일(`App.tsx`, `Header.tsx`, `application.yml`, `build.gradle`) 수정 시 반드시 미리 커뮤니케이션합니다.

2. **Flyway DB 마이그레이션 파일 버저닝 규칙**:
   - 마이그레이션 파일 이름이 겹치지 않도록 아래와 같이 버전을 순차 부여합니다.
     - 팀원 A (커뮤니티): `V2__add_comments_table.sql`
     - 팀원 B (봉사): `V3__add_volunteer_fields.sql`
     - 팀원 C (AI/기타): `V4__add_ai_logs.sql`

3. **PR (Pull Request) & Code Review**:
   - PR 작성 시 개발 내용 및 검증 결과를 명시합니다.
   - 팀원 1인 이상의 검증 리뷰 완료 후 `main`에 Merge합니다.
