# 🏗️ Pixel Care CLM 시스템 아키텍처 (ARCHITECTURE.md)

선행 약정 CLM (Contract Lifecycle Management) 플랫폼의 기술 아키텍처, 데이터 모델 ERD, 백엔드/프론트엔드 시스템 레이어 명세서입니다.

---

## 🛠️ 1. 전체 CLM 시스템 아키텍처

```
[ Client (React + Vite) ]
     │
     ├── 🏠 홈 (Solar LLM 대화 UI)
     ├── 🎁 선행하기 (5대 카탈로그)
     ├── 💬 커뮤니티 (인증된 선행 공유)
     └── ❤️ 내 기록 (서명 완료 PDF & 증빙)
            │
            ▼ (HTTP REST API / JSON)
[ Spring Boot Backend (CLM Engine) ]
     │
     ├── 🤖 Upstage API (Solar LLM + Information Extract + Document Parse)
     ├── 📜 CLM State Machine (Draft ➔ Signed ➔ Active ➔ Renewal)
     └── ✒️ Modusign API (전자서명 요청 & Webhook 수신)
            │
            ▼ (Spring Data JPA + Flyway)
[ Database Layer ]
     ├── MySQL 8.0 / H2 In-Memory DB
     └── Flyway Migration (V1__init_schema.sql, V2__add_clm_tables.sql)
```

---

## 🗄️ 2. 핵심 데이터 구조 & ERD (Domain Entities)

### 2.1 사용자 & 단체 (User & Organization)
- **`User`**: `userId`, `name`, `email`, `phone`, `role` (USER | ADMIN), `region`, `interests`, `createdAt`
- **`Organization`**: `organizationId`, `name`, `type`, `managerId`, `contact`, `verificationStatus`

### 2.2 선행 기회 & 약정 (Opportunity & Commitment)
- **`Opportunity`**: `opportunityId`, `organizationId`, `type` (VOLUNTEER | DONATION | GOHYANG | CULTURAL_HERITAGE | HERITAGE_WILL), `category`, `title`, `description`, `region`, `startDate`, `endDate`, `status`
- **`Commitment`**: `commitmentId`, `userId`, `organizationId`, `opportunityId`, `commitmentType`, `status` (DRAFT ~ COMPLETED 11개 상태), `amount`, `paymentCycle`, `startDate`, `endDate`, `specialConditions`, `currentVersion`, `createdAt`

### 2.3 전자서명 & 문서 (ContractDocument & SignatureRequest)
- **`ContractDocument`**: `documentId`, `commitmentId`, `documentType`, `version`, `fileUrl`, `extractedFields` (JSON), `documentHash`, `status`, `createdAt`
- **`SignatureRequest`**: `signatureRequestId`, `commitmentId`, `modusignDocumentId`, `signerId`, `status`, `requestedAt`, `completedAt`, `failureReason`
- **`Consent`**: `consentId`, `commitmentId`, `consentType`, `agreed`, `agreedAt`, `version`

### 2.4 활동 기록 & 커뮤니티 (ActivityRecord & CommunityPost)
- **`ActivityRecord`**: `activityRecordId`, `userId`, `opportunityId`, `commitmentId`, `status`, `verificationFileUrl`, `verifiedAt`
- **`CommunityPost`**: `postId`, `userId`, `activityRecordId`, `content`, `imageUrl`, `visibility`, `createdAt`

---

## 📂 3. 백엔드 패키지 구조 (`backend/src/main/java/com/pixelcare/`)

```
backend/src/main/java/com/pixelcare/
├── PixelCareApplication.java         # 메인 실행 파일
├── config/                           # CorsConfig, DataLoader, FlywayConfig
├── user/                             # 회원 & 역할 관리 (User, Organization)
├── opportunity/                      # 봉사/기부 카탈로그 (Opportunity)
├── commitment/                       # CLM 약정 & 전자서명 (Commitment, ContractDocument, SignatureRequest)
├── ai/                               # Upstage Solar LLM & Information Extract
├── webhook/                          # 모두싸인 (Modusign) Webhook 수신기
└── community/                        # 선행 인증 커뮤니티 (CommunityPost, ActivityRecord)
```

---

## 📂 4. 프론트엔드 폴더 구조 (`frontend/src/`)

```
frontend/src/
├── App.tsx                           # 4대 탭 (홈, 선행하기, 커뮤니티, 내 기록)
├── components/
│   ├── home/                         # AI 대화 & 빠른 메뉴 & 서명 대기 알림
│   ├── catalog/                      # 5대 선행 카탈로그 & 상세 신청 모달
│   ├── commitment/                   # 약정서 미리보기 & 모두싸인 서명 웹뷰
│   ├── community/                    # 인증된 선행 후기 & 인증 배지 게시글
│   ├── myrecords/                    # 서명 완료 PDF 증빙 & 감사추적서 조회
│   └── admin/                        # 단체 대시보드 (약정 승인 & 템플릿 관리)
├── services/
│   ├── aiApi.ts                      # Upstage Solar LLM 대화 API
│   ├── commitmentApi.ts              # CLM 약정 & 모두싸인 서명 API
│   ├── opportunityApi.ts             # 카탈로그 조회 API
│   └── communityApi.ts               # 커뮤니티 API
└── types/
    └── index.ts                      # CLM 인터페이스 정의
```
