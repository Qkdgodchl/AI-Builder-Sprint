# 🏗️ Pixel Care 시스템 아키텍처 (ARCHITECTURE.md)

픽셀 케어 (Pixel Care) 플랫폼의 계정 역할(Role: `ROLE_USER` / `ROLE_ORGANIZER`) 분리 아키텍처 및 폴더 구조 명세서입니다.

---

## 🛠️ 1. 전체 계정 권한 아키텍처 (Role-Based Access Control)

```
[ Client (React + Vite) ]
     │
     ├── 🏠 홈 (Upstage AI 픽셀 큐레이터)
     ├── 🎁 선행하기 (봉사 공고 & 단체 전용 기금)
     ├── 💬 커뮤니티 (선행 후기 & 동행 모집)
     └── ❤️ 마이페이지 (My Page - Role-Based Switcher)
            │
            ├── 🙋‍♂️ 일반 참가자 (ROLE_USER): 내 신청 봉사, 기부 내역, 온도계 & 뱃지 도감
            └── 🏢 주최측/단체 (ROLE_ORGANIZER): 신규 공고 등록, 신청자 승인/완료 처리, 기금 대시보드
```

---

## 📂 2. 백엔드 패키지 구조 (`backend/src/main/java/com/pixelcare/`)

```
backend/src/main/java/com/pixelcare/
├── PixelCareApplication.java         # 메인 실행 파일
├── config/                           # CorsConfig, DataLoader, SecurityConfig
├── user/                             # 유저 & 역할 관리 (User, Role: ROLE_USER | ROLE_ORGANIZER)
├── volunteer/                        # 봉사 공고 & 단체 기금 & 신청자 승인 도메인
│   ├── Volunteer.java                # 봉사/기부 엔티티
│   ├── VolunteerApplication.java     # 유저 봉사 신청 엔티티
│   ├── VolunteerService.java         # 신청/승인/완료 처리 비즈니스 로직
│   └── VolunteerController.java      # REST 컨트롤러 (/api/volunteers, /api/organizer)
├── community/                        # 커뮤니티 게시판 도메인
└── ai/                               # Upstage AI Solar LLM 도메인
```

---

## 📂 3. 프론트엔드 폴더 구조 (`frontend/src/`)

```
frontend/src/
├── App.tsx                           # 4대 탭 (홈, 선행하기, 커뮤니티, 마이페이지)
├── components/
│   ├── common/                       # Header, Modal, Toast
│   ├── volunteer/                    # 봉사 공고 & 단체 기금 (VolunteerCatalog)
│   ├── diary/                        # 픽셀 커뮤니티 (PixelDiary)
│   ├── ai/                           # Upstage AI 챗봇 (PixelAiMate)
│   └── mypage/                       # [마이페이지 탭 역할 분리 컴포넌트]
│       ├── MyPageTab.tsx             # 마이페이지 메인 스위처
│       ├── UserMyPage.tsx            # 🙋‍♂️ 일반 참가자 뷰 (내 봉사, 온도계, 뱃지 도감)
│       └── OrganizerMyPage.tsx       # 🏢 봉사 주최측/단체 뷰 (공고 등록, 신청자 승인 관리)
├── services/                         # REST API 통신 모듈 (volunteerApi, communityApi, aiApi)
└── types/                            # TypeScript interfaces (UserRole, ApplicationStatus)
```
