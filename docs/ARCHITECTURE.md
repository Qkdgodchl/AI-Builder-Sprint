# 🏗️ Pixel Care 시스템 아키텍처 (ARCHITECTURE.md)

픽셀 케어 (Pixel Care) 플랫폼의 3단계 계정 역할 (`USER` / `CENTER_MANAGER` / `OPERATOR`) 분리 아키텍처 및 시스템 구조 명세서입니다.

---

## 🛠️ 1. 전체 계정 권한 & 탭 아키텍처 (Role-Based Dynamic Navigation)

```
                                [ Client (React + Vite) ]
                                            │
         ┌──────────────────────────────────┼──────────────────────────────────┐
         │                                  │                                  │
  🙋‍♂️ 일반 사용자 (USER)          🏢 센터 관리자 (CENTER_MANAGER)         🛡️ 운영진 (OPERATOR)
   [ 하단 4대 탭 ]                    [ 하단 5대 탭 ]                   [ 하단 5대 탭 ]
  ├── 🏠 홈 (AI 큐레이터)              ├── 🏠 홈                          ├── 🏠 홈
  ├── 🎁 선행하기                     ├── 🎁 선행하기                    ├── 🎁 선행하기
  ├── 💬 커뮤니티                     ├── 💬 커뮤니티                    ├── 💬 커뮤니티
  └── 📜 내 기록                      ├── 📜 내 기록                     ├── 📜 내 기록
       (내 신청, 온도계, 뱃지)          └── 🏢 센터 관리                    └── 🛡️ 운영 관리
                                          (대시보드, 모집글/신청자 관리)      (권한/센터 승인, 삭제, 감사로그)
```

---

## 🔄 2. 승인 및 권한 부여 플로우 (Approval Workflow)

1. **센터 관리자 신청**:
   - `USER` $\rightarrow$ `POST /api/users/manager-applications` (증빙 서류, 소속 기관 정보)
   - `OPERATOR` $\rightarrow$ `PUT /api/operator/manager-applications/{id}/approve` $\rightarrow$ `USER` 역할에 `CENTER_MANAGER` 추가.
2. **센터 등록 신청**:
   - `CENTER_MANAGER` $\rightarrow$ `POST /api/center/organizations` (센터 정보, 1365 제공 여부 등)
   - `OPERATOR` $\rightarrow$ `PUT /api/operator/organization-applications/{id}/approve` $\rightarrow$ `Organization.status = APPROVED`.
3. **모집글 게시**:
   - `CENTER_MANAGER` $\rightarrow$ `POST /api/center/opportunities` $\rightarrow$ `status = PUBLISHED` 시 `선행하기` 탭 자동 공개.

---

## 📂 3. 백엔드 패키지 구조 (`backend/src/main/java/com/pixelcare/`)

```text
backend/src/main/java/com/pixelcare/
├── PixelCareApplication.java         # 메인 실행 파일 (@EnableJpaAuditing)
│
├── global/                            # 🌐 전역 공통 시스템
│   ├── config/                        # WebConfig (CORS), UpstageConfig, SecurityConfig
│   ├── common/                        # ApiResponse<T> (표준 JSON 통일 응답 래퍼)
│   ├── entity/                        # BaseTimeEntity (생성일시/수정일시/소프트 삭제 isDeleted 상속)
│   └── error/                         # GlobalExceptionHandler (전역 예외 처리)
│
└── domain/                            # 🎯 기능(도메인)별 비즈니스 로직
    ├── community/                     # 💬 커뮤니티 도메인 (Post, Comment, PostLike)
    ├── user/                          # 📜 유저 & 마이페이지 도메인 (User, UserBadge, UserTempLog)
    ├── ai/                            # 🤖 Upstage AI Solar LLM 도메인 (ChatMessage, AiMateClient)
    ├── volunteer/                     # 🎁 봉사/기부 모집글 & 신청 도메인 (Opportunity, Application)
    ├── center/                        # 🏢 센터 관리자 & 등록 센터 도메인 (Organization)
    └── operator/                      # 🛡️ 운영진 승인 및 감사 로그 도메인 (AdminAuditLog, ManagerApplication)
```

---

## 📂 4. 프론트엔드 폴더 구조 (`frontend/src/`)

```
frontend/src/
├── App.tsx                           # 역할별 하단 가변 탭 라우팅 (USER, CENTER_MANAGER, OPERATOR)
├── components/
│   ├── common/                       # Header, NavigationBar, Modal, Toast
│   ├── home/                         # 🏠 홈 (Upstage AI 픽셀 큐레이터 진입점)
│   ├── volunteer/                    # 🎁 선행하기 (봉사/기부 카탈로그 & 상세)
│   ├── community/                    # 💬 커뮤니티 (선행 후기 & 동행 모집)
│   ├── myrecords/                    # 📜 내 기록 (내 신청 봉사/기부 이력, 온기 온도계, 뱃지 도감)
│   ├── center/                       # 🏢 센터 관리 (센터 대시보드, 내 센터, 모집글 작성, 신청자 승인)
│   ├── operator/                     # 🛡️ 운영 관리 (권한 승인 대기, 센터 승인 대기, 소프트 삭제, 감사 로그)
│   └── ai/                           # 🤖 Upstage AI 챗봇 (PixelAiMate)
├── services/                         # REST API 통신 모듈 (userApi, centerApi, operatorApi, volunteerApi, aiApi)
└── types/                            # TypeScript interfaces (UserRole, Opportunity, ManagerApplication, Organization)
```
