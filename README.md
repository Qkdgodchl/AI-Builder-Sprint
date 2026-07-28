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
- **Backend**: Java 17, Spring Boot, Spring Data JPA, H2 / PostgreSQL
- **AI Integration**: Upstage Solar LLM API (대화형 선행 큐레이터 & 의향 JSON 파싱)
- **Design System**: 8-bit/16-bit Retro Pixel UI (`pixel-roadmap-design` 스킬)

---

## 📚 프로젝트 문서 목록 (`docs/`)

- [📜 서비스 기획 & 단계별 개발 로드맵 (PLAN.md)](file:///Users/tatata/Desktop/3-1/project/AI-Builder-Sprint/docs/PLAN.md)
- [🏗️ 시스템 아키텍처 (ARCHITECTURE.md)](file:///Users/tatata/Desktop/3-1/project/AI-Builder-Sprint/docs/ARCHITECTURE.md)
- [📡 REST API 명세서 (API_SPEC.md)](file:///Users/tatata/Desktop/3-1/project/AI-Builder-Sprint/docs/API_SPEC.md)
- [🤝 팀 협업 가이드라인 (TEAM_CONVENTIONS.md)](file:///Users/tatata/Desktop/3-1/project/AI-Builder-Sprint/docs/TEAM_CONVENTIONS.md)
