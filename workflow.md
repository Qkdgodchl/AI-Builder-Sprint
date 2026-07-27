# 픽셀 케어 (Pixel Care) - 전체 서비스 확정 기획서 (workflow.md)

> **대회명**: AI Builder Sprint 2026 (부산대학교 APPTIVE 주최 / Upstage 후원)  
> **주제**: AI를 통해 인간다움을 더욱 잘 드러낼 수 있는 서비스 개발  
> **서비스명**: **픽셀 케어 (Pixel Care)** — 레트로 픽셀 아트 기반 AI 기부 & 봉사 커뮤니티 플랫폼  
> **슬로건**: *"하나하나의 픽셀이 모여 만드는 세상에서 가장 따뜻한 공간"*

---

## 1. 확정 서비스 비전 & 전체 구조 (Confirmed Architecture)

### 🎨 1.1 비주얼 테마 & 세계관 (Confirmed: Option C 융합 테마)
- **8-bit/16-bit 레트로 RPG 퀘스트 감성**: 전체 메인 프레임, 픽셀 뱃지, 레트로 칩튠 사운드 FX.
- **아기자기한 픽셀 일기장 감성**: 커뮤니티 탭은 따뜻한 파스텔 베이지 픽셀 일기장(Diary) 인터페이스.

### 🖼️ 1.2 메인 홈 화면 & 상단 탭 구조 (Confirmed: 사용자 맞춤 센터 레이아웃)
- **메인 홈 중앙 (Centerpiece)**: 커다랗고 귀여운 **🤖 픽셀 AI 대화창 (Pixel AI Mate)** 배치 (Upstage Solar LLM 기반 대화형 추천).
- **상단 메인 네비게이션 탭**:
  1. `[🤝 봉사 & 기부]` (하이브리드 1초 간편 신청 + 1365 공공데이터 출처 태그)
  2. `[📖 픽셀 일기장 & 커뮤니티]` (봉사 일기 작성, 날씨/감정 스티커, 응원 하트)
  3. `[🗺️ 성장의 길 (로드맵)]` (S자 곡선 도로 & 픽셀 뱃지 도감)

### 🤖 1.3 Upstage AI 연동 파이프라인 (Confirmed: Option C)
- **Upstage Solar LLM 단일 집중 챗봇**: 메인 홈 중앙 AI 대화창에서 사용자 질문/대화로 개인 맞춤형 봉사 미션 및 기부 펀딩 추천 카드 생성.
- **스마트 AI 응답 폴백(Failover Mock Engine)**: API 키 미입력 시에도 완벽한 시연이 가능하도록 고도화된 목업 응답 지원.

### 🏆 1.4 게이미피케이션 & 보상 시스템 (Confirmed: Option A)
- **픽셀 뱃지 도감 (LV1~LV5)**: 봉사/기부 참가 시 레벨업 픽셀 뱃지 획득.
- **우리 동네 '픽셀 온기 온도계'**: 참가자 합산에 따라 **36.5°C ➔ 99.9°C**로 상승하는 픽셀 온도계 인터랙션.
- **실시간 통계 바**: 총 누적 기부금(₩), 총 봉사시간(시간), 참여 영웅 수(명).

---

## 2. 🧩 개발 모듈 단위 태스크 (Feature Modules)

```
[Module 1: 🤖 메인 AI 대화창] ➔ [Module 2: 🤝 봉사 & 기부 (하이브리드)] ➔ [Module 3: 📖 픽셀 일기장] ➔ [Module 4: 🏆 뱃지 & 온도계] ➔ [Module 5: 🏁 최종 통합]
```

### Module 1. 🤖 메인 AI 대화창 (Pixel AI Mate Chatbot Module)
- 메인 홈 중앙에 귀여운 픽셀 AI 챗봇 대화창 UI 구현 (말풍선, AI 프로필 모션, 프롬프트 추천 칩).
- Upstage Solar LLM 연동 (`src/js/upstageApi.js`) 및 대화 응답 내 [바로 참가하기] 카드 출력.

### Module 2. 🤝 봉사 & 기부 탭 (Volunteer & Donation Module - 하이브리드)
- 봉사 모집 및 기부 펀딩 카탈로그 UI (1365 연동 기관 태그 표시).
- ⚡ 1초 픽셀 간편 봉사/기부 신청 모달 (서비스 내 즉시 참가 확정 & 1365 원문보러가기 ↗ 아웃링크).

### Module 3. 📖 픽셀 일기장 & 커뮤니티 (Pixel Diary Module)
- 레트로 픽셀 일기장 피드 UI (감정/날씨 스티커 😊🥰🌱🔥, 날짜, 픽셀 사진 첨부).
- 응원 픽셀 하트(❤️) 보내기 인터랙션 & `localStorage` 저장.

### Module 4. 🏆 뱃지 도감 & 온기 온도계 (Badge & Thermometer Module)
- 프로필 픽셀 뱃지(LV1~LV5 레전드 트로피) 도감 UI.
- 실시간 픽셀 온기 온도계 게이지 모션 및 상단 누적 통계 바 연동.

### Module 5. 🏁 최종 통합 & 푸시 (Final Integration Module)
- Web Audio API 레트로 8-bit 사운드 (`src/js/soundFx.js`) 전 모듈 통합.
- 대회 제출 규칙 `AGENTS.md` 작성 및 GitHub `main` 최종 커밋/푸시.

---

*본 기획서는 사용자 공동 기획을 통해 100% 확정되었습니다.*
