# 🎮 픽셀 케어 (Pixel Care)

> **AI Builder Sprint 2026 참가작** (부산대학교 APPTIVE 주최 / Upstage 후원)  
> **레트로 픽셀 아트 기반 AI 기부 & 봉사 커뮤니티 플랫폼**

---

## 💡 프로젝트 소개 (Project Overview)

**픽셀 케어 (Pixel Care)**는 8-bit/16-bit 레트로 RPG 아케이드 감성의 **게이미피케이션(Gamification)**과 **Upstage Solar LLM AI**를 결합한 신개념 봉사 & 기부 커뮤니티 플랫폼입니다.

Phase 1에서 사용자 핵심 메인 기능(**1365 봉사/기부 카탈로그, 픽셀 커뮤니티, Upstage AI 챗봇, 성장의 길 온기 온도계**)을 완성하고, Phase 2에서 **모두싸인 CLM 전자서명 및 약정 생애주기 관리**로 확장합니다.

---

## 🌟 Phase 1 4대 핵심 메인 기능 (Phase 1 Core Features)

1. **🤝 1365 연동 봉사 & 픽셀 기부 카탈로그**
   - 행정안전부 1365 자원봉사 포털 공공데이터 실시간 연동 (부산 지역 관내 실체적 봉사 정보)
   - 소규모 동네 봉사 및 픽셀 기부 펀딩 프로젝트 직접 등록 및 달성률(%) 시각화

2. **💬 픽셀 커뮤니티 (Pixel Community)**
   - 봉사 참여 후기, 동행 모집, 소통 피드 및 카테고리별 실시간 검색
   - 댓글 소통 및 응원 픽셀 하트(❤️) 전달 기능

3. **🤖 Upstage AI 픽셀 메이트 (Pixel AI Mate)**
   - Upstage Solar LLM 파이프라인 기반 사용자 감정/시간/지역 파싱 맞춤형 봉사 미션 추천
   - 오프라인/네트워크 장애 시 100% 정상 작동하는 **Smart Failover** 렌더링 엔진

4. **🗺️ 성장의 길 & 실시간 온기 온도계 (Pixel Roadmap)**
   - 사용자 활동(봉사, 기부, 커뮤니티 작성)에 따른 온기 온도계(+0.5°C) 실시간 상승
   - 레벨별(LV1~LV5) 픽셀 아트 트로피 뱃지 해금 및 Web Audio 칩튠 효과음

---

## 📚 상세 개발 & 협업 문서 (Documentation)

- [📜 단계별 개발 로드맵 (PLAN.md)](docs/PLAN.md)
- [🤝 팀 기능별 협업 가이드라인 (TEAM_CONVENTIONS.md)](docs/TEAM_CONVENTIONS.md)
- [📡 REST API 명세서 (API_SPEC.md)](docs/API_SPEC.md)
- [🏗️ 시스템 아키텍처 명세서 (ARCHITECTURE.md)](docs/ARCHITECTURE.md)

---

## 🚀 로컬 실행 방법 (Quick Start Guide)

### 1. 백엔드 실행 (Spring Boot & Flyway)
```bash
cd backend
./gradlew bootRun
```

### 2. 프론트엔드 실행 (React & Vite)
```bash
cd frontend
npm install
npm run dev
```
