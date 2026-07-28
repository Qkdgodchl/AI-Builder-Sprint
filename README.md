# 🎮 픽셀 케어 (Pixel Care)

> **AI Builder Sprint 2026 참가작** (부산대학교 APPTIVE 주최 / Upstage 후원)  
> **레트로 픽셀 아트 기반 AI 기부 & 봉사 커뮤니티 플랫폼**

---

## 💡 프로젝트 소개 (Project Overview)

**픽셀 케어 (Pixel Care)**는 **봉사 단체가 직접 공고를 등록하고 플랫폼에서 신청/이행을 직접 관리**하며, **특정 단체별 전용 기금(Fund) 탭을 개설하여 기부금을 수령하고 전달**하는 8-bit/16-bit 레트로 RPG 게이미피케이션 플랫폼입니다.

---

## 🌟 4대 핵심 메인 기능 (Core Features)

1. **🤝 봉사 직접 신청 & 단체 전용 기금 펀딩**
   - 봉사 단체가 직접 등록한 봉사 공고 1초 간편 신청 및 단체 승인 관리
   - 수혜 기관별 전용 기금 펀딩 모금 및 실시간 달성률(%) 시각화

2. **💬 픽셀 커뮤니티 (Pixel Community)**
   - 봉사 참여 후기, 동행 모집, 소통 피드 및 카테고리별 실시간 검색
   - 댓글 소통 및 응원 픽셀 하트(❤️) 전달 기능

3. **🤖 Upstage AI 픽셀 메이트 (Pixel AI Mate)**
   - Upstage Solar LLM 파이프라인 기반 사용자 맞춤형 봉사 공고 & 단체 기금 큐레이션
   - Smart Failover 인메모리 렌더링 엔진

4. **🗺️ 성장의 길 & 실시간 온기 온도계 (Pixel Roadmap)**
   - 봉사 참여 및 기부 시 온기 온도계(+0.5°C) 실시간 상승
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
