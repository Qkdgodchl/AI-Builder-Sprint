# 📜 픽셀 케어 (Pixel Care CLM)

> **AI Builder Sprint 2026 참가작** (부산대학교 APPTIVE 주최 / Upstage 후원)  
> **AI 기반 선행 의향 구조화 및 약정 생성·전자서명·증빙·갱신 전과정 관리 CLM 플랫폼**

---

## 💡 프로젝트 소개 (Project Overview)

**픽셀 케어 (Pixel Care CLM)**는 **Upstage Solar LLM AI**와 **모두싸인(Modusign) 전자서명**을 결합하여, 구두나 말로만 흩어져 있던 사용자의 기부·봉사·유산기부 의사를 계약 가능한 데이터로 구조화하고 **약정서 생성부터 전자서명, 증빙 보관, 이행 관리, 갱신까지 관리하는 선행 약정 CLM(Contract Lifecycle Management) 플랫폼**입니다.

---

## 🌟 5대 선행 신청 라인업 (5 Action Lineups)

1. **🤝 자원봉사 신청**: 1365 공공데이터 연동 봉사 및 자원봉사 참여 약정서 / 개인정보 동의서 서명
2. **❤️ 일반 기부 (일시/정기)**: 기부 약정서, 세액공제 동의서 생성 및 정기후원 납부·갱신 관리
3. **🏛️ 부산 고향사랑기부**: 답례품 선택, 기부 조건 확인 및 busanlove.kr 연계 사전 약정
4. **🏺 문화유산 후원**: 문화재 복원/보존 사업 선택 및 문화유산 후원 약정서 자동 생성
5. **📜 유산기부 사전 의향**: 유산기부 사전 의향서 구조화 및 단체/전문상담 연결 약정 파이프라인

---

## 🤖 Upstage AI & CLM 기술 연동 (AI Pipeline)

- **Upstage Solar LLM**: 대화형 선행 의향 파싱, 누락 항목 추가 질문, 약정서 초안 문구 생성
- **Upstage Information Extract**: 단체 등록 약정서 필드 추출 및 서명 문서 핵심 정보 자동 검증
- **Upstage Document Parse**: PDF/이미지 종이 약정서 템플릿 디지털화
- **모두싸인 (Modusign) API & Webhook**: 전자서명 요청 링크 발급, Webhook 실시간 동기화, 서명 완료 PDF 및 감사추적인증서 저장

---

## 📚 상세 개발 & 협업 문서 (Documentation)

- [📜 통합 기획 & 기능 명세서 (PLAN.md)](docs/PLAN.md)
- [🤝 팀 기능별 협업 가이드라인 (TEAM_CONVENTIONS.md)](docs/TEAM_CONVENTIONS.md)
- [📡 CLM REST API 명세서 (API_SPEC.md)](docs/API_SPEC.md)
- [🏗️ 시스템 아키텍처 & ERD 명세서 (ARCHITECTURE.md)](docs/ARCHITECTURE.md)

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
