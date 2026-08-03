# 📜 잇다 ITDA 서비스 기획 & 제출물 명세서 (PLAN.md)

> **대회명**: AI Builder Sprint 2026 (부산대학교 APPTIVE 주최 / Upstage 후원)  
> **프로젝트명**: 잇다 ITDA  
> **핵심 주제**: Upstage Solar LLM AI & CLM 전자서명 기반 AI 기부·봉사 커뮤니티 플랫폼

---

## 🎯 1. 기획 배경 및 문제 정의 (Background & Problem Definition)

### 👥 타겟 사용자 (Target Users)
- **일반 사용자 (`USER`)**: 나에게 꼭 맞는 봉사/기부 프로그램을 찾고, 선행 아이디어를 직접 역제안하고 싶은 봉사자 및 후원자.
- **센터 관리자 (`CENTER_MANAGER`)**: 봉사/기부 프로그램을 개설하고 신청자 관리, 출석 승인 및 모집 현황을 관리하는 복지/봉사 기관 담당자.
- **운영진 (`OPERATOR`)**: 센터 및 관리자 승인 검토, 허위/불법 게시물 소프트 삭제, 플랫폼 전체 감사 로그를 관리하는 플랫폼 운영 주체.

### 🚨 문제 정의 (Problem Statement)
1. **파편화된 공고 & 탐색 피로도**: 수많은 복지 사이트에 흩어진 봉사 공고 속에서 나와 맞는 활동을 찾기 어려움.
2. **수동적 공급자 중심 구조**: 봉사자가 원하는 선행 아이디어를 직접 역제안할 창구가 부족함.
3. **복잡한 약정 & 이탈**: 정기후원이나 장기 봉사 약정 체결 시 복잡한 서류 작업과 갱신 절차로 이탈률이 높음.
4. **신뢰성 & 무결성 부족**: 허위 모집글 검증 체계 부족 및 작성된 약정서의 원본 무결성 증명 불가.

---

## 🤖 2. AI 활용 증빙 및 구현 명세 (AI Implementation Proof)

1. **사용 AI 모델**: Upstage Solar LLM (`solar-1-mini-chat`)
2. **API 사용 위치**:
   - `UpstageApiClient.java`: Upstage Solar LLM HTTP 통신
   - `AiMateService.java`: 자연어 대화 및 선행 큐레이팅
   - `AiConsultationService.java`: 대화문에서 지역, 시간, 금액, 감정을 고정 JSON 스키마로 추출
3. **프롬프트 페르소나**: `Pixel AI Mate` 친근한 마스코트 페르소나 및 JSON extraction 설정
4. **Smart Failover Engine**: API 장애 시 외부 통신 없이 100% 가동되는 내장 로컬 폴백 엔진

---

## 📱 3. 역할별 가변 하단 탭 구조 & 권한 체계

```text
1. 일반 사용자 (USER) - 4개 탭 + CONNECT
   ├── 🏠 홈 (Upstage AI 픽셀 큐레이터)
   ├── 🎁 선행하기 (봉사/기부 카탈로그 & 신청)
   ├── 💬 커뮤니티 (선행 인증 후기 & 소통)
   └── 📜 내 기록 (내 봉사/기부 이력, 온기 온도계 & 뱃지 도감)

2. 센터 관리자 (CENTER_MANAGER) - 5개 탭
   ├── 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록
   └── 🏢 센터 관리 (대시보드, 모집글 작성, 신청자 승인/출석 처리)

3. 운영진 (OPERATOR) - 5개 탭
   ├── 🏠 홈 | 🎁 선행하기 | 💬 커뮤니티 | 📜 내 기록
   └── 🛡️ 운영 관리 (권한 승인/거절, 센터 승인/거절, 소프트 삭제, 감사 로그)
```

---

## 🔄 4. 핵심 승인 및 워크플로우

1. **센터 관리자 권한 신청**: `USER`가 신청 ➔ `OPERATOR` 승인 ➔ `CENTER_MANAGER` 롤 변경 및 5탭 라우팅 전환.
2. **센터 등록 신청**: `CENTER_MANAGER`가 센터 신청 ➔ `OPERATOR` 승인 시 `APPROVED`.
3. **모집글 게시 & 실시간 노출**: 승인된 센터가 모집글(`Opportunity`) 작성 및 공개 ➔ 유저 `선행하기` 탭에 실시간 반영.
4. **CONNECT (선행 역제안)**: 유저가 선행 아이디어 작성 ➔ 이웃 응원(`Support`) ➔ 센터 수락(`Claim`) ➔ 정식 공고 개설.

---

## 🏆 5. 서비스 강점 및 기대 효과 (Strengths & Expected Impact)

- **창의성**: VolunteerCatalog Editorial Bento Grid UI, 실시간 온기 온도계 UI(36.5°C ➔ 상승) 및 `warmth_events` 기록.
- **AI 활용도**: Upstage Solar LLM 파이프라인, JSON 의향 파싱, 외부 전송 동의(Opt-in) 보관, Smart Failover Engine.
- **완성도**: 3단계 계정 역할 분리 및 역할별 가변 탭 라우팅 (4대 탭 / 5대 탭).
- **실용성**: CONNECT 선행 역제안, CLM 전자서명 갱신, SHA-256 PDF 감사추적, 소프트 삭제 & 감사 로그.
- **배포 및 운영**: Render 클라우드 프로덕션 배포 및 Spring Boot Native SQL Initialization (`schema-postgresql.sql`).
