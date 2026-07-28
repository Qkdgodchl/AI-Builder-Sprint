# 🎮 픽셀 케어 (Pixel Care)

> **AI Builder Sprint 2026 참가작** (부산대학교 APPTIVE 주최 / Upstage 후원)  
> **레트로 픽셀 아트 기반 AI 기부 & 봉사 커뮤니티 플랫폼**

---

## 💡 프로젝트 소개 (Project Overview)

**픽셀 케어 (Pixel Care)**는 8-bit/16-bit 레트로 RPG 아케이드 감성의 **게이미피케이션(Gamification)**과 **Upstage Solar LLM AI**를 결합한 신개념 봉사 & 기부 커뮤니티 플랫폼입니다.

기존의 딱딱하고 이질적인 봉사 신청 UX에서 벗어나, 사용자가 선행(Pixel)을 행할 때마다 **"픽셀 온기 온도계"**가 차오르고 **"S자 로드맵 뱃지 도감"**이 해금되는 즐거운 퀘스트 경험을 제공합니다.

---

## 🌟 4대 핵심 기능 (Key Features)

1. **🤝 1365 연동 봉사 & 픽셀 기부 카탈로그**
   - 행정안전부 1365 자원봉사 포털 공공데이터 실시간 연동 (부산 지역 관내 실체적 봉사 정보)
   - 소규모 동네 봉사 및 픽셀 기부 펀딩 직접 등록 및 달성률(%) 시각화

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

## 🛠️ 기술 스택 (Tech Stack)

- **Frontend**: React 18, TypeScript, Vite, Vanilla CSS (Pixel Aesthetics), Web Audio API
- **Backend**: Java 17, Spring Boot 3.3.4, Spring Data JPA, Flyway DB Migration
- **Database**: MySQL 8.0, H2 (In-Memory Fallback)
- **AI & Open API**: Upstage Solar LLM REST API, 행정안전부 1365 자원봉사 포털 Open API

---

## 📚 상세 개발 & 협업 문서 (Documentation)

- [📋 서비스 기획 & 기능 명세서 (PLAN.md)](docs/PLAN.md)
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
> 백엔드 서버가 `http://localhost:8080`에서 가동되며, Flyway에 의해 DB 마이그레이션이 자동 수행됩니다.

### 2. 프론트엔드 실행 (React & Vite)
```bash
cd frontend
npm install
npm run dev
```
> 브라우저에서 `http://localhost:5173` 접속하여 서비스를 확인합니다.

---

## 🏆 심사 기준 반영 포인트 (Evaluation Points)

- **창의성 (20점)**: 레트로 RPG 아케이드 감성의 픽셀 도감 & 온기 온도계 게이미피케이션 UX
- **AI 활용도 (20점 + 가점 5점)**: Upstage Solar LLM 기반 맞춤형 봉사 미션 추천 및 Failover 엔진
- **실용성 (20점)**: 1365 자원봉사 포털 공공데이터 실체 연동
- **완성도 (20점)**: Spring Boot + MySQL(Flyway) + React 풀스택 및 8-bit 사운드 효과음
- **지역사회 기여도 (가점 5점)**: 부산 지역 관내 봉사 및 동네 밀착형 선순환 나눔 문화 구축
