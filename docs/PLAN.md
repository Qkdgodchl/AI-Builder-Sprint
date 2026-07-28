# 📜 픽셀 케어 (Pixel Care) 서비스 기획 & 단계별 개발 로드맵 (PLAN.md)

> **AI Builder Sprint 2026 로드맵 명세서**  
> **3단계 계정 역할 (`USER` / `CENTER_MANAGER` / `OPERATOR`) & 가변 하단 탭 모델**:  
> 일반 사용자, 센터 관리자, 운영진의 권한 및 대시보드 화면을 명확히 분리하여 승인 프로세스 및 선행 플랫폼 관리를 수행합니다. (CLM 전자서명 및 약정서 파싱은 Phase 2 배치)

---

## 📱 1. 역할별 하단 내비게이션 탭 구조 & 권한 분리

```
[ 역할별 하단 내비게이션 탭 구조 ]

1. 일반 사용자 (USER) - 4개 탭
   ├── 🏠 홈 (Home - Upstage AI 픽셀 큐레이터)
   ├── 🎁 선행하기 (Volunteer & Donation Catalog)
   ├── 💬 커뮤니티 (Pixel Community)
   └── 📜 내 기록 (My Records - 내 봉사/기부 이력, 온기 온도계 & 뱃지 도감, 센터 관리자 신청 버튼)

2. 센터 관리자 (CENTER_MANAGER) - 5개 탭
   ├── 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록
   └── 🏢 센터 관리 (Center Management - 센터 대시보드, 내 센터 관리, 모집글 작성, 신청자 승인/출석 처리)

3. 운영진 (OPERATOR) - 5개 탭
   ├── 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록
   └── 🛡️ 운영 관리 (Operator Management - 권한 요청 승인/거절, 센터 승인/거절, 게시물 소프트 삭제, 감사 로그)
```

---

## 🔄 2. 센터 관리자 및 센터 승인 프로세스

1. **센터 관리자 권한 신청**:
   - 일반 사용자(`USER`)가 `내 기록` 또는 프로필 화면에서 **[센터 관리자 신청]** 진행 (`ManagerApplication`).
   - 소속 기관명, 담당 업무, 연락처, 사업자등록번호/증빙서류 등 제출.
2. **운영진 권한 승인**:
   - 운영진(`OPERATOR`)이 `운영 관리` 탭에서 관리자 신청 목록 검토 후 승인 (`APPROVED`).
   - 승인 시 해당 사용자 역할에 `CENTER_MANAGER` 추가 및 하단에 `센터 관리` 탭 활성화.
3. **센터 등록 및 모집글 게시**:
   - `CENTER_MANAGER`가 `센터 관리` 탭에서 **센터 등록 신청** (`OrganizationApplication`).
   - 운영진 승인 후, APPROVED 상태인 센터에서 봉사/기부 모집글(`Opportunity`) 작성 및 공개.
   - 모집글이 공개(`PUBLISHED`) 상태가 되면 일반 사용자의 `선행하기` 탭 목록에 자동 노출.

---

## 🗑️ 3. 게시글 삭제 및 DB 소프트 삭제 (Soft Delete)

- 운영진(`OPERATOR`)이 허위 모집글 또는 위험/불법 커뮤니티 게시글 삭제 시:
  - 백엔드에서 운영진 권한 검증.
  - DB 하드 삭제 대신 소프트 삭제 적용: `isDeleted = true`, `status = DELETED`, `deletedBy`, `deletedAt`, `deletionReason` 저장.
  - 일반 사용자 및 센터 관리자 조회 API 검색 결과에서 즉시 제외.
  - 삭제 작업은 감사 로그(`AdminAuditLog`)에 자동 기록.

---

## 🚀 4. 단계별 개발 로드맵

### 1단계: 핵심 MVP (Current Scope)
- **회원가입 & 로그인**: 사용자 기본 정보 관리.
- **3단계 역할 권한 체계**: `USER`, `CENTER_MANAGER`, `OPERATOR`.
- **센터 관리자 신청 & 운영진 승인/거절**: `ManagerApplication` 흐름.
- **센터 등록 요청 & 운영진 승인/거절**: `OrganizationApplication` 및 센터 승인 상태 관리.
- **역할별 가변 하단 탭 구현**: USER(4탭), CENTER_MANAGER(5탭), OPERATOR(5탭).
- **봉사/기부 모집글 작성 & 선행하기 탭 자동 노출**: 모집글 공개 시 사용자 탭 반영.
- **봉사 신청 & 센터 관리자 승인/출석 완료 처리**: 온기 온도계(+0.5°C) 및 뱃지 적립.
- **운영진 게시글 소프트 삭제 & 감사 로그**: `isDeleted` 및 `AdminAuditLog`.
- **Upstage Solar LLM 챗봇 (`Pixel AI Mate`)**: 맞춤 선행 추천 및 대화 파싱.

### 2단계: CLM (Contract Lifecycle Management) 강화 (Phase 2 - Deferred)
- **전자서명 연동**: 모두싸인(Modusign) API 연동, 약정서/동의서 생성 파이프라인.
- **계약 생애주기 관리**: 약정 수정 요청, 계약 버전 관리, 서명 재요청, 정기후원 갱신, 해지.
- **Upstage AI 확장**: Information Extract (약정서 필드 추출/검증), Document Parse (PDF/이미지 종이 약정서 파싱).

### 3단계: 서비스 확장
- **선행 인증 및 배지**: 활동 인증 사진, 배지 도감 완성.
- **신고 및 제재 시스템**: 게시글/댓글 신고 수집 및 사용자 이용 정지.
- **AI 후기 초안 작성**: 봉사 완료 후 AI 자동 후기 가이드 생성.
