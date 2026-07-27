---
name: pixel-roadmap-design
description: Retro 8-bit/16-bit pixel-art style interactive roadmap UI design system. Defines aesthetics, color tokens, layout math, milestone cards, player avatar sprites, retro typography, and CSS/SVG implementation rules for gameified journey maps. Use when creating retro progress tracking UIs, level maps, or pixel art roadmaps.
---

# Retro Pixel-Art Roadmap Design System (레트로 픽셀 아트 로드맵 디자인 시스템)

이 디자인 스킬은 레트로 RPG/아케이드 게임 스타일의 **곡선형 픽셀 아트 로드맵(Winding Pixel Roadmap)** 및 진행 상황 트래커 UI를 구축하기 위한 완전한 디자인 토큰, 컴포넌트 명세, 그리고 CSS/JS 구현 가이드를 제공합니다.

---

## 1. Visual Theme & Aesthetic Principles (디자인 철학)

1. **Pixel-Perfect Crispness (선명한 픽셀 렌더링)**
   - 모든 그래픽 요소, 폰트, 보더는 픽셀 그리드 단위(Crisp pixels)로 결착되어야 합니다.
   - CSS 이미지 렌더링 옵션을 필수로 적용합니다:
     ```css
     image-rendering: pixelated;
     image-rendering: crisp-edges;
     font-smooth: never;
     -webkit-font-smoothing: none;
     ```

2. **Vibrant Retro Color Palette (선명한 레트로 도트 컬러)**
   - 80~90년대 8-bit/16-bit 게임 콘솔(GBA, SNES) 특유의 아기자기하고 명확한 원색 계열 활용.
   - 과도한 현대적 소프트 그래디언트 대신, 명확한 픽셀 명암 하이라이트/셰이딩 라인을 사용합니다.

3. **Winding Journey Path (구불구불한 아스팔트 도로)**
   - 직선 리스트 형태가 아닌, 화면 상단으로 향하는 S자 곡선 형태의 픽셀 도로(Asphalt Road) 레이아웃.
   - 중앙의 흰색 점선 도로 중앙선(Dashed Divider Line)과 진한 아스팔트 테두리 선.

4. **Gameified Interactivity (게임화된 상호작용)**
   - 도로 위를 이동하는 플레이어 캐릭터 아바타(Player Sprite).
   - 각 스테이지/단계 지점을 표시하는 붉은색 픽셀 위치 핀(Location Pin).
   - 단계 설명을 담는 레트로 베이지 픽셀 노드 카드(Milestone Card).
   - 시작점의 "START" 픽셀 버튼 & 최종 목적지의 "골드 트로피(Trophy)".

---

## 2. Color Palette & Tokens (디자인 토큰)

| 토큰명 | 색상 코드 | 설명 & 용도 |
| :--- | :--- | :--- |
| `--pixel-bg-grass` | `#55C54F` | 기본 잔디 필드 배경색 |
| `--pixel-bg-grass-dark` | `#47B542` | 잔디 픽셀 격자 및 텍스처 명암 |
| `--pixel-road-main` | `#383944` | 아스팔트 도로 메인 컬러 |
| `--pixel-road-border` | `#23242B` | 도로 가장자리 외곽선 |
| `--pixel-road-line` | `#E4E2D4` | 도로 중앙 점선 |
| `--pixel-pin-red` | `#E52521` | 위치 핀 메인 레드 |
| `--pixel-pin-dark` | `#9B0F0C` | 위치 핀 명암 그림자 |
| `--pixel-card-bg` | `#E4E2D4` | 마일스톤 카드 라이트 베이지 배경 |
| `--pixel-card-border` | `#9E9C8F` | 카드 픽셀 외곽 보더 |
| `--pixel-brick-red` | `#C84C31` | 하단 베이스라인 레트로 붉은 벽돌 |
| `--pixel-brick-mortar` | `#8B2C19` | 벽돌 사이 붉은 줄눈 |
| `--pixel-trophy-gold` | `#F4C542` | 트로피 골드 포인트 |
| `--pixel-start-btn` | `#3DA047` | START 버튼 바디 그린 |
| `--pixel-start-border` | `#6BE676` | START 버튼 픽셀 네온 테두리 |

