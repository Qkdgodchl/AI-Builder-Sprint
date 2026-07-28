# 🤝 Pixel Care 팀 기능별 협업 가이드라인 (TEAM_CONVENTIONS.md)

이 가이드라인은 팀원 3명이 **마이페이지 계정 역할(ROLE_USER / ROLE_ORGANIZER) 분리 시스템**을 포함하여 모듈별로 풀스택 개발을 완수하기 위한 규약입니다.

---

## 👥 1. 기능별 담당 분담표 (Feature Ownership)

| 팀원 | 담당 모듈 | 개발 영역 (FE UI + BE Controller/Service + DB) |
| :--- | :--- | :--- |
| **팀원 A (리더)** | **💬 커뮤니티 & ❤️ 마이페이지 (일반 참가자)** | - BE: `com.pixelcare.community`, `com.pixelcare.user`<br>- FE: `components/diary/`, `components/mypage/UserMyPage.tsx` |
| **팀원 B** | **🤝 봉사/기금 & 🏢 마이페이지 (주최측/단체)** | - BE: `com.pixelcare.volunteer` (신청/승인/완료 관리)<br>- FE: `components/volunteer/`, `components/mypage/OrganizerMyPage.tsx` |
| **팀원 C** | **🤖 Upstage AI 챗봇 & 🗺️ 뱃지 도감 로드맵** | - BE: `com.pixelcare.ai` (Upstage Solar LLM 연동)<br>- FE: `components/ai/`, 뱃지 도감 UI |
