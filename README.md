# 🎮 픽셀 케어 (Pixel Care)

> **AI Builder Sprint 2026 참가작** (부산대학교 APPTIVE 주최 / Upstage 후원)  
> **레트로 픽셀 아트 기반 AI 기부 & 봉사 커뮤니티 플랫폼**

---

## 💡 프로젝트 소개 (Project Overview)

**픽셀 케어 (Pixel Care)**는 **봉사 주최측/단체 계정(`ROLE_ORGANIZER`)과 일반 참가자 계정(`ROLE_USER`)의 권한 및 마이페이지 대시보드를 이원화하여 관리**하며, Upstage Solar LLM AI와 8-bit/16-bit 레트로 게이미피케이션이 결합된 신개념 플랫폼입니다.

---

## 📱 4대 메인 하단 탭 (4 Main Tabs)

1. **🏠 홈 (Home - Upstage AI 픽셀 큐레이터)**
   - Upstage Solar LLM 파이프라인 기반 맞춤형 봉사 공고 & 단체 기금 대화 큐레이션

2. **🎁 선행하기 (Volunteer & Donation Catalog)**
   - 봉사 공고 직접 신청 및 수혜 단체별 전용 기금 펀딩 모금 (실시간 달성률 % 시각화)

3. **💬 픽셀 커뮤니티 (Pixel Community)**
   - 봉사 참여 후기, 동행 모집, 소통 피드, 키워드 실시간 검색 및 응원 픽셀 하트(❤️)

4. **❤️ 마이페이지 (My Page - 역할별 대시보드)**
   - **🙋‍♂️ 일반 참가자 (`ROLE_USER`)**: 내 신청 봉사 현황, 기부 내역, 온기 온도계(+0.5°C) 및 LV1~LV5 뱃지 도감
   - **🏢 봉사 주최측 (`ROLE_ORGANIZER`)**: 신규 공고/기금 등록, 참가 신청자 1초 승인/거절 및 활동 완료 출석 처리

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
