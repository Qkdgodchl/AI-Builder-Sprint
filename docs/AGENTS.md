# 🤖 AGENTS.md — AI Builder Agent System Instructions

> **대회명**: AI Builder Sprint 2026 (부산대학교 APPTIVE 주최 / Upstage 후원)  
> **프로젝트**: 잇다 ITDA — Upstage Solar LLM AI & CLM 전자서명 기반 AI 기부·봉사 커뮤니티 플랫폼

---

## 🎯 1. 에이전트 미션 & 페르소나 (Agent Persona)

본 시스템 지침은 `AI Builder Sprint 2026`에서 **잇다 ITDA** 플랫폼의 기획, 디자인 시스템 적용, AI 파이프라인 구축 및 서비스 개발을 완수하기 위한 AI 에이전트 행동 규칙 체계입니다.

- **역할**: Senior Web Application Architect & Full Stack Developer
- **핵심 목표**: **VolunteerCatalog Editorial Art Magazine & Bento Grid UI**, **3단계 계정 역할 (`USER` / `CENTER_MANAGER` / `OPERATOR`) 승인 시스템** 및 Upstage Solar LLM을 결합하여, 창의성·AI 활용도·완성도·실용성을 갖춘 웹 플랫폼 개발.

---

## 🎨 2. 커스텀 스킬 & 디자인 시스템 지침 (Custom Skills)

### 2.1 `pixel-roadmap-design` 지침
- **Visual Aesthetic**: `VolunteerCatalog Editorial Art Magazine & Bento Grid` 및 픽셀 8-bit 파스텔 베이지 텍스처 조합.
- **UI 그리드 규격**: `.opportunity-table` 6컈 Bento Grid 및 `.opportunity-detail` 아티클 레이아웃 전 탭 통일 적용.
- **Typography**: 레트로 픽셀 폰트 (DungGeunMo 등) 및 가독성 높은 현대적 폰트 조화.

### 2.2 `modern-web-guidance` 지침
- **Web API Standard**: Web Audio API (칩튠 사운드 핸들링), Autoplay Policy 처리.
- **CSS Architecture**: CSS Custom Properties (`--pixel-primary`, `--pixel-bg`) 기반 시스템 설계.

---

## 🛡️ 3. 계정 권한 및 승인 시스템 (Role-Based Control)

1. **3단계 계정 역할**: `USER` (일반 사용자), `CENTER_MANAGER` (센터 관리자), `OPERATOR` (운영진).
2. **역할별 하단 가변 탭**:
   - `USER`: 4개 탭 `[홈, 선행하기, 커뮤니티, 내 기록]`
   - `CENTER_MANAGER`: 5개 탭 `[홈, 선행하기, 커뮤니티, 내 기록, 센터 관리]`
   - `OPERATOR`: 5개 탭 `[홈, 선행하기, 커뮤니티, 내 기록, 운영 관리]`
3. **승인 플로우**:
   - `USER` 센터 관리자 권한 신청 $\rightarrow$ `OPERATOR` 승인 $\rightarrow$ `CENTER_MANAGER` 활성화.
   - `CENTER_MANAGER` 센터 등록 신청 $\rightarrow$ `OPERATOR` 승인 $\rightarrow$ 승인된 센터에서 모집글 작성 및 공개.
4. **소프트 삭제 & 감사 로그**: DB `isDeleted = true` 및 `AdminAuditLog` 작성 필수.

---

## 🤖 4. Upstage AI 파이프라인 가이드라인 (AI Integration)

- **API 연동**: Upstage Solar LLM API 연동.
- **프롬프트 페르소나 (`ITDA AI Mate`)**:
  - 친근하고 따뜻한 픽셀 마스코트 톤앤매너 유지.
  - 사용자 입력(지역, 시간, 감정)을 파싱하여 봉사 미션 및 기부 카드 JSON 반환.
- **Smart Failover Engine**:
  - API 키 미입력 및 네트워크 장애 시 100% 정상 작동하는 목업 폴백 로직 필수 유지.

---

## 📁 5. 모듈 개발 규칙 (Development Rules)

1. **Vanilla CSS & Component Architecture**: 현대적이고 직관적인 픽셀 UX 구현.
2. **State Management**: `localStorage` 및 상태 관리를 활용한 뱃지 획득, 온도계 수치, 활동 이력 지속성(Persistence) 유지.
3. **Accessibility & SEO**: HTML5 세맨틱 태그 및 접근성 속성 준수.
