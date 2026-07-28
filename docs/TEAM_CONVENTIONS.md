# 🤝 Pixel Care 팀 기능별 협업 가이드라인 (TEAM_CONVENTIONS.md)

이 가이드라인은 팀원 3명이 **3단계 계정 역할 (`USER` / `CENTER_MANAGER` / `OPERATOR`) 승인 및 관리 시스템**을 포함하여 풀스택 개발을 완수하기 위한 규약입니다.

---

## 👥 1. 기능별 담당 분담표 (Feature Ownership)

| 팀원 | 담당 모듈 | 개발 영역 (FE UI + BE Controller/Service + DB) |
| :--- | :--- | :--- |
| **팀원 A (리더)** | **💬 커뮤니티 & 📜 내 기록 (`USER`)** | - BE: `com.pixelcare.community`, `com.pixelcare.user`<br>- FE: `components/community/`, `components/myrecords/` (내 이력, 온도계, 뱃지 도감, 센터 관리자 신청 폼) |
| **팀원 B** | **🏢 센터 관리 (`CENTER_MANAGER`) & 🛡️ 운영 관리 (`OPERATOR`)** | - BE: `com.pixelcare.organization`, `com.pixelcare.operator`<br>- FE: `components/center/` (센터 대시보드, 모집글 작성, 신청자 승인), `components/operator/` (권한/센터 승인 처리, 게시물 소프트 삭제, 감사 로그) |
| **팀원 C** | **🤖 Upstage AI 챗봇 & 🎁 선행하기 목록 (`USER`)** | - BE: `com.pixelcare.ai` (Upstage Solar LLM 연동), `com.pixelcare.volunteer`<br>- FE: `components/ai/` (PixelAiMate 챗봇), `components/volunteer/` (선행하기 카탈로그 및 상세페이지) |

---

## 🌿 2. Git 브랜치 전략

- **`main`**: 배포용 메인 브랜치 (PR 후 Merge)
- **`feature/community-user`**: 커뮤니티 게시판 & 내 기록/센터 관리자 신청 (팀원 A)
- **`feature/center-operator`**: 센터 관리 & 운영진 관리 시스템 (팀원 B)
- **`feature/ai-volunteer`**: Upstage AI 챗봇 & 봉사/기부 선행하기 목록 (팀원 C)

---

## 📝 3. Commit 메시지 컨벤션 (⚠️ 한글 작성 원칙)

커밋 메시지는 모든 팀원이 한눈에 이해할 수 있도록 **한글(Korean)**로 작성합니다.

```
feat(유저): 일반 사용자 내 기록 조회 및 센터 관리자 권한 신청 API 구현
feat(센터): 승인된 센터의 봉사 모집글 작성 및 신청자 완료 승인 기능 구현
feat(운영진): 센터 관리자 및 센터 등록 요청 승인/거절 및 게시글 소프트 삭제 기능 구현
feat(AI): Upstage Solar LLM 파이프라인 의향 상담 및 맞춤 봉사 추천 연동
docs: 3-Role 계정 권한 및 역할별 가변 탭 아키텍처 문서 업데이트
```
