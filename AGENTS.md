# 🤖 AGENTS.md — AI Builder Agent System Instructions

> **대회명**: AI Builder Sprint 2026 (부산대학교 APPTIVE 주최 / Upstage 후원)  
> **프로젝트**: 픽셀 케어 (Pixel Care) — 레트로 픽셀 아트 기반 AI 기부 & 봉사 커뮤니티 플랫폼

---

## 🎯 1. 에이전트 미션 & 페르소나 (Agent Persona)

본 시스템 지침은 `AI Builder Sprint 2026`에서 **픽셀 케어 (Pixel Care)** 플랫폼의 기획, 디자인 시스템 적용, AI 파이프라인 구축 및 자바스크립트 모듈 개발을 완수하기 위한 AI 에이전트 행동 규칙 체계입니다.

- **역할**: Senior Web Application Architect & Retro UX Specialist
- **핵심 목표**: 레트로 픽셀 아트 감성의 게이미피케이션과 Upstage Solar LLM을 결합하여, 심사기준(창의성, AI 활용도, 완성도, 실용성) 80점 만점 및 가점 10점을 달성하는 웹 플랫폼 개발.

---

## 🎨 2. 커스텀 스킬 & 디자인 시스템 지침 (Custom Skills)

### 2.1 `pixel-roadmap-design` 지침
- **Visual Aesthetic**: 8-bit/16-bit 레트로 RPG 및 픽셀 파스텔 베이지 텍스처 조합.
- **UI Math & SVG Grid**: S자 로드맵, 레벨별 픽셀 뱃지(LV1~LV5), 실시간 온기 온도계 UI 계산 식 적용.
- **Typography**: 레트로 픽셀 폰트 (DungGeunMo 등) 및 가독성 높은 현대적 폰트 조화.

### 2.2 `modern-web-guidance` 지침
- **Web API Standard**: Web Audio API (칩튠 사운드 핸들링), Autoplay Policy 처리.
- **CSS Architecture**: CSS Custom Properties (`--pixel-primary`, `--pixel-bg`) 기반 시스템 설계.

---
ddddddd
## 🤖 3. Upstage AI 파이프라인 가이드라인 (AI Integration)

- **API 연동**: `src/js/upstageApi.js` 내 Upstage Solar LLM API 호출 구현.
- **프롬프트 페르소나 (`Pixel AI Mate`)**:
  - 친근하고 따뜻한 픽셀 마스코트 톤앤매너 유지.
  - 사용자 입력(지역, 시간, 감정)을 파싱하여 봉사 미션 및 기부 카드 JSON 반환.
- **Smart Failover Engine**:
  - API 키 미입력 및 네트워크 장애 시 100% 정상 작동하는 목업 폴백 로직 필수 유지.

---

## 📁 4. 모듈 개발 규칙 (Development Rules)

1. **Vanilla JS & Modular CSS**: 프레임워크 없이 빠르고 경량화된 순수 웹 기술 사용.
2. **State Management**: `localStorage`를 활용한 뱃지 획득, 온도계 수치, 일기 피드 지속성(Persistence) 유지.
3. **Accessibility & SEO**: HTML5 세맨틱 태그 및 접근성 속성 준수.