---

## 3. Typography & Titles (타이포그래피 및 헤더)

### 폰트 설정
- **추천 레트로 픽셀 폰트**: `'Press Start 2P'`, `'Silkscreen'`, `'VT323'`, `'DungGeunMo'` (한글 지원)
- **CSS 폰트 로드 예시**:
  ```css
  @import url('https://fonts.googleapis.com/css2?family=Press+Start+2P&family=Silkscreen:wght@400;700&display=swap');
  
  .pixel-text {
    font-family: 'Press Start 2P', 'DungGeunMo', monospace;
    font-smooth: never;
    -webkit-font-smoothing: none;
  }
  ```

### 헤더 타이틀 ("ROAD MAP")
- 각 글자마다 도트 그래픽 느낌을 주는 다채로운 다색 픽셀 컬러(Red, Orange, Yellow, Green, Blue) 조합.
- 픽셀 아이콘 요소(지도 지도 스크롤, 버섯 decor)가 글자 위에 배치됨.
- 검은색 2px/4px 픽셀 텍스트 섀도우 적용:
  ```css
  .pixel-title {
    font-size: 2.5rem;
    letter-spacing: 4px;
    text-shadow: 
      3px 3px 0px #000,
      -1px -1px 0px #000,
      1px -1px 0px #000,
      -1px 1px 0px #000;
  }
  ```

---

## 4. Key Component Specifications (핵심 컴포넌트 명세)

### (1) Background & Ground Grid (잔디 필드 및 하단 벽돌)
- **상단 메인 영역**: 도트 패턴 잔디 타일 배경 (`#55C54F` 배경에 `#47B542` 2x2px 점 무작위 배치).
- **하단 베이스라인**: 픽셀 붉은 벽돌 패턴 타일 바 (`#C84C31` 벽돌 + `#8B2C19` 줄눈)로 맵의 시각적 하중(Foundation) 완충.

### (2) Winding Asphalt Road (곡선 도로)
- **구현 방식**: SVG Path 또는 픽셀 타일링 Canvas / CSS Clip-path.
- **도로 속성**:
  - 두께: 약 60px ~ 80px
  - 굴곡: 하단 좌측 START 버튼 근처에서 시작하여 우상단 Trophy 지점까지 3~4번 S자로 꺾이는 구불구불한 형태.
  - 중앙선: 8px x 4px 규격의 픽셀 대시(Dashed Line)가 일정 간격으로 도로 중앙을 따라 정렬.

### (3) Milestone Nodes & Red Location Pins (마일스톤 핀 & 카드)
- **Location Pin (위치 핀)**:
  - 픽셀 도트로 그려진 붉은색 물방울 아이콘.
  - 검은색 픽셀 외곽선과 내부 하이라이트 포함.
  - 현재 진행 중인 단계의 핀은 픽셀 위아래 보빙(Hovering Animation) 효과:
    ```css
    @keyframes pixel-bounce {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-6px); }
    }
    ```
- **Milestone Card (정보 상자)**:
  - 핀 바로 옆/아래에 위치한 모서리가 살짝 둥근 픽셀 프레임 (`#E4E2D4`).
  - 크기: 텍스트 입력이 가능한 빈 영역(Grey placeholder box), 활성화 시 각 단계의 제목, 미션, 진행도 표시.
  - 박스 그림자: Solid 픽셀 섀도우 (`box-shadow: 4px 4px 0px #23242B`).

### (4) Player Character Sprite (캐릭터 아바타)
- 도로 위 현재 진행 중인 단계 노드 옆에 위치.
- 16-bit RPG 주인공 스타일(예: 금발/보라 옷 레트로 캐릭터).
- Idle 상태 시 2프레임 픽셀 숨쉬기 모션, 이동 시 도로의 Path를 따라 이동하는 애니메이션.

### (5) Start Button & Trophy Goal (시작 버튼 및 목표 트로피)
- **START 버튼**:
  - 도로 시작점(하단 좌측)에 위치.
  - 레트로 녹색 프레임 + 네온 픽셀 테두리 (`#6BE676`) + 흰색 픽셀 글자 "START".
  - 클릭 시 픽셀 눌림 효과 (`transform: translate(2px, 2px)` & `box-shadow` 축소).
