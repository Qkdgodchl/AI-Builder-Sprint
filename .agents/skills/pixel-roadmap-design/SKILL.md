---
name: pixel-roadmap-design
description: Master Synthesized Multi-Aesthetic Design System for Pixel Care platform. Combines Editorial Art Magazine, Terrava Green Organic Bento Grid, ArteDante Dark Glow Mesh, and GlowGarden Glassmorphism custom-tailored for each tab (Volunteer, Community, AI Mate, Roadmap).
---

# Master Synthesized UI Design System (픽셀 케어 탭별 맞춤 융합 디자인 시스템)

이 디자인 스킬은 현대 웹 디자인의 4대 트렌드(**에디토리얼 매거진, 내추럴 그린 벤토 그리드, 다크 아비에이트 엠비언트 글로우, 글로스모피즘**)를 융합하여 픽셀 케어의 탭별 핵심 기능에 맞게 통합한 마스터 디자인 시스템입니다.

---

## 🎨 탭별 1:1 맞춤 디자인 컨셉 (Tab-Specific Aesthetic Guidelines)

### 1. 🤝 봉사 & 기부 탭 (`Volunteer & Donation`)
- **디자인 모티브**: `Terrava Green Infrastructure` & `Nucleate Minimal` (자연친화적 산뜻한 에코 벤토)
- **비주얼 스펙**:
  - 배경: 상쾌한 클린 그린 에코 노드 (`#f4f8f3` / `#ffffff`)
  - 카드: 둥근 모서리(`border-radius: 12px`), 연한 잎새 그린 테두리, 목표 달성률 프로그레스 바.
  - 알약 태그: 1365 연동 그린 뱃지 (`#2d6a4f` 배경 + 흰색 텍스트).

### 2. 💬 픽셀 커뮤니티 탭 (`Pixel Community`)
- **디자인 모티브**: `Editorial Art Magazine` & `GlowGarden Bento Grid` (에디토리얼 매거진 + 벤토 그리드)
- **비주얼 스펙**:
  - 대형 에디토리얼 타이포그래피, 정교한 1px 테두리 서체 분할.
  - 상단 타원형 알약 필터 (`ALL`, `REVIEW`, `RECRUIT`, `GENERAL`).
  - 인기글 TOP 3 황금 픽셀 트로피 배너 및 댓글 오버레이 소통 영역.

### 3. 🤖 AI 픽셀 메이트 탭 (`Pixel AI Mate`)
- **디자인 모티브**: `ArteDante Dark Glow` & `Glassmorphism Mesh` (다크 아비에이트 엠비언트 글로우)
- **비주얼 스펙**:
  - 배경: 고풍스러운 다크 모드 (`#121218` 배경 + 주황/보라 태양광 메시 그래디언트 글로우 `radial-gradient`).
  - 채팅창: 유선형 프론트엔드 프롬프트 버블, 유리질 투명 엠보싱 (`backdrop-filter: blur(16px)`).
  - 응답 카드: 픽셀 마스코트 Solar LLM 추천 카드.

### 4. 🗺️ 성장의 길 & 뱃지 도감 탭 (`Roadmap Tab`)
- **디자인 모티브**: `GlowGarden Trophy Grid` (트로피 벤토 그리드 + 네온 해금)
- **비주얼 스펙**:
  - LV1~LV5 레벨별 트로피 카드 그리드.
  - 해금 완료 시 골드 엠비언트 광원 효과, 미해금 시 반투명 다크 락(Lock) 처리.

---

## 🎨 마스터 디자인 토큰 (Design Tokens)

```css
:root {
  /* Common Palette */
  --pc-dark: #111111;
  --pc-white: #ffffff;
  --pc-muted: #666666;
  --pc-border: #e2e8f0;

  /* Volunteer Tab (Terrava Green) */
  --tab-volunteer-bg: #f4f8f3;
  --tab-volunteer-card: #ffffff;
  --tab-volunteer-primary: #2d6a4f;
  --tab-volunteer-accent: #52b788;

  /* Community Tab (Editorial Art Magazine) */
  --tab-community-bg: #ffffff;
  --tab-community-border: #111111;
  --tab-community-pill-active: #111111;

  /* AI Mate Tab (ArteDante Dark Glow) */
  --tab-ai-bg: #121218;
  --tab-ai-glow-warm: radial-gradient(circle at 80% 20%, rgba(255, 120, 40, 0.25) 0%, transparent 60%);
  --tab-ai-glow-purple: radial-gradient(circle at 20% 80%, rgba(138, 43, 226, 0.25) 0%, transparent 60%);
  --tab-ai-glass-card: rgba(255, 255, 255, 0.06);
  --tab-ai-glass-border: rgba(255, 255, 255, 0.12);

  /* Roadmap Tab (GlowGarden Trophy Grid) */
  --tab-roadmap-bg: #fafafa;
  --tab-roadmap-gold: #ffb703;
}
```
