# 🏗️ Pixel Care 시스템 아키텍처 (ARCHITECTURE.md)

픽셀 케어 (Pixel Care) 플랫폼의 직접 관리형 봉사 & 단체 기금 모금 아키텍처 및 시스템 명세서입니다.

---

## 🛠️ 1. 전체 플랫폼 아키텍처

```
[ Client (React + Vite) ]
     │
     ├── 🏠 홈 (Solar LLM 큐레이터)
     ├── 🎁 선행하기 (봉사 신청 & 단체 전용 기금 모금)
     ├── 💬 커뮤니티 (선행 후기 & 동행 모집)
     └── 🗺️ 성장의 길 (뱃지 도감 & 온기 온도계)
            │
            ▼ (HTTP REST API / JSON)
[ Spring Boot Backend ]
     │
     ├── Volunteer Domain (com.pixelcare.volunteer) ───► 봉사 공고 & 단체 기금 직접 관리
     ├── Community Domain (com.pixelcare.community)
     └── AI Domain (com.pixelcare.ai) ────────────────► Upstage Solar LLM
            │
            ▼ (Spring Data JPA + Flyway)
[ Database Layer ]
     └── MySQL 8.0 / H2 In-Memory DB
```

---

## 📂 2. 백엔드 패키지 구조 (`backend/src/main/java/com/pixelcare/`)

```
backend/src/main/java/com/pixelcare/
├── PixelCareApplication.java         # 메인 실행 파일
├── config/                           # CorsConfig, DataLoader, FlywayConfig
├── volunteer/                        # 봉사 공고 & 단체 전용 기금 모금 도메인
│   ├── Volunteer.java                # 봉사/기획 JPA 엔티티
│   ├── VolunteerRequestDto.java      # 등록 요청 DTO
│   ├── VolunteerResponseDto.java     # 응답 DTO
│   ├── VolunteerRepository.java      # JPA 리포지토리
│   ├── VolunteerService.java         # 봉사/기금 모금 서비스
│   └── VolunteerController.java      # REST 컨트롤러 (/api/volunteers)
├── community/                        # 커뮤니티 게시판 도메인
│   ├── Post.java                     # 게시글 JPA 엔티티
│   ├── PostService.java              # 게시글 서비스
│   └── PostController.java           # REST 컨트롤러 (/api/posts)
└── ai/                               # Upstage AI 도메인
    ├── UpstageApiService.java        # Upstage Solar LLM 파이프라인
    └── AiController.java             # REST 컨트롤러 (/api/ai)
```
