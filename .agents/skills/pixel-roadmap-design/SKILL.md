---
name: pixel-roadmap-design
description: Editorial Art Magazine & Modern Minimalist Grid UI Design System for Pixel Care platform. Defines high-impact editorial typography, 3-column grid layouts, thin architectural borders, pill category filters, and clean card components based on modern magazine aesthetics.
---

# Editorial Art Magazine UI Design System (에디토리얼 매거진 디자인 시스템)

이 디자인 스킬은 감각적인 **에디토리얼 아키텍처 매거진 스타일(Editorial Art Magazine Aesthetic)**의 모던 미니멀 UI 디자인 토큰, 컴포넌트 명세, 그리고 CSS/JS 구현 가이드를 정의합니다.

---

## 1. Visual Theme & Aesthetic Principles (디자인 철학)

1. **High-Impact Editorial Typography (압도적인 메인 타이포그래피)**
   - 페이지 상단에 거대하고 대담한 센스리프/에디토리얼 대형 타이틀(**PIXEL CARE MAGAZINE**) 배치.
   - 높은 대비와 명확한 시각적 위계 질서 형성.

2. **Clean 3-Column Architectural Grid (3열 모던 아키텍처 그리드)**
   - 얇고 정교한 검은색 외곽선(`1px solid #111111` 또는 `#e5e5e5`)으로 분할된 3열 카드 그리드.
   - 여백(Whitespace)과 미니멀한 여백 레이아웃 강조.

3. **Pill Category Filters (타원형 카테고리 필터 알약)**
   - 상단 카테고리 필터: `ALL`, `VOLUNTEER`, `DONATION`, `COMMUNITY`, `AI MATE`, `ROADMAP`.
   - 활성 상태: 단단한 솔리드 블랙 배경 (`#111111`) + 흰색 텍스트.
   - 비활성 상태: 투명/라이트 베이지 배경 + 얇은 픽셀 보더 외곽선.

4. **Magazine-Style Content Cards (매거진 카드 스펙)**
   - 상단 좌측: 작성일/등록일 (`2026.07.28`).
   - 상단 우측: 둥근 타원형 카테고리 뱃지 태그 (`VOLUNTEER`, `DONATION`, `COMMUNITY`).
   - 중앙: 썸네일 이미지/일러스트 뷰.
   - 타이틀: 검은색 Bold 제목.
   - 설명: 명확하고 읽기 쉬운 요약 텍스트.
   - 하단 버튼: `READ MORE →`, `신청하기 →`, `자세히 보기 →`.

---

## 2. Color Palette & Tokens (디자인 토큰)

| 토큰명 | 색상 코드 | 설명 & 용도 |
| :--- | :--- | :--- |
| `--magazine-bg` | `#FFFFFF` | 깨끗하고 깔끔한 순백색 메인 배경 |
| `--magazine-text` | `#111111` | 고대비 메인 텍스트 및 헤더 |
| `--magazine-muted` | `#666666` | 날짜, 작성자, 보조 요약 텍스트 |
| `--magazine-border` | `#111111` | 모던 얇은 아키텍처 테두리 선 (1px) |
| `--magazine-border-light` | `#E5E5E5` | 카드 분할선 및 내비게이션 라인 |
| `--magazine-accent` | `#FF3B30` | 픽셀 온기 및 하트 포인트 컬러 |
| `--magazine-pill-active` | `#111111` | 활성화된 카테고리 필터 알약 |
| `--magazine-pill-text` | `#FFFFFF` | 활성화 필터 알약 텍스트 |

---

## 3. Key CSS Implementation (주요 CSS 패턴)

```css
/* Editorial Magazine Title Header */
.magazine-header-title {
  font-size: 64px;
  font-weight: 900;
  letter-spacing: -2px;
  text-align: center;
  text-transform: uppercase;
  margin: 20px 0 30px;
  color: var(--magazine-text);
  border-bottom: 2px solid var(--magazine-text);
  padding-bottom: 20px;
}

/* 3-Column Magazine Grid */
.magazine-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  border-top: 1px solid var(--magazine-border);
  padding-top: 24px;
}

/* Magazine Card Item */
.magazine-card {
  border: 1px solid var(--magazine-border);
  padding: 20px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.magazine-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
```
