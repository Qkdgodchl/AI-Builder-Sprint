# 🏗️ Pixel Care 시스템 아키텍처 (ARCHITECTURE.md)

픽셀 케어(Pixel Care) 시스템 전체의 기술 아키텍처 및 계층별 폴더 구조 명세서입니다.

---

## 🛠️ 1. 전체 기술 아키텍처 (Overall Architecture)

```
[ Client (Browser) ]
     │
     ├── React 18 (TypeScript + Vite)
     ├── Pixel Aesthetics (Vanilla CSS & Web Audio API)
     └── REST Client (fetch API)
            │
            ▼ (HTTP / REST API - Port 8080)
[ Spring Boot Backend Server ]
     │
     ├── Volunteer Domain (com.pixelcare.volunteer) ───► 1365 Open API Gateway
     ├── Community Domain (com.pixelcare.community)
     └── AI Domain (com.pixelcare.ai) ────────────────► Upstage Solar LLM API
            │
            ▼ (Spring Data JPA)
[ Database Layer ]
     ├── Flyway DB Migration (db/migration/V1__...)
     └── MySQL 8.0 / H2 In-Memory DB
```

---

## 📂 2. 백엔드 패키지 구조 (`backend/src/main/java/com/pixelcare/`)

```
backend/src/main/java/com/pixelcare/
├── PixelCareApplication.java         # 메인 애플리케이션 실행 파일
├── config/                           # 공통 설정 클래스
│   ├── CorsConfig.java               # 프론트엔드 CORS 허용 설정
│   └── DataLoader.java               # 초기 시딩 데이터 자동 생성
├── volunteer/                        # [팀원 B 담당] 봉사 & 기부 도메인
│   ├── Volunteer.java                # JPA 엔티티
│   ├── VolunteerRequestDto.java      # 요청 DTO
│   ├── VolunteerResponseDto.java     # 응답 DTO
│   ├── VolunteerRepository.java      # JPA 리포지토리
│   ├── VolunteerService.java         # 봉사 서비스
│   ├── Gov1365ApiService.java       # 1365 공공데이터 XML 파서
│   └── VolunteerController.java      # REST 컨트롤러 (/api/volunteers)
├── community/                        # [팀원 A 담당] 커뮤니티 게시판 도메인
│   ├── Post.java                     # 게시글 JPA 엔티티
│   ├── PostRequestDto.java           # 게시글 요청 DTO
│   ├── PostResponseDto.java          # 게시글 응답 DTO
│   ├── PostRepository.java           # 게시글 JPA 리포지토리
│   ├── PostService.java              # 게시글 서비스
│   └── PostController.java           # REST 컨트롤러 (/api/posts)
└── ai/                               # [팀원 C 담당] Upstage AI 도메인
    ├── UpstageApiService.java        # Upstage Solar LLM API 파이프라인
    └── AiController.java             # REST 컨트롤러 (/api/ai)
```

---

## 📂 3. 프론트엔드 폴더 구조 (`frontend/src/`)

```
frontend/src/
├── App.tsx                           # 메인 애플리케이션 탭 조율
├── App.css / index.css               # 레트로 픽셀 디자인 시스템 & 토큰
├── main.tsx                          # React 엔트리포인트
├── components/                       # 화면 컴포넌트
│   ├── common/                       # 공통 컴포넌트 (Header, Modal, Toast)
│   ├── volunteer/                    # 봉사/기부 (VolunteerCatalog, RegisterVolunteerModal)
│   ├── diary/                        # 커뮤니티 (PixelDiary - 피드, 댓글, 검색)
│   ├── ai/                           # AI 챗봇 (PixelAiMate)
│   └── roadmap/                      # 로드맵 (RoadmapMap - S자 도감, 뱃지)
├── services/                         # REST API 통신 모듈
│   ├── volunteerApi.ts               # 봉사 API 서비스
│   ├── communityApi.ts               # 커뮤니티 API 서비스
│   └── soundFx.ts                    # Web Audio API 8-bit 효과음
└── types/                            # 공통 TypeScript 타입 인터페이스
    └── index.ts
```

---

## 🗄️ 4. 데이터베이스 ERD & Flyway 마이그레이션

- **`volunteers` 테이블**: 봉사 공고 및 기부 펀딩 프로젝트 저장.
- **`volunteer_tags` 테이블**: 봉사 관련 태그 (1365 연동, 시간 인정 등).
- **`posts` 테이블**: 커뮤니티 게시글 (제목, 내용, 작성자, 카테고리, 좋아요, 조회수).
- **`comments` 테이블**: 게시글 연동 소통 댓글 (`post_id` FK).
- **Flyway 마이그레이션**: `backend/src/main/resources/db/migration/`에 버전별 DDL 관리.
