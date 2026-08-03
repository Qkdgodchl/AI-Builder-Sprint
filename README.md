# 👾 잇다 ITDA

> **Upstage Solar LLM AI & CLM 전자서명 기반 AI 기부·봉사 커뮤니티 플랫폼**  
> **AI Builder Sprint 2026** (부산대학교 APPTIVE 주최 / Upstage 후원)

[![Vercel](https://img.shields.io/badge/Vercel-Live--Demo-000000?style=flat-square&logo=vercel&logoColor=white)](https://itdafront.vercel.app)
![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.0-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Upstage Solar](https://img.shields.io/badge/AI-Upstage%20Solar%20LLM-purple?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Render-4169E1?style=flat-square&logo=postgresql&logoColor=white)

---

## 🔗 배포 주소 및 라이브 데모 계정

| 구분 | 주소 및 정보 |
|---|---|
| **서비스 웹 (프론트엔드)** | [https://itdafront.vercel.app](https://itdafront.vercel.app) |
| **API 서버 (백엔드)** | `https://ai-builder-sprint.onrender.com` |
| **데모 계정 (센터 관리자)** | `manager@pixelcare.demo` / `Manager123!` |
| **데모 계정 (운영진)** | `operator@pixelcare.local` / `Operator123!` |
| **데모 계정 (일반 사용자)** | `donor@pixelcare.demo` / `Donor123!` |

> 💡 **백엔드 접속 안내**: 백엔드는 Render 무료 인스턴스로 접속이 없으면 절전 상태가 됩니다. 첫 요청 시 백엔드 상향까지 50초 남짓 걸릴 수 있습니다.

---

## 📌 프로젝트 개요 및 한 줄 소개 (Project Summary)

**잇다 ITDA**는 **VolunteerCatalog Editorial Bento Grid UI**와 **Upstage Solar LLM AI** 기술을 결합하여, 사용자의 선행 의향(일반 봉사/기부, **고향사랑기부제**, **유산기부**, **문화유산 후원**)을 자연어로 파싱하고 봉사·기부 약정의 체결부터 갱신, 전자서명 보관, 그리고 원하는 선행을 역제안하고 센터가 프로그램을 개설하는 **CONNECT(온기 잇다)**까지 원스톱으로 제공하는 혁신적인 선행 커뮤니티 플랫폼입니다.

---

## 🎯 1. 기획 배경 및 문제 정의 (Background & Problem)

### 👥 타겟 사용자 (Target Users)
- **일반 사용자 (`USER`)**: 나에게 꼭 맞는 봉사/기부 프로그램을 찾고, 선행 아이디어를 직접 역제안하고 싶은 봉사자 및 후원자.
- **센터 관리자 (`CENTER_MANAGER`)**: 봉사/기부 프로그램을 개설하고 신청자 관리, 출석 승인 및 모집 현황을 관리하는 복지/봉사 기관 담당자.
- **운영진 (`OPERATOR`)**: 센터 및 관리자 승인 검토, 허위/불법 게시물 소프트 삭제, 플랫폼 전체 감사 로그를 관리하는 플랫폼 운영 주체.

### 🚨 문제 정의 (Problem Statement)
1. **파편화된 공고 & 탐색 피로도**: 흩어진 봉사 공고 속에서 성향(지역, 시간, 감정)에 맞는 활동을 찾기 어려움.
2. **수동적 공급자 중심 구조**: 봉사자가 "원하는 봉사"를 직접 제안할 창구가 없어 센터 위주의 수동적 참여에 의존.
3. **복잡한 약정 & 이탈**: 정기후원이나 장기 봉사 약정 체결 시 서류 작업과 갱신 절차가 복잡하여 도중 이탈률이 높음.
4. **신뢰성 & 무결성 부족**: 허위 모집글 검증 수단 부재 및 작성된 약정서의 원본 무결성 증명 불가.

### 💡 솔루션 및 기대 효과 (Solution & Impact)
1. **Upstage Solar LLM 대화형 큐레이팅**: 자연어 몇 마디로 맞춤 봉사를 추천받고 약정 조건을 JSON으로 자동 구조화.
2. **CONNECT (선행 역제안 생태계)**: 유저가 원하는 선행을 역제안하면 이웃 응원(`Support`)과 센터 수락(`Claim`)으로 정식 프로그램 개설.
3. **CLM & 모두싸인 API 전자서명**: 카카오톡/이메일 전자서명, Webhook 실시간 동기화, 서명 완료 PDF SHA-256 감사추적 보관.
4. **3단계 승인 권한 체계 & 신뢰성 검증**: 2단계 승인 체계, 소프트 삭제(`isDeleted = true`), 감사 로그(`AdminAuditLog`) 완비.

---

## 🤖 2. AI 활용 증빙 (AI Integration & Implementation Proof)

### 🧠 사용 AI 모델 (AI Model)
| API | 모델 | 계약 파이프라인에서 맡는 일 |
| :--- | :--- | :--- |
| **Solar LLM** | `solar-pro3` | 대화로 약정 의사를 정리하고, 체결 후 감사 인사를 씀 |
| **Document Parse** | `document-parse` | 체결된 약정서 PDF에서 글자를 되읽음 |
| **Information Extract** | `information-extract` | 되읽은 약정서를 고정 스키마로 구조화 |

### 📍 API 사용 위치 (Code Location)
- **Solar 대화 클라이언트**: `backend/src/main/java/com/pixelcare/domain/ai/service/UpstageApiClient.java`
- **문서 AI 클라이언트**: `backend/src/main/java/com/pixelcare/domain/ai/service/UpstageDocumentClient.java`
- **대화 및 선행 큐레이팅**: `backend/src/main/java/com/pixelcare/domain/ai/service/AiMateService.java`
- **의향 파싱 & 스키마 구조화**: `backend/src/main/java/com/pixelcare/domain/ai/service/AiConsultationService.java`
- **체결본 대조 검증**: `backend/src/main/java/com/pixelcare/domain/clm/service/ClmDocumentVerificationService.java`

### 🔍 체결본 대조 검증 (Signed Document Verification)
서명이 끝났다는 사실만으로는 **무엇에 서명했는지**를 증명하지 못합니다.
잇다는 보관된 체결본을 다시 읽어, 신청할 때 정한 조건 그대로 서명됐는지 항목별로 대조합니다.

```
체결본 PDF → Document Parse(글자 추출) → Information Extract(항목 구조화)
           → DB 약정 원본과 대조 → 항목별 일치/불일치 표시
```

- 대조 항목: 약정자 · 수혜기관 · 약정 금액 · 약정 주기
- 표기 차이를 감안합니다. `"120,000 원"`은 숫자만 비교하고, `ANNUAL`은 약정서 표기인 `"연간 정기 후원"`과 맞춥니다.
- 읽어내지 못하면 결과를 지어내지 않고 `UNREADABLE`로 남깁니다.
- API: `POST /api/v1/clm/documents/{id}/verification`

### ⚙️ 프롬프트 및 설정 (Prompt & Configuration)
- **프롬프트 페르소나**: 친근하고 따뜻한 픽셀 마스코트 `Pixel AI Mate` 페르소나 적용.
- **JSON 스키마 extraction**: 사용자의 대화문에서 희망 지역, 활동 시간, 감정, 기부 주기, 금액, 답례품 희망 여부를 고정 JSON 파싱.
- **외부 전송 동의 (Opt-in Consent)**: `externalAiConsentAt` 저장을 통해 사용자 동의 시에만 외부 LLM에 데이터 전송.

### 🛡️ Smart Failover Engine (테스트·검증 산출물)
- Upstage API 키 미설정 또는 네트워크 단락 시 외부 요청 없이 100% 정상 작동하는 내장 **Smart Failover Engine**을 구현하여 장애 상황에서도 안정적인 대화 서비스 제공 (`AiMateService.java`).

---

## ✨ 3. 주요 핵심 기능 (Key Features)

### 1. 🤖 Upstage Solar LLM AI 파이프라인 (`Pixel AI Mate`)
- **자연어 선행 큐레이팅**: "주말에 부산 해운대에서 할 수 있는 봉사 추천해줘" 등 자연어 대화 분석 및 맞춤 미션 추천.
- **의향 구조화 (JSON Extraction)**: 희망 지역, 활동 시간, 감정, 기부 주기, 금액, 답례품 희망 여부를 고정 JSON 스키마로 추출.

### 2. 🤝 CONNECT (온기 잇다 — 선행 역제안 생태계)
- **자유 역제안**: 일반 사용자가 원하는 봉사/기부 활동 아이디어를 작성하여 플랫폼에 등록.
- **AI 챗봇 연동 (`connectDraft`)**: `Pixel AI Mate` 대화 중 도출된 미션을 즉시 CONNECT 초안으로 자동 연결.
- **이웃 응원 (`Support`) & 센터 수락 (`Claim`)**: 유저들의 응원을 모으면 센터 관리자(`CENTER_MANAGER`)가 수락하여 실제 모집 공고로 정식 개설.

### 3. 📝 CLM 전자서명 & 유형별 맞춤 약정 파이프라인 (모두싸인 API 연동)
- **유형별 특화 기부·봉사 약정 지원**:
  - 🌾 **고향사랑기부제 (`HOMETOWN_DONATION`)**: 지자체(부산광역시 등) 세액공제, 답례품(동백전 지역화폐 등) 자동 매칭 및 지자체 전용 약정서 생성.
  - 🏛️ **유산기부 (`LEGACY_DONATION`) & 문화유산 후원 (`CULTURAL_HERITAGE_DONATION`)**: 유산 상속 및 문화재 보전을 위한 전용 CLM 전자서명 약정서 서식 지원.
  - 🤝 **일반 봉사 & 기부 (`VOLUNTEER` / `DONATION`)**: 맞춤형 봉사 활동 및 정기/일시 기부 약정.
- **약정서 자동 생성**: AI 큐레이팅 결과를 바탕으로 약정서(`Commitment`) 및 약정 버전(`CommitmentVersion`) 자동 생성.
- **모두싸인 API & Webhook**: 전자서명 보안 링크 생성 및 카카오톡/이메일 서명 요청, 실시간 Webhook 상태 동기화.
- **PDF & SHA-256 감사추적**: 완료 서명 PDF 생성 및 원본 무결성 검증용 SHA-256 해시 보관 (`ClmDocumentArchiveService`).
- **약정 갱신 및 변경**: 정기후원 만료 전 갱신(`renewalDueAt`) 및 약정 조건 변경 요청(`CommitmentChangeRequest`) 관리.

### 4. 🛡️ 3단계 계정 역할 & 역할별 가변 하단 탭 (Dynamic Navigation)
- **일반 사용자 (`USER`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 ]` (4대 탭)
- **센터 관리자 (`CENTER_MANAGER`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 | 🏢 센터 관리 ]` (5대 탭)
- **운영진 (`OPERATOR`)** — `[ 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록 | 🛡️ 운영 관리 ]` (5대 탭)

---

## 📁 4. 주요 코드 및 프로젝트 구조 (Code Structure)

```text
AI-Builder-Sprint/
├── README.md                          # 프로젝트 메인 설명서
├── docker-compose.yml                 # 로컬 개발용 PostgreSQL Docker 설정
├── docs/                              # 상세 설계 및 명세 문서군
│   ├── AGENTS.md                      # AI 에이전트 시스템 지침 파일
│   ├── PLAN.md                        # 기획 및 로드맵
│   ├── ARCHITECTURE.md                # 시스템 아키텍처 명세서
│   ├── API_SPEC.md                    # REST API 명세서
│   ├── DB_SCHEMA.md                   # 데이터베이스 스키마 명세서
│   ├── SKILL.md                       # 디자인 시스템 스킬 지침
│   ├── TEAM_CONVENTIONS.md            # 팀 협업 가이드라인
│   └── workflow.md                    # 서비스 기획서
│
├── backend/src/main/java/com/pixelcare/
│   ├── domain/ai/                     # 🤖 Upstage Solar LLM AI & Smart Failover
│   ├── domain/clm/                    # 📝 CLM 약정서 & 모두싸인 Webhook / SHA-256
│   ├── domain/user/                   # 📜 유저, 온기 온도계, 뱃지 도감
│   ├── domain/management/             # 🏢/🛡️ 센터 관리자 & 운영진 승인/감사로그
│   ├── domain/opportunity/            # 🎁 봉사/기부 공고 관리
│   ├── domain/application/            # 📋 공고 신청 & 출석 처리
│   ├── domain/connect/                # 🤝 CONNECT 선행 역제안 & Support/Claim
│   ├── domain/community/              # 💬 커뮤니티 후기 & 소프트 삭제
│   ├── domain/journal/                # 📖 봉사 일지
│   ├── domain/news/                   # 📰 미담 뉴스 큐레이팅
│   ├── domain/stats/                  # 📊 실시간 플랫폼 통계
│   └── domain/file/                   # 📁 파일 저장소
│
└── frontend/src/
    ├── components/                    # 탭별 React 컴포넌트
    │   ├── ai/                        # 🤖 Pixel AI Mate 챗봇
    │   ├── auth/                      # 🔐 로그인 & 회원가입
    │   ├── center/                    # 🏢 센터 관리 대시보드
    │   ├── common/                    # Header, NavigationBar, Modal 등
    │   ├── connect/                   # 🤝 CONNECT 역제안 & 응원
    │   ├── diary/                     # 📖 봉사 일지
    │   ├── home/                      # 🏠 홈 (AI 큐레이터 진입점)
    │   ├── news/                      # 📰 미담 뉴스
    │   ├── operator/                  # 🛡️ 운영 관리
    │   ├── roadmap/                   # 🗺️ 성장의 길 & 뱃지 도감
    │   ├── user/                      # 📜 내 기록
    │   └── volunteer/                 # 🎁 선행하기 (봉사/기부 카탈로그)
    ├── services/                      # REST API 모듈 (16개 api 파일)
    ├── hooks/                         # 커스텀 React Hook
    ├── types/                         # TypeScript 타입 명세
    ├── utils/                         # 유틸리티 함수
    └── App.tsx                        # 3단계 역할별 가변 탭 라우터
```

---

## 🛠️ 5. 기술 스택 (Tech Stack)

| 구분 | 사용 기술 명세 |
| :--- | :--- |
| **Frontend** | React 18, TypeScript, Vite, Editorial Bento Grid CSS (Vercel Deployment) |
| **Backend** | Java 21, Spring Boot 3.3.4, Spring Data JPA, JdbcTemplate (Render Cloud) |
| **Database** | PostgreSQL (Render Deployment), PostgreSQL 16-alpine (Local Docker) |
| **AI LLM** | Upstage Solar LLM API (`solar-pro3`) |
| **E-Signature** | 모두싸인 (Modusign) Webhook API & PDF SHA-256 Archive |
| **DB Migration** | Spring Boot Native SQL Initialization (`spring.sql.init.platform=postgresql`) |
| **Testing** | JUnit 5, Gradle Test Runner |

---

## 👥 6. 팀원 소개 (Team Members)

| 이름 | 역할 (R&R) | 담당 업무 |
| :---: | :---: | :--- |
| **권윤재** | **Full Stack Developer** | • React 18 + TypeScript 기반 Editorial Bento Grid 디자인 시스템 구축<br>• 3단계 계정 역할별 가변 하단 탭 내비게이션 라우팅 및 전단 UI 개발<br>• CONNECT(온기 잇다) 역제안 프론트엔드 및 백엔드 연동 |
| **이영민** | **Full Stack Developer** | • Upstage Solar LLM 프롬프트 페르소나 설계 및 JSON 의향 파싱 엔진 구축<br>• 로컬 Smart Failover Engine 개발 및 외부 AI 전송 동의(Opt-in) 제어<br>• 센터 관리자 대시보드 및 실시간 플랫폼 통계 기능 개발 |
| **전동훈** | **Full Stack Developer** | • Spring Boot 백엔드 아키텍처 설계 및 PostgreSQL Native Initialization 구축<br>• Upstage Solar LLM 연동, CLM 전자서명 및 모두싸인 Webhook 파이프라인 개발<br>• 3단계 권한 승인 워크플로우, 소프트 삭제 및 감사 로그 시스템 구축 |

---

> **AI Builder Sprint 2026** — 잇다 ITDA