- **Goal Trophy**:
  - 도로 종착점(상단 우측)에 위치한 황금 픽셀 트로피 아이콘.
  - 목표 달성 시 반짝이는 픽셀 스파클(Sparkle) 이펙트 발생.

### (6) Environmental Decor (환경 오브젝트)
- **자연 요소**: 픽셀 소나무(Pine trees), 해바라기(Sunflowers), 통나무(Logs), 빨간 독버섯(Mushrooms).
- **동물 노드**: 하단 벽돌 위 및 풀밭에 배치된 레트로 도트 동물(고양이, 너구리/라쿤 등).

---

## 5. UI Layout Example Architecture (HTML / CSS Structure)

```html
<div class="pixel-roadmap-container">
  <!-- Header Title -->
  <header class="pixel-header">
    <div class="map-icon-pixel"></div>
    <h1 class="pixel-title">
      <span class="red">R</span><span class="orange">O</span><span class="yellow">A</span><span class="green">D</span>
      <span class="blue">M</span><span class="light-green">A</span><span class="pink">P</span>
    </h1>
    <div class="trophy-pixel-header"></div>
  </header>

  <!-- Canvas or SVG Winding Road Background -->
  <div class="roadmap-viewport">
    <svg class="road-svg-layer" viewBox="0 0 400 700">
      <!-- Outer Road Border -->
      <path d="M 50 650 C 150 600, 350 550, 320 420 C 290 300, 50 350, 80 220 C 110 100, 300 120, 350 40" 
            stroke="#23242B" stroke-width="64" fill="none" stroke-linecap="round" />
      <!-- Road Surface -->
      <path d="M 50 650 C 150 600, 350 550, 320 420 C 290 300, 50 350, 80 220 C 110 100, 300 120, 350 40" 
            stroke="#383944" stroke-width="52" fill="none" stroke-linecap="round" />
      <!-- Dashed Center Line -->
      <path d="M 50 650 C 150 600, 350 550, 320 420 C 290 300, 50 350, 80 220 C 110 100, 300 120, 350 40" 
            stroke="#E4E2D4" stroke-width="4" stroke-dasharray="10 10" fill="none" />
    </svg>

    <!-- Milestone Nodes (Pinned along the path coordinates) -->
    <div class="milestone-node step-1" style="top: 80%; left: 50%;">
      <div class="pixel-pin active"></div>
      <div class="pixel-card">
        <div class="card-title">01. START</div>
      </div>
    </div>

    <!-- Player Character Avatar -->
    <div class="player-avatar-sprite" style="top: 82%; left: 35%;"></div>

    <!-- START Button -->
    <button class="pixel-btn-start">START</button>
  </div>

  <!-- Bottom Brick Baseline Decor -->
  <div class="pixel-brick-baseline"></div>
</div>
```

---

## 6. Implementation Checklist for Agents

웹 앱 또는 UI 구현 시 다음 사항을 검증하십시오:

- [ ] `image-rendering: pixelated` 속성이 이미지와 캔버스, 아이콘에 전역 적용되어 블러 현상이 없는가?
- [ ] 픽셀 폰트(Press Start 2P, Silkscreen, DungGeunMo 등)가 정상 적용되었는가?
- [ ] 아스팔트 도로의 굴곡과 마일스톤 위치 핀(Pin), 카드(Card)의 위치가 시각적으로 자연스럽게 정렬되었는가?
- [ ] 현재 활성화된 핀에 픽셀 바운스(Bounce) 애니메이션이 적용되어 있는가?
- [ ] 시작점(START 버튼), 과정(위치 핀 & 카드), 도착점(골드 트로피)의 3단계 흐름이 명확히 파악되는가?
- [ ] 하단 붉은 벽돌과 풀밭 위 소소한 픽셀 에셋(나무, 꽃, 동물)이 배치되어 완성도 높은 8-bit 게임 스크린 느낌을 전달하는가?
