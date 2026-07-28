# 🤝 Pixel Care 팀 기능별 협업 가이드라인 (TEAM_CONVENTIONS.md)

이 가이드라인은 팀원 3명이 **플랫폼 직접 관리형 봉사 공고 & 단체 전용 기금 모금 시스템**을 분담하여 완수하기 위한 협업 규약입니다.

---

## 👥 1. 기능별 담당 분담표 (Feature Ownership)

| 팀원 | 담당 모듈 | 개발 영역 (FE UI + BE Controller/Service + DB) |
| :--- | :--- | :--- |
| **팀원 A (리더)** | **💬 커뮤니티 & 📖 일기 모듈** | - BE: `com.pixelcare.community` (`Post`, 댓글 API, Flyway)<br>- FE: `components/diary/`, `services/communityApi.ts` |
| **팀원 B** | **🤝 봉사 직접 신청 & ❤️ 단체 전용 기금** | - BE: `com.pixelcare.volunteer` (`Volunteer`, 기금 모금 API)<br>- FE: `components/volunteer/`, `services/volunteerApi.ts` |
| **팀원 C** | **🤖 Upstage AI 챗봇 & 🗺️ 성장의 길** | - BE: `com.pixelcare.ai` (Upstage Solar LLM 연동)<br>- FE: `components/ai/`, `components/roadmap/` |

---

## 🌿 2. Git 브랜치 전략

- **`main`**: 배포용 메인 브랜치 (PR 후 Merge)
- **`feature/community`**: 커뮤니티 게시판 & 댓글 풀스택 (팀원 A)
- **`feature/volunteer`**: 봉사 신청 & 단체 전용 기금 모금 풀스택 (팀원 B)
- **`feature/ai-mate`**: Upstage AI 챗봇 & 로드맵 풀스택 (팀원 C)
