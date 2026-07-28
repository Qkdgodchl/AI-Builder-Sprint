# 🤝 Pixel Care 팀 기능별 협업 가이드라인 (TEAM_CONVENTIONS.md)

이 가이드라인은 팀원 3명이 **마이페이지 계정 역할(ROLE_USER / ROLE_ORGANIZER) 분리 시스템**을 포함하여 모듈별로 풀스택 개발을 완수하기 위한 규약입니다.

---

## 👥 1. 기능별 담당 분담표 (Feature Ownership)

| 팀원 | 담당 모듈 | 개발 영역 (FE UI + BE Controller/Service + DB) |
| :--- | :--- | :--- |
| **팀원 A (리더)** | **💬 커뮤니티 & ❤️ 마이페이지 (일반 참가자)** | - BE: `com.pixelcare.community`, `com.pixelcare.user`<br>- FE: `components/diary/`, `components/mypage/UserMyPage.tsx` |
| **팀원 B** | **🤝 봉사/기금 & 🏢 마이페이지 (주최측/단체)** | - BE: `com.pixelcare.volunteer` (신청/승인/완료 관리)<br>- FE: `components/volunteer/`, `components/mypage/OrganizerMyPage.tsx` |
| **팀원 C** | **🤖 Upstage AI 챗봇 & 🗺️ 뱃지 도감 로드맵** | - BE: `com.pixelcare.ai` (Upstage Solar LLM 연동)<br>- FE: `components/ai/`, 뱃지 도감 UI |

---

## 🌿 2. Git 브랜치 전략

- **`main`**: 배포용 메인 브랜치 (PR 후 Merge)
- **`feature/community`**: 커뮤니티 게시판 & 댓글 풀스택 (팀원 A)
- **`feature/volunteer`**: 봉사 신청 & 단체 전용 기금 모금 풀스택 (팀원 B)
- **`feature/ai-mate`**: Upstage AI 챗봇 & 로드맵 풀스택 (팀원 C)

---

## 📝 3. Commit 메시지 컨벤션 (⚠️ 한글 작성 원칙)

커밋 메시지는 모든 팀원이 한눈에 이해할 수 있도록 **한글(Korean)**로 작성합니다.

```
feat(커뮤니티): 게시글 댓글 작성 및 검색 API 구현
feat(봉사): 봉사 공고 직접 등록 및 단체 기금 모금 연동
feat(AI): Upstage Solar LLM 맞춤 봉사 추천 파이프라인 연동
feat(마이페이지): 일반 참가자 및 주최측 단체 계정 권한 분리 대시보드 구현
fix(커뮤니티): 응원 하트 수 실시간 상승 오류 수정
docs: 마이페이지 역할 분리 및 한글 커밋 컨벤션 문서 업데이트
```
