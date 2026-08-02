# 👾 잇다 ITDA (Pixel Care)

> **Upstage Solar LLM AI & CLM 전자서명 기반 AI 기부·봉사 커뮤니티 플랫폼**  
> **AI Builder Sprint 2026** (부산대학교 APPTIVE 주최 / Upstage 후원)

![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.0-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Upstage](https://img.shields.io/badge/AI-Upstage%20Solar-purple?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Render-4169E1?style=flat-square&logo=postgresql&logoColor=white)

---

## 📌 1. 프로젝트 이름 & 한 줄 소개 (Project Title & Summary)

**잇다 ITDA (Pixel Care)**는 **VolunteerCatalog Editorial Bento Grid UI**와 **Upstage Solar LLM AI** 기술을 결합하여, 사용자의 선행 의향을 자연어로 파싱하고 봉사·기부 약정의 체결부터 갱신, 전자서명 보관, 그리고 원하는 선행을 역제안하고 센터가 프로그램을 개설하는 **CONNECT(온기 잇다)**까지 원스톱으로 제공하는 혁신적인 선행 커뮤니티 플랫폼입니다.

---

## 🎯 2. 기획 배경 및 문제 정의 (Background & Problem)

### 🚨 Problem: 기존 봉사/기부 생태계의 4대 한계점
1. **파편화된 공고 & 탐색 피로도**: 수많은 사이트에 공고가 파편화되어 있어 나에게 맞는 봉사를 찾기 어려움.
2. **수동적 공급자 중심 구조**: 봉사자가 "원하는 봉사"를 직접 제안할 창구가 없어 센터 위주의 수동적 참여에 의존.
3. **복잡한 약정 & 이탈**: 정기후원이나 장기 봉사 약정 체결 시 서류 작업과 갱신 절차가 복잡하여 도중 이탈률이 높음.
4. **신뢰성 & 무결성 부족**: 허위 모집글 검증 수단 부재 및 작성된 약정서의 원본 무결성 증명 불가.

### 💡 Solution: 잇다 ITDA의 4대 혁신 솔루션
1. **🤖 Upstage Solar LLM AI 대화형 큐레이팅 (`Pixel AI Mate`)**: 자연어 대화 몇 마디로 맞춤 봉사를 추천받고, 약정 조건을 표준 JSON으로 자동 구조화.
2. **🤝 CONNECT (온기 잇다 — 선행 역제안 생태계)**: 봉사자가 원하는 아이디어를 자유롭게 올리고, 이웃의 응원(`Support`)과 센터의 수락(`Claim`)으로 실제 프로그램화.
3. **📝 CLM & 모두싸인 API 전자서명**: AI 파싱 약정서 자동 생성, 카카오톡/이메일 전자서명, Webhook 실시간 동기화, SHA-256 PDF 감사추적 보관.
4. **🛡️ 3단계 계정 역할 (`USER` / `CENTER_MANAGER` / `OPERATOR`) & 신뢰성 제어**: 2단계 승인 체계, 소프트 삭제(`isDeleted = true`), 운영진 감사 로그(`AdminAuditLog`) 완비.

---

## ✨ 3. 주요 기능 (Key Features)

### 1. 🤖 Upstage Solar LLM AI 파이프라인 (`Pixel AI Mate`)
- **자연어 선행 큐레이팅**: "주말에 부산 해운대에서 할 수 있는 봉사 추천해줘" 등 자연어 대화 분석 및 맞춤 미션 추천.
- **의향 구조화 (JSON Extraction)**: 희망 지역, 활동 시간, 감정, 기부 주기, 금액, 답례품 희망 여부를 고정 JSON 스키마로 추출.
- **Opt-in Consent & Smart Failover**: 외부 전송 동의 시각/제공자 감사 정보 보관, 장애 시 100% 가동되는 내장 로컬 폴백 엔진.

### 2. 🤝 CONNECT (온기 잇다 — 선행 역제안 생태계)
- **자유 역제안**: 일반 사용자가 원하는 봉사/기부 활동 아이디어를 작성하여 플랫폼에 등록.
- **AI 챗봇 연동 (`connectDraft`)**: `Pixel AI Mate` 대화 중 도출된 미션을 즉시 CONNECT 초안으로 자동 연결.
- **이웃 응원 (`Support`) & 센터 수락 (`Claim`)**: 유저들의 응원을 모으면 센터 관리자(`CENTER_MANAGER`)가 수락하여 실제 모집 공고로 정식 개설.

### 3. 📝 CLM 전자서명 & 모두싸인 API 연동
- **약정서 자동 생성**: AI 큐레이팅 결과를 바탕으로 약정서(`Commitment`) 및 약정 버전(`CommitmentVersion`) 자동 생성.
- **모두싸인 API & Webhook**: 전자서명 보안 링크 생성 및 카카오톡/이메일 서명 요청, 실시간 Webhook 상태 동기화.
- **PDF & SHA-256 감사추적**: 완료 서명 PDF 생성 및 원본 무결성 검증용 SHA-256 해시 보관 (`ClmDocumentArchiveService`).
- **약정 갱신 및 변경**: 정기후원 만료 전 갱신(`renewalDueAt`) 및 약정 조건 변경 요청(`CommitmentChangeRequest`) 관리.

### 4. 🛡️ 3단계 계정 역할 & 역할별 가변 하단 탭 (Dynamic Navigation)
- **일반 사용자 (`USER`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 ]` (4대 탭)
- **센터 관리자 (`CENTER_MANAGER`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 | 🏢 센터 관리 ]` (5대 탭)
- **운영진 (`OPERATOR`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 | 🛡️ 운영 관리 ]` (5대 탭)

---

## 🛠️ 4. 기술 스택 (Tech Stack)

| 구분 | 사용 기술 명세 |
| :--- | :--- |
| **Frontend** | React 18, TypeScript, Vite, Editorial Bento Grid CSS |
| **Backend** | Java 21, Spring Boot 3.3.4, Spring Data JPA, JdbcTemplate |
| **Database** | PostgreSQL (Render Deployment), MySQL 8.4 LTS (Local Docker) |
| **AI LLM** | Upstage Solar LLM API (`solar-1-mini-chat`) |
| **E-Signature** | 모두싸인 (Modusign) Webhook API & PDF SHA-256 Archive |
| **DB Migration** | Spring Boot Native SQL Initialization (`spring.sql.init.platform=postgresql`) |
| **Testing** | JUnit 5, Gradle Test Runner |

---

## 👥 5. 팀원 소개 및 역할 (Team Members)

| 이름 | 역할 (R&R) | 담당 업무 |
| :---: | :---: | :--- |
| **전동훈** | **Backend & Infra Lead** | • Spring Boot 백엔드 아키텍처 설계 및 PostgreSQL Native Initialization 구축<br>• Upstage Solar LLM 연동, CLM 전자서명 및 모두싸인 Webhook 파이프라인 개발<br>• 3단계 권한 승인 워크플로우, 소프트 삭제 및 감사 로그 시스템 완비 |
| **권윤재** | **Frontend & UI/UX Lead** | • React 18 + TypeScript 기반 Editorial Bento Grid 디자인 시스템 구축<br>• 3단계 계정 역할별 가변 하단 탭 내비게이션 라우팅 시스템 개발<br>• CONNECT(온기 잇다) 역제안 UI 및 선행 인증 커뮤니티 컴포넌트 구현 |
| **이영민** | **Full Stack & AI Specialist** | • Upstage Solar LLM 프롬프트 페르소나 설계 및 JSON 의향 파싱 엔진 구축<br>• 로컬 Smart Failover Engine 개발 및 외부 AI 전송 동의(Opt-in) 제어<br>• 센터 관리자 대시보드 및 실시간 플랫폼 통계 기능 구현 |

---

> **AI Builder Sprint 2026** — 잇다 ITDA (Pixel Care)
