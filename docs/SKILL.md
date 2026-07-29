---
name: pixel-roadmap-design
description: Master Synthesized Multi-Aesthetic Design System for Pixel Care platform. Standardized around the VolunteerCatalog Editorial Art Magazine & Bento Grid system across all tabs (Volunteer, Community, AI Mate, Roadmap).
---

# Master Synthesized UI Design System (픽셀 케어 글로벌 표준 디자인 시스템)

본 디자인 스킬은 **픽셀 케어 (Pixel Care)** 플랫폼의 최고 디자인 가이드라인입니다. **봉사 & 기부 탭 (`VolunteerCatalog` & Editorial Art Magazine)**의 벤토 그리드 및 표 매거진 디자인 규격을 커뮤니티 탭을 비롯한 플랫폼 전체의 **공통 최고 표준(Master Standard)**으로 통일하여 적용합니다.

---

## 🎨 1. 글로벌 통일 메인 디자인 시스템 (Master Standard: VolunteerCatalog Aesthetic)

### 📌 1.1 봉사/기부 & 커뮤니티 공통 잡지형 프리미엄 스펙 (`Editorial Art Magazine`)
- **디자인 모티브**: `Editorial Art Magazine` & `Bento Grid` & 픽셀 8-bit 파스텔
- **테이블 & 목록 그리드 규격**:
  - `grid-template-columns: 1.1fr 2.5fr 1fr 1.5fr 0.8fr auto`
  - **Col 1 (1.1fr)**: `opportunity-type` (분류 태그 및 픽셀 아이콘)
  - **Col 2 (2.5fr)**: `opportunity-program` (제목 `strong` + 미리보기/주관기관 `span` + 48px 썸네일)
  - **Col 3 (1.0fr)**: `opportunity-area` (작성자/지역 텍스트)
  - **Col 4 (1.5fr)**: `opportunity-keywords` (픽셀 레벨 뱃지 및 조회/하트 반응 칩)
  - **Col 5 (0.8fr)**: `opportunity-status` (작성 시각/모집 상태)
  - **Col 6 (auto)**: `opportunity-action` (액션 버튼)

### 📌 1.2 상세 보기 아티클 레이아웃 스펙 (`opportunity-detail`)
- **Back Nav**: `.detail-back-nav` (`.detail-back-button` ← 목록으로 돌아가기 & 🗑️ 삭제)
- **Hero Title**: `.detail-hero` (카테고리 칩 + 24px 타이틀 + `#키워드` 칩)
- **Facts Grid**: `<dl className="detail-facts">` (4분할 정보 박스: 작성자, 뱃지, 시각, 조회/하트)
- **Article Sections**: `<section className="detail-section">` (`01`, `02` 픽셀 번호 세션 + 파스텔 본문 박스)
- **Action Footer**: `<footer className="detail-apply-bar">` (플로팅 공유/링크 복사 & ❤️ 응원 하트 액션 바)

---

## 🎨 2. 탭별 세부 융합 가이드라인 (Tab Specific Adaptation)

### 🤝 1. 봉사 & 기부 탭 (`VolunteerCatalog`)
- 1365 연동 상태 뱃지, CLM 전자서명 약정 신청 준비 모달 (`ClmApplicationPreparation`)과 연결.

### 💬 2. 픽셀 커뮤니티 탭 (`Pixel Community / PixelDiary`)
- 봉사/기부 탭과 100% 동일한 `.opportunity-table` 및 `.opportunity-detail` 스펙 채택.
- 48px 썸네일 이미지/이모지 뱃지, 작성자 픽셀 레벨 뱃지(`LV1_SEED` ~ `LV5_GUARDIAN`), 실시간 댓글 세션 확장 지원.

### 🤖 3. AI 픽셀 메이트 탭 (`Pixel AI Mate`)
- `ArteDante Dark Glow` 다크 메시 그래디언트 배경 (`#121218`), 유리질 투명 채팅 카드 (`backdrop-filter: blur(16px)`).
- Upstage Solar LLM 기반 추천 픽셀 카드는 봉사 카탈로그 스펙과 동기화.

### 🗺️ 4. 성장의 길 & 뱃지 도감 탭 (`Roadmap Tab`)
- `GlowGarden Trophy Grid`: LV1~LV5 레벨별 픽셀 뱃지 카드 및 온기 온도계 UI.

---

## 🎨 3. 마스터 디자인 토큰 (Design Tokens)

```css
:root {
  /* Common Master Palette */
  --pc-dark: #111111;
  --pc-white: #ffffff;
  --pc-muted: #666666;
  --pc-border: #e2e8f0;
  --pixel-primary: #ff3b30;
  --magazine-accent: #ff3b30;

  /* Volunteer & Community Standard Tokens */
  --tab-community-bg: #ffffff;
  --tab-community-border: #111111;
  --tab-community-pill-active: #111111;
  --tab-bento-form-bg: #faf0ca;

  /* AI Mate Tab Tokens */
  --tab-ai-bg: #121218;
  --tab-ai-glass-card: rgba(255, 255, 255, 0.06);
  --tab-ai-glass-border: rgba(255, 255, 255, 0.12);
}
```
