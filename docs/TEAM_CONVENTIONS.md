# 🤝 Pixel Care CLM 팀 기능별 협업 가이드라인 (TEAM_CONVENTIONS.md)

이 가이드라인은 팀원 3명이 **CLM 선행 약정 플랫폼의 핵심 기능별(Vertical Slice / Feature-Driven)**로 구역을 나누어 풀스택 개발할 때 준수해야 하는 규약입니다.

---

## 👥 1. 기능별 담당 분담표 (Feature Ownership)

| 팀원 | 담당 모듈 | 개발 영역 (FE UI + BE Domain + DB) |
| :--- | :--- | :--- |
| **팀원 A (리더)** | **📜 CLM 약정 & 모두싸인 전자서명 & 내 기록** | - BE: `com.pixelcare.commitment` (Commitment, SignatureRequest, Webhook)<br>- FE: `components/myrecords`, `components/commitment`, `commitmentApi.ts` |
| **팀원 B** | **🎁 5대 선행 카탈로그 & 🏢 단체 대시보드** | - BE: `com.pixelcare.opportunity` (1365 연동, Opportunity, Organization)<br>- FE: `components/catalog`, `components/admin`, `opportunityApi.ts` |
| **팀원 C** | **🤖 Upstage AI (Solar LLM + Extract) & 💬 커뮤니티** | - BE: `com.pixelcare.ai`, `com.pixelcare.community` (AI Chat, Post, Certificate)<br>- FE: `components/home`, `components/community`, `aiApi.ts` |

---

## 🌿 2. Git 브랜치 전략 (Feature Branch Workflow)

- **`main`**: 배포용 안정 브랜치 (직접 Push 금지, PR 필수)
- **`feature/clm-signature`**: CLM 약정서 생성 & 모두싸인 연동 & 내 기록 (팀원 A)
- **`feature/catalog-admin`**: 5대 선행 카탈로그 & 단체 대시보드 (팀원 B)
- **`feature/ai-community`**: Upstage Solar AI 대화 & 인증 커뮤니티 (팀원 C)

---

## 📝 3. Commit 메시지 컨벤션

```
feat(clm): 모두싸인 전자서명 요청 API 및 Webhook 구현
feat(catalog): 5대 선행 신청 유형(고향사랑기부/문화유산/유산기부) 카탈로그 연동
feat(ai): Solar LLM 선행 의향 JSON 구조화 프롬프트 구현
fix(clm): 약정 상태 변경(SIGN_REQUESTED -> SIGNED) Webhook 오류 수정
docs: CLM 기획서 PLAN.md 및 API_SPEC.md 업데이트
```

---

## 🔒 4. Flyway 버저닝 & 충돌 방지 규칙

1. **Flyway DB 마이그레이션 파일 버저닝**:
   - 팀원 A (CLM 약정): `V2__add_clm_commitments.sql`
   - 팀원 B (카탈로그/단체): `V3__add_opportunities.sql`
   - 팀원 C (AI/커뮤니티): `V4__add_ai_and_community.sql`

2. **공통 파일 수정을 원할 경우 사전 커뮤니케이션 필수** (`App.tsx`, `application.yml`, `build.gradle`).
