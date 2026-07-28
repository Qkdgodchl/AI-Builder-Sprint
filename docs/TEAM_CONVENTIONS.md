# 🤝 Pixel Care 팀 기능별 협업 가이드라인 (TEAM_CONVENTIONS.md)

이 가이드라인은 팀원 3명이 **Phase 1 최우선 메인 기능(봉사/기부, 커뮤니티, Upstage AI, 로드맵)**을 분담하여 풀스택으로 완성하기 위한 협업 규약입니다.

---

## 👥 1. Phase 1 기능별 담당 분담표 (Feature Ownership)

| 팀원 | 담당 기능 모듈 | 개발 영역 (FE UI + BE Controller/Service + DB) |
| :--- | :--- | :--- |
| **팀원 A (리더)** | **💬 커뮤니티 & 📖 일기 모듈** | - BE: `com.pixelcare.community` (`Post`, 댓글 API, Flyway)<br>- FE: `components/diary/`, `services/communityApi.ts` |
| **팀원 B** | **🤝 봉사/기부 & 1365 공공 API** | - BE: `com.pixelcare.volunteer` (`Volunteer`, 1365 연동, REST API)<br>- FE: `components/volunteer/`, `services/volunteerApi.ts` |
| **팀원 C** | **🤖 Upstage AI 챗봇 & 🗺️ 성장의 길** | - BE: `com.pixelcare.ai` (Upstage Solar LLM 연동)<br>- FE: `components/ai/`, `components/roadmap/` |

---

## 🌿 2. Git 브랜치 전략

- **`main`**: 최종 발표 및 배포용 안정 브랜치 (직접 Push 금지, PR 필수)
- **`feature/community`**: 커뮤니티 게시판 & 댓글 풀스택 개발 (팀원 A)
- **`feature/volunteer`**: 봉사/기부 카탈로그 & 1365 API 풀스택 개발 (팀원 B)
- **`feature/ai-mate`**: Upstage AI 챗봇 & 로드맵 풀스택 개발 (팀원 C)

---

## 📝 3. Commit 메시지 컨벤션

```
feat(community): 게시글 댓글 작성 및 검색 REST API 구현
feat(volunteer): 1365 공공데이터 XML 파싱 및 봉사 카탈로그 연동
feat(ai): Upstage Solar LLM 프롬프트 추천 파이프라인 연동
fix(community): 응원 하트 수 실시간 상승 오차 수정
docs: Phase 1 핵심 개발 로드맵 PLAN.md 및 API_SPEC.md 업데이트
```
