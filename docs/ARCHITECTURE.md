# 🏗️ 잇다 ITDA 시스템 아키텍처 명세서 (ARCHITECTURE.md)

> **대회명**: AI Builder Sprint 2026 (부산대학교 APPTIVE 주최 / Upstage 후원)  
> **프로젝트명**: 잇다 ITDA

---

## 🛠️ 1. 전체 계정 권한 & 가변 하단 탭 아키텍처 (Role-Based Dynamic Navigation)

```text
                                [ Client: React + TypeScript + Vite ]
                                                  │
         ┌────────────────────────────────────────┼────────────────────────────────────────┐
         │                                        │                                        │
  🙋‍♂️ 일반 사용자 (USER)               🏢 센터 관리자 (CENTER_MANAGER)              🛡️ 운영진 (OPERATOR)
   [ 하단 4대 탭 + CONNECT ]               [ 하단 5대 탭 ]                        [ 하단 5대 탭 ]
  ├── 🏠 홈 (AI 큐레이터)                   ├── 🏠 홈                              ├── 🏠 홈
  ├── 🎁 선행하기                          ├── 🎁 선행하기                        ├── 🎁 선행하기
  ├── 💬 커뮤니티                          ├── 💬 커뮤니티                        ├── 💬 커뮤니티
  ├── 📜 내 기록                           ├── 📜 내 기록                         ├── 📜 내 기록
  └── 🤝 CONNECT                           └── 🏢 센터 관리                        └── 🛡️ 운영 관리
       (선행 역제안, 이웃 응원)                  (대시보드, 모집글/신청자 관리)          (권한/센터 승인, 삭제, 감사로그)
```

---

## 🤖 2. Upstage Solar LLM & CLM 전자서명 파이프라인

```text
[ 사용자 자연어 대화 ] ──> [ Upstage Solar LLM ] ──> [ 고정 JSON 의향 파싱 ] ──> [ CONNECT 역제안 초안 ]
                                                              │
                                                              ▼
[ 모두싸인 전자서명 ] <── [ CLM 약정서 & 약정 버전 ] <── [ 봉사/기부 약정 자동 생성 ]
         │
         ▼
[ Webhook 상태 동기화 ] ──> [ PDF 생성 & SHA-256 감사추적 암호화 보관 ]
```

1. **Upstage Solar LLM 파이프라인**: 대화문에서 지역, 활동 시간, 기부 주기, 금액, 답례품 희망 여부를 JSON 스키마로 추출 (`UpstageApiClient`, `AiConsultationService`).
2. **Smart Failover Engine**: API 키 미설정 또는 네트워크 장애 시 100% 정상 가동되는 내장 로컬 폴백 엔진.
3. **CLM & 전자서명**: 약정서 자동 생성, 모두싸인 API 연동, Webhook 수신, PDF SHA-256 해시 검증 보관 (`ClmDocumentArchiveService`).

---

## 🔄 3. 승인 및 권한 워크플로우 (Approval Workflows)

1. **센터 관리자 권한 신청**: `USER` $\rightarrow$ `POST /api/v1/manager-applications` ➔ `OPERATOR` 승인 ➔ `CENTER_MANAGER` 롤 변경 및 5탭 전환.
2. **센터 등록 신청**: `CENTER_MANAGER` $\rightarrow$ `POST /api/v1/organization-applications` ➔ `OPERATOR` 승인 ➔ 센터 `APPROVED`.
3. **모집글 게시**: `CENTER_MANAGER` $\rightarrow$ `POST /api/v1/opportunities` (PUBLISHED 시 유저 `선행하기` 탭 노출).
4. **CONNECT 선행 역제안**: 유저가 아이디어 작성 ➔ 이웃 응원(`Support`) ➔ 센터 수락(`Claim`) ➔ 정식 공고 개설.

---

## 📂 4. 백엔드 패키지 구조 (`backend/src/main/java/com/pixelcare/`)

```text
backend/src/main/java/com/pixelcare/
├── PixelCareApplication.java         # Spring Boot 메인 실행 파일
│
├── global/                            # 🌐 전역 공통 시스템
│   ├── config/                        # Spring Boot Config (WebConfig)
│   ├── common/                        # ApiResponse<T> 표준 응답 래퍼, KeyExtractUtils
│   └── error/                         # GlobalExceptionHandler 전역 예외 처리
│
├── config/                            # ⚙️ 부트스트랩 데이터 시딩
│   ├── DataLoader.java                # 봉사·기부 원천 데이터(volunteers) 시딩
│   └── DevelopmentBootstrap.java      # 데모 사용자, 센터, 모집글 자동 시딩
│
└── domain/                            # 🍯 기능 도메인 모듈
    ├── ai/                            # 🤖 Upstage Solar LLM 연동, JSON 파싱, Smart Failover
    ├── clm/                           # 📝 CLM 약정서, 모두싸인 Webhook, PDF SHA-256 보관
    ├── user/                          # 📜 유저 프로필, 온기 온도계, warmth_events, 뱃지 도감
    ├── management/                    # 🏢/🛡️ 센터 관리자/센터 승인, 대시보드, 감사 로그
    ├── opportunity/                   # 🎁 봉사/기부 모집글 작성 및 게시
    ├── application/                   # 📋 공고 신청, 출석 처리, 약정 갱신
    ├── connect/                       # 🤝 CONNECT 선행 역제안 및 Support/Claim
    ├── community/                     # 💬 커뮤니티 후기, 댓글, 좋아요, 소프트 삭제(isDeleted)
    ├── journal/                       # 📖 봉사 일지
    ├── news/                          # 📰 미담 뉴스(Good News) 큐레이팅
    ├── stats/                         # 📊 실시간 플랫폼 종합 통계 대시보드
    └── file/                          # 📁 증빙서류, 후기 사진, PDF 문서 파일 저장소
```

---

## 📂 5. 프론트엔드 폴더 구조 (`frontend/src/`)

```text
frontend/src/
├── App.tsx                           # 3단계 역할별 하단 가변 탭 라우터
├── components/
│   ├── ai/                           # 🤖 ITDA AI Mate (대화형 AI 큐레이터)
│   ├── auth/                         # 🔐 로그인 & 회원가입
│   ├── center/                       # 🏢 센터 관리 (대시보드, 모집글 작성, 출석 승인)
│   ├── common/                       # Header, Modal, Toast
│   ├── connect/                      # 🤝 CONNECT (선행 아이디어 제안 & 응원/개설)
│   ├── diary/                        # 📖 봉사 일지
│   ├── home/                         # 🏠 홈 (Upstage AI 픽셀 큐레이터 진입점)
│   ├── news/                         # 📰 미담 뉴스
│   ├── operator/                     # 🛡️ 운영 관리 (권한/센터 승인, 소프트 삭제, 감사로그)
│   ├── roadmap/                      # 🗺️ 성장의 길 & 뱃지 도감
│   ├── user/                         # 📜 내 기록 (활동 이력, 온기 온도계, 뱃지 도감)
│   └── volunteer/                    # 🎁 선행하기 (봉사/기부 카탈로그 & 신청)
├── services/                         # REST API 모듈 (connectApi, clmApi, aiApi 등 14개)
├── hooks/                            # 커스텀 React Hook
├── types/                            # TypeScript 타입 명세서
└── utils/                            # 유틸리티 함수
```
