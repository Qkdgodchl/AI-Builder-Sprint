# Pixel Care CLM REST API 명세서

> 기준 기획: `CLM 기반 선행 플랫폼 최종 서비스 기획`
>
> 문서 버전: `v1.1-mvp-implementation`
>
> Base URL: `/api/v1`
>
> 목적: 백엔드 구현 순서, 프론트엔드 계약, API별 성공·실패 테스트 현황을 한 문서에서 관리한다.

---

## 0. 문서 사용법

각 API는 다음 세 항목을 모두 완료해야 한다.

- `[ ] 구현`: Controller, Service, Repository와 권한·입력 검증 구현
- `[ ] 성공`: 대표 성공 케이스 자동 테스트 통과
- `[ ] 실패`: 입력 오류, 인증·권한, 상태 충돌, 미존재 리소스 테스트 통과

체크 예시:

```markdown
| [x] | [x] | [x] | POST | `/auth/signup` | 회원가입 |
```

완료 처리 원칙:

1. 단순히 HTTP 응답이 오는 것만으로 구현 완료 처리하지 않는다.
2. 성공·실패 테스트가 모두 통과한 뒤 세 칸을 `[x]`로 바꾼다.
3. API 구현 커밋에서 이 문서의 해당 체크박스도 함께 갱신한다.
4. 테스트하지 못한 외부 연동은 구현 완료로 표시하지 않는다.
5. 기존 레거시 `/api/volunteers`, `/api/posts`는 새 `/api/v1` 규격으로 이전한 뒤 체크한다.

### 공개 식별자 규칙

- 센터·모집글처럼 공개 목록에서 조회되는 자원은 정상 양수 `id`를 사용한다.
- 신청, 약정, 문서, 서명 요청은 추측이 어려운 UUID `publicId`를 사용한다.
- 음수 ID, 배열 인덱스, 프론트 임시 순번을 API 식별자로 사용하지 않는다.

---

## 1. 공통 규약

### 1.1 인증과 권한

- 인증 방식: `Authorization: Bearer <access-token>`
- 기본 역할:
  - `USER`: 일반 사용자
  - `CENTER_MANAGER`: 승인된 센터 관리자
  - `OPERATOR`: 플랫폼 운영진
- 한 사용자는 여러 역할을 가질 수 있다.
- 화면에서 버튼을 숨기는 것과 별개로 모든 API에서 역할과 리소스 소유권을 검사한다.
- `CENTER_MANAGER`는 승인된 본인 센터만 관리할 수 있다.
- `OPERATOR`만 관리자 승인, 센터 승인, 전체 게시물 삭제, 계정·센터 정지, 감사 로그 조회를 수행할 수 있다.

### 1.2 성공 응답

```json
{
  "success": true,
  "data": {},
  "message": "요청이 성공적으로 처리되었습니다."
}
```

삭제·로그아웃처럼 본문이 필요 없는 작업은 `204 No Content`를 반환한다.

### 1.3 실패 응답

```json
{
  "success": false,
  "message": "입력값을 확인해주세요."
}
```

### 1.4 HTTP 상태 코드

| 상태 | 사용 기준 |
|---|---|
| `200 OK` | 조회, 수정, 상태 변경 성공 |
| `201 Created` | 사용자, 신청, 센터, 모집글, 약정, 문서 등 생성 성공 |
| `202 Accepted` | AI·문서 생성처럼 비동기 작업 접수 |
| `204 No Content` | 로그아웃, 취소, 삭제 성공 |
| `400 Bad Request` | 형식 오류, 필수값 누락, 잘못된 상태 전이 |
| `401 Unauthorized` | 토큰 없음, 만료 또는 위조 |
| `403 Forbidden` | 역할 부족, 타 사용자·타 센터 리소스 접근 |
| `404 Not Found` | 리소스 없음 또는 소프트 삭제되어 비공개 |
| `409 Conflict` | 이메일 중복, 중복 신청, 중복 서명 요청, 상태 충돌 |
| `410 Gone` | 만료된 서명 링크나 더 이상 사용할 수 없는 요청 |
| `413 Payload Too Large` | 업로드 파일 용량 초과 |
| `415 Unsupported Media Type` | 허용되지 않은 파일 형식 |
| `422 Unprocessable Entity` | AI 구조화 실패, 문서 필드 검증 불일치 |
| `429 Too Many Requests` | AI·외부 API 호출 제한 |
| `502 Bad Gateway` | Upstage·모두싸인의 잘못된 응답 |
| `503 Service Unavailable` | 외부 서비스 또는 시스템 일시 장애 |

### 1.5 페이지네이션

목록 API 기본값:

```http
?page=0&size=20&sort=createdAt,desc
```

응답:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

`size`는 `1~100`만 허용한다.

### 1.6 날짜·금액·멱등성

- 날짜·시간: ISO 8601, 서버 저장은 UTC, 응답은 Offset 포함
- 금액: 원 단위 정수 `long`, 음수·소수 금지
- 생성·서명·Webhook처럼 중복 영향이 큰 요청은 멱등성을 보장한다.
- 서명 요청:

```http
Idempotency-Key: commitment-72-version-1
```

---

## 2. 공통 테스트 규칙

### 2.1 자동 테스트 도구

- Controller: Spring MockMvc
- Service: JUnit 5 + Mockito
- Repository: `@DataJpaTest`
- API 통합 테스트: `@SpringBootTest` + MockMvc
- 외부 API: WireMock 또는 MockWebServer
- 운영 DB 호환성 확인: Testcontainers MySQL
- 인증: 테스트용 opaque Access Token 발급 헬퍼

### 2.2 API를 하나 만들 때마다 실행할 테스트

모든 API에 최소한 다음을 적용한다.

1. 정상 요청의 응답 코드·본문·DB 변경 확인
2. 필수값 누락, 형식 오류, 경계값 확인
3. 비로그인 `401` 확인
4. 역할 부족 및 타인·타 센터 접근 `403` 확인
5. 없는 ID `404` 확인
6. 중복 요청 또는 상태 충돌 `409` 확인
7. 외부 API가 있으면 timeout, `4xx`, `5xx`, 잘못된 JSON 응답 확인
8. 실패 시 DB가 부분 저장되지 않는지 트랜잭션 확인

### 2.3 테스트 실행 명령

전체:

```bash
cd backend
./gradlew test
```

클래스 단위:

```bash
./gradlew test --tests "com.pixelcare.auth.AuthControllerTest"
```

특정 메서드:

```bash
./gradlew test --tests "com.pixelcare.auth.AuthControllerTest.signup_success"
```

수동 확인 예시:

```bash
curl -i -X GET \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  http://localhost:8080/api/v1/users/me
```

자동 테스트가 기준이며 `curl`은 보조 검증으로만 사용한다.

### 2.4 현재 구현 검증 기록

2026-07-30에 로컬 MySQL 8.4와 실제 백엔드를 연결해 다음 흐름을 검증했다.

| 구간 | 성공 케이스 | 실패 케이스 | DB 확인 대상 |
|---|---|---|---|
| 인증 | 회원가입 `201`, 로그인·내 정보 `200` | 무인증 내 정보 `401`, 잘못된 로그인 `401` | `users`, `user_roles`, `access_tokens`, `refresh_tokens` |
| 관리자 전환 | 신청 `201`, 운영진 승인 `200` | 중복 대기 신청 `409`, 일반 사용자 승인 `403` | `manager_applications`, `manager_application_files`, `organizations`, `organization_managers` |
| 모집글 | 초안 `201`, 공개·목록·상세 `200` | 잘못된 상태 재공개 `409`, 타 센터 접근 `403`, 없는 글 `404` | `opportunities`, `opportunity_required_documents` |
| 신청·CLM | 신청 `201`, 약정 제출 `200`, 센터 승인 `200` | 동일 사용자 중복 신청 `409` | `applications`, `commitments`, `commitment_versions`, `application_consents` |

Controller 자동 테스트는 `AuthControllerTest`, `OpportunityControllerTest`에 성공·입력 오류·
인증 실패·미존재 케이스를 두었고 `./gradlew test`로 실행한다. 체크표의 성공·실패 칸은
실제로 검증한 항목만 표시하며, 구현만 끝난 항목은 후속 테스트가 추가될 때 체크한다.

---

# Phase 1 — 핵심 MVP API

## 3. 인증·사용자·파일

### 3.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [x] | [x] | [x] | POST | `/auth/signup` | Public | 이메일 회원가입 |
| [x] | [x] | [x] | POST | `/auth/login` | Public | 로그인 |
| [x] | [x] | [ ] | POST | `/auth/refresh` | Refresh Token | Access Token 재발급 |
| [x] | [x] | [ ] | POST | `/auth/logout` | USER | 로그아웃·Refresh Token 폐기 |
| [x] | [x] | [x] | GET | `/users/me` | USER | 내 프로필·역할 조회 |
| [x] | [ ] | [ ] | PATCH | `/users/me` | USER | 프로필·관심 분야 수정 |
| [x] | [ ] | [ ] | POST | `/files` | USER | 증빙·이미지 파일 업로드 |

### 3.2 회원가입

`POST /api/v1/auth/signup`

```json
{
  "email": "user@example.com",
  "password": "StrongPassword1!",
  "name": "권윤재",
  "phone": "01012345678",
  "birthDate": "2000-01-01",
  "region": "BUSAN",
  "interests": ["ENVIRONMENT", "CULTURAL_HERITAGE"],
  "privacyConsent": true
}
```

성공 `201`:

```json
{
  "userId": 1,
  "email": "user@example.com",
  "roles": ["USER"]
}
```

테스트 방법:

- 성공: 유효한 값 → `201`, 비밀번호 hash 저장, `USER` 역할 부여
- 실패: 잘못된 이메일·약한 비밀번호·필수 동의 누락 → `400`
- 실패: 이미 존재하는 이메일 → `409`

### 3.3 로그인·토큰

테스트 방법:

- 로그인 성공 → `200`, Access/Refresh Token 발급
- 비밀번호 불일치·정지 계정 → `401` 또는 `403`
- Refresh 성공 → `200`, 폐기·만료·재사용 토큰 → `401`
- 로그아웃 성공 → `204`, 이후 Refresh Token 사용 → `401`

### 3.4 프로필·파일

테스트 방법:

- 내 프로필 조회·수정 성공 → `200`
- 이메일 등 수정 불가 필드 변조 → `400`
- 토큰 없음·만료 → `401`
- 파일 업로드 성공 → `201`, 허용 MIME·크기·소유자 저장
- 실행 파일·허용하지 않은 확장자 → `415`
- 용량 초과 → `413`

---

## 4. 센터 관리자 권한 신청

### 4.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [x] | [x] | [x] | POST | `/manager-applications` | USER | 센터 관리자 권한 신청 |
| [x] | [x] | [ ] | GET | `/manager-applications/me` | USER | 내 신청 목록·상태 |
| [x] | [x] | [x] | GET | `/manager-applications/{id}` | 신청자·OPERATOR | 신청 상세 |
| [x] | [ ] | [ ] | POST | `/manager-applications/{id}/cancel` | 신청자 | 대기 신청 취소 |
| [x] | [x] | [x] | GET | `/operator/manager-applications` | OPERATOR | 신청 목록·필터 |
| [x] | [x] | [x] | POST | `/operator/manager-applications/{id}/approve` | OPERATOR | 신청 승인 |
| [x] | [ ] | [ ] | POST | `/operator/manager-applications/{id}/reject` | OPERATOR | 신청 거절 |

신청 요청:

```json
{
  "organizationName": "부산희망봉사센터",
  "position": "봉사 담당자",
  "contact": "01012345678",
  "organizationType": "NON_PROFIT",
  "registrationNumber": "123-45-67890",
  "evidenceFileId": 901,
  "evidenceFileIds": [901, 902],
  "reason": "센터 봉사 모집과 신청자를 관리하기 위해 신청합니다.",
  "plannedCenterName": "부산희망봉사센터"
}
```

승인·거절:

```json
{
  "reason": "제출 서류와 재직 정보를 확인했습니다."
}
```

테스트 방법:

- 신청 성공 → `201`, 상태 `PENDING`
- 증빙 없음·필수값 누락 → `400`
- 진행 중 신청 중복 → `409`
- 신청자 본인 상세 조회 → `200`, 타 사용자 → `403`
- `PENDING` 취소 → `200`, 처리 완료 신청 취소 → `409`
- 운영진 승인 → `200`, 사용자 역할에 `CENTER_MANAGER` 추가
- 운영진 거절 → `200`, `REJECTED`와 사유 저장
- 일반 사용자의 승인·거절 시도 → `403`
- 같은 요청 이중 승인 → `409`

---

## 5. 센터 등록·관리

### 5.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [x] | [x] | [x] | POST | `/organization-applications` | CENTER_MANAGER | 센터 등록 신청 |
| [x] | [x] | [x] | GET | `/organization-applications/me` | CENTER_MANAGER | 내 센터 등록 요청 |
| [x] | [x] | [x] | GET | `/operator/organization-applications` | OPERATOR | 센터 요청 목록 |
| [x] | [x] | [x] | POST | `/operator/organization-applications/{id}/approve` | OPERATOR | 센터 승인 |
| [x] | [x] | [x] | POST | `/operator/organization-applications/{id}/reject` | OPERATOR | 센터 거절 |
| [ ] | [ ] | [ ] | POST | `/operator/organization-applications/{id}/request-revision` | OPERATOR | 보완 요청 |
| [x] | [x] | [x] | GET | `/organizations/{id}` | Public | 승인 센터 상세 |
| [x] | [x] | [x] | GET | `/manager/organizations` | CENTER_MANAGER | 내가 관리하는 센터 |
| [x] | [ ] | [ ] | PATCH | `/manager/organizations/{id}` | 해당 센터 관리자 | 센터 정보 수정 |
| [x] | [x] | [x] | GET | `/manager/organizations/{id}/dashboard` | 해당 센터 관리자 | 센터 대시보드 |

센터 등록 요청:

```json
{
  "name": "부산희망봉사센터",
  "organizationType": "NON_PROFIT",
  "registrationNumber": "123-45-67890",
  "representativeName": "권윤재",
  "phone": "051-123-4567",
  "email": "center@example.org",
  "address": "부산광역시 금정구 ...",
  "description": "지역 봉사 프로그램을 운영합니다.",
  "evidenceFileId": 902
}
```

테스트 방법:

- `CENTER_MANAGER` 신청 → `201`, 상태 `PENDING`
- 일반 사용자 신청 → `403`
- 등록번호 중복 → `409`
- 운영진 승인 → `200`, `Organization(APPROVED)`와 관리자 관계 생성
- 거절·보완 요청 → `200`, 사유 저장
- 승인되지 않은 센터 상세 → 일반 사용자 `404`
- 센터 수정 성공 → `200`
- 다른 센터 관리자가 ID를 바꿔 수정·대시보드 조회 → `403`
- `SUSPENDED`, `DELETED` 센터의 관리자 기능 → `403` 또는 `409`

첫 센터는 관리자 권한 승인 시 함께 생성한다. 추가 센터는
`/organization-applications`로 신청하며, 운영진 승인 시 `organizations`와
`organization_managers`가 한 트랜잭션으로 생성된다.

---

## 6. 선행 기회·모집글

`Opportunity.type`:

- `VOLUNTEER`
- `DONATION`
- `HOMETOWN_DONATION`
- `CULTURAL_HERITAGE_DONATION`
- `LEGACY_DONATION`

### 6.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [x] | [x] | [x] | GET | `/opportunities` | Public | 공개 모집글 검색·필터 |
| [x] | [x] | [x] | GET | `/opportunities/{opportunityId}` | Public | 공개 모집글 상세 |
| [x] | [x] | [x] | DELETE | `/opportunities/{opportunityId}` | 작성자 또는 OPERATOR | 일반 목록·상세 화면에서 모집글 소프트 삭제 |
| [x] | [x] | [x] | POST | `/manager/organizations/{organizationId}/opportunities` | 해당 센터 관리자 | 모집글 초안 작성 |
| [x] | [x] | [x] | GET | `/manager/organizations/{organizationId}/opportunities` | 해당 센터 관리자 | 센터 모집글 목록 |
| [x] | [x] | [x] | GET | `/manager/opportunities/{opportunityId}` | 해당 센터 관리자 | 비공개 포함 상세 |
| [x] | [ ] | [ ] | PATCH | `/manager/opportunities/{opportunityId}` | 해당 센터 관리자 | 초안·수정가능 모집글 수정 |
| [x] | [x] | [x] | POST | `/manager/opportunities/{opportunityId}/publish` | 해당 센터 관리자 | 모집글 공개 |
| [x] | [ ] | [ ] | POST | `/manager/opportunities/{opportunityId}/close` | 해당 센터 관리자 | 모집 마감 |
| [x] | [ ] | [ ] | POST | `/manager/opportunities/{opportunityId}/cancel` | 해당 센터 관리자 | 모집 취소 |
| [x] | [x] | [x] | GET | `/operator/opportunities` | OPERATOR | 전체 모집글 관리 목록 |
| [x] | [x] | [x] | GET | `/operator/opportunities/{opportunityId}` | OPERATOR | 모집글 관리 상세 |
| [x] | [x] | [x] | PATCH | `/operator/opportunities/{opportunityId}` | OPERATOR | 모집글 강제 수정 |
| [x] | [x] | [x] | DELETE | `/operator/opportunities/{opportunityId}` | OPERATOR | 모집글 소프트 삭제 |

삭제 권한 테스트:

- 성공: 모집글 작성자 토큰 또는 운영진 토큰으로 삭제하면 `200`
- 권한 실패: 작성자가 아닌 일반 사용자 토큰이면 `403`
- 인증 실패: 토큰이 없거나 유효하지 않으면 `401`
- 대상 없음: 존재하지 않거나 이미 삭제된 모집글이면 `404`

목록 필터:

```http
GET /api/v1/opportunities
  ?type=VOLUNTEER
  &category=ENVIRONMENT
  &region=BUSAN
  &participationMode=OFFLINE
  &status=PUBLISHED
  &keyword=해변
  &sort=recruitmentEndDate,asc
```

생성 요청:

```json
{
  "type": "VOLUNTEER",
  "category": "ENVIRONMENT",
  "title": "해운대 해변 환경정화 봉사",
  "description": "해변 플로깅 활동입니다.",
  "recruitmentCapacity": 30,
  "recruitmentStartDate": "2026-08-01",
  "recruitmentEndDate": "2026-08-08",
  "activityStartDateTime": "2026-08-10T09:00:00+09:00",
  "activityEndDateTime": "2026-08-10T12:00:00+09:00",
  "location": "부산 해운대구",
  "eligibility": "중학생 이상",
  "requiredDocumentTypes": [
    "VOLUNTEER_PLEDGE",
    "PRIVACY_CONSENT",
    "SAFETY_RULES"
  ],
  "cancellationPolicy": "활동 2일 전까지 취소 가능"
}
```

테스트 방법:

- 공개 목록은 `PUBLISHED`, `isDeleted=false`, 승인 센터 글만 반환 → `200`
- 필터·검색·정렬·페이지 경계값 확인 → `200` 또는 `400`
- 공개 상세 조회 성공 → `200`
- 초안·삭제·정지 센터 글을 일반 사용자가 조회 → `404`
- 승인 센터 관리자의 초안 작성 → `201`, 상태 `DRAFT`
- 비승인 센터·타 센터 관리자 작성 → `403`
- 날짜 역전, 정원 0, 필수 문서 유형 오류 → `400`
- 필수값 완성된 초안 공개 → `200`, 상태 `PUBLISHED`
- 필수값 누락·이미 삭제된 글 공개 → `409`
- 운영진 삭제 → `200`, `isDeleted`, 삭제자·시각·감사 로그 저장
- 일반 목록과 상세에서 삭제 즉시 제외 확인

---

## 7. AI 의향 상담·추천

### 7.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [x] | [ ] | [ ] | POST | `/ai/consultations` | USER | 상담 세션 시작·의향 구조화 |
| [x] | [ ] | [ ] | POST | `/ai/consultations/{id}/messages` | 상담 소유자 | 후속 메시지 전송·의향 재구조화 |
| [x] | [ ] | [ ] | GET | `/ai/consultations/{id}` | 상담 소유자 | 상담·구조화 결과 조회 |
| [x] | [ ] | [ ] | PATCH | `/ai/consultations/{id}/intent` | 상담 소유자 | 구조화 값 직접 수정 |
| [x] | [ ] | [ ] | POST | `/ai/consultations/{id}/confirm` | 상담 소유자 | 필수 필드 검증·의향 최종 확인 |
| [ ] | [ ] | [ ] | GET | `/ai/consultations/{id}/recommendations` | 상담 소유자 | 조건 기반 추천 |

메시지 요청:

```json
{
  "message": "부산에서 8월 10일 오전에 환경 봉사를 하고 싶어요."
}
```

응답:

```json
{
  "consultationId": 41,
  "assistantMessage": "이동 가능한 범위와 초상권 동의 여부를 알려주세요.",
  "intent": {
    "commitmentType": "VOLUNTEER",
    "region": "BUSAN",
    "availableDate": "2026-08-10",
    "availableTime": "MORNING",
    "portraitConsent": null
  },
  "missingFields": ["travelRadiusKm", "portraitConsent"],
  "readyToConfirm": false
}
```

테스트 방법:

- 상담 생성 → `201`
- 메시지 성공 → `200`, AI 출력 JSON Schema·enum·날짜 재검증
- 빈 메시지·최대 길이 초과 → `400`
- 타인 상담 접근 → `403`
- 없는 상담 → `404`
- 필수값 누락 상태에서 확인 → `400`
- 사용자 확인 성공 → `200`, `confirmedAt` 저장
- 확인 전 추천 요청 → `409`
- 공개·모집 중 기회만 추천되는지 확인 → `200`
- Upstage timeout·`5xx` → 재시도 정책 후 `503`
- 잘못된 JSON·허용되지 않은 금액·날짜 → `422`, 신청·약정 미생성
- 호출 제한 → `429`

---

## 8. 봉사·기부 신청

신청 상태:

```text
APPLIED → DOCUMENT_PENDING → SIGNATURE_PENDING → IN_REVIEW
→ REVISION_REQUESTED | APPROVED | REJECTED
→ ATTENDED | ABSENT → COMPLETED → VERIFIED
```

종료 상태: `CANCELLED`

### 8.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [x] | [x] | [x] | POST | `/opportunities/{opportunityId}/applications` | USER | 봉사·기부 신청 |
| [x] | [x] | [x] | GET | `/applications/me` | USER | 내 신청 목록 |
| [x] | [x] | [x] | GET | `/applications/{applicationPublicId}` | 신청자·해당 센터 관리자 | 신청 상세 |
| [x] | [ ] | [ ] | POST | `/applications/{applicationPublicId}/cancel` | 신청자 | 신청 취소 |
| [x] | [x] | [x] | GET | `/manager/opportunities/{opportunityId}/applications` | 해당 센터 관리자 | 모집글별 신청자 목록 |
| [x] | [x] | [x] | GET | `/manager/applications/{applicationPublicId}` | 해당 센터 관리자 | 신청자·문서 상세 |
| [x] | [ ] | [ ] | POST | `/manager/applications/{applicationPublicId}/request-revision` | 해당 센터 관리자 | 수정 요청 |
| [x] | [x] | [x] | POST | `/manager/applications/{applicationPublicId}/approve` | 해당 센터 관리자 | 신청 승인 |
| [x] | [ ] | [ ] | POST | `/manager/applications/{applicationPublicId}/reject` | 해당 센터 관리자 | 신청 거절 |

신청 요청:

```json
{
  "consultationId": 41,
  "participationDate": "2026-08-10",
  "specialConditions": "오전 시간 참여 가능",
  "consents": {
    "privacy": true,
    "thirdParty": true,
    "portrait": false
  }
}
```

테스트 방법:

- 모집 중인 글 신청 → `201`, 상태 `APPLIED`
- 상담 없이 직접 신청도 유효 데이터면 → `201`
- 모집 마감·취소·삭제·정지 센터 글 → `409` 또는 `404`
- 동일 사용자 중복 신청 → `409`
- 정원 초과 동시 신청 → 한 요청만 성공, 나머지 `409`
- 신청자 본인 상세 → `200`, 타 사용자 → `403`
- 취소 가능 기간 내 취소 → `200`, 이후 취소 → `409`
- 센터 신청자 목록 필터·집계 → `200`
- 다른 센터 관리자가 신청자 조회·처리 → `403`
- 필수 서명 미완료 승인 → `409`
- 승인·거절 성공 → `200`, 처리자·시각·사유 저장
- 이미 승인된 신청 재승인 → `409`

---

## 9. 약정·동의서·문서

약정 상태:

```text
DRAFT → IN_REVIEW → REVISION_REQUESTED | APPROVED
→ SIGN_REQUESTED → SIGNING → SIGNED → ACTIVE
→ RENEWAL_DUE | COMPLETED
```

종료 상태: `CANCELLED`, `EXPIRED`

### 9.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [ ] | [ ] | [ ] | POST | `/applications/{applicationId}/commitments` | 신청자 | 신청 기반 약정 초안 생성 |
| [ ] | [ ] | [ ] | GET | `/commitments/me` | USER | 내 약정 목록 |
| [x] | [x] | [x] | GET | `/commitments/{publicId}` | 약정 소유자·해당 센터 관리자 | 약정 상세 |
| [x] | [x] | [x] | PATCH | `/commitments/{publicId}` | 약정 소유자 | 초안·수정요청 상태 편집 |
| [x] | [x] | [x] | POST | `/commitments/{publicId}/submit-review` | 약정 소유자 | 사용자 검토 완료 |
| [x] | [ ] | [ ] | POST | `/commitments/{publicId}/renew` | 약정 소유자 | 월간·연간 활성 약정 갱신 |
| [ ] | [ ] | [ ] | POST | `/commitments/{id}/documents` | 약정 소유자·센터 관리자 | 유형별 문서 생성 |
| [ ] | [ ] | [ ] | GET | `/commitments/{id}/documents` | 약정 소유자·해당 센터 관리자 | 문서 목록 |
| [ ] | [ ] | [ ] | GET | `/documents/{id}` | 문서 접근 권한자 | 문서 메타데이터 |
| [ ] | [ ] | [ ] | GET | `/documents/{id}/preview` | 문서 접근 권한자 | 만료형 미리보기 URL |
| [ ] | [ ] | [ ] | GET | `/documents/{id}/download` | 문서 접근 권한자 | 서명 완료 문서 다운로드 |
| [ ] | [ ] | [ ] | POST | `/documents/{id}/validate` | 문서 접근 권한자 | Information Extract 재검증 |
| [ ] | [ ] | [ ] | POST | `/manager/documents/{id}/request-revision` | 해당 센터 관리자 | 문서 수정 요청 |
| [ ] | [ ] | [ ] | POST | `/manager/documents/{id}/cancel` | 해당 센터 관리자 | 문서 취소 |

현재 MVP에서는 신청 생성 트랜잭션 안에서 약정 초안과 첫 버전, 동의 이력을 자동 생성한다.
따라서 별도 `POST /applications/{applicationId}/commitments`는 아직 구현하지 않는다.
전자서명·서명 완료 PDF 생성은 외부 연동 단계이므로 체크하지 않았다.

약정 초안:

```json
{
  "commitmentType": "VOLUNTEER",
  "startDate": "2026-08-10",
  "endDate": "2026-08-10",
  "specialConditions": "오전 참여",
  "consents": [
    {
      "consentType": "PRIVACY_COLLECTION",
      "agreed": true,
      "version": "2026-07-01"
    },
    {
      "consentType": "PORTRAIT_RIGHTS",
      "agreed": false,
      "version": "2026-07-01"
    }
  ]
}
```

테스트 방법:

- 신청 기반 초안 생성 → `201`, 유형별 필수 문서 선택
- 한 신청에 활성 약정 중복 생성 → `409`
- 타인 신청으로 생성 → `403`
- 필수값·필수동의 누락 → `400`
- 초안 수정 성공 → `200`, 서명 이후 수정 → `409`
- 검토 제출 성공 → `200`, 상태 전이·감사 로그 확인
- 문서 생성 → `201` 또는 비동기 `202`, version·hash·저장 위치 기록
- 같은 약정 버전·문서 유형 중복 요청 → 동일 문서 반환 또는 `409`
- 생성 PDF 필드 재추출 일치 → `200`, 불일치 → `422`
- 타 사용자·타 센터 문서 조회·다운로드 → `403`
- 없는·소프트 삭제 문서 → `404`
- 만료 미리보기 URL → `410`
- 문서 생성 중 저장 실패 → `503`, 약정 상태가 서명 단계로 진행되지 않음

---

## 10. 모두싸인 전자서명

### 10.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [x] | [x] | [ ] | POST | `/clm/documents/request-sign` | 로그인 사용자 | 템플릿 문서 생성 및 `SECURE_LINK` 최초 발급 |
| [x] | [x] | [ ] | POST | `/clm/documents/{id}/secure-link` | 서류 소유자 | 만료된 보안 서명 링크 재발급 |
| [x] | [x] | [ ] | GET | `/clm/documents/{id}` | 서류 소유자·OPERATOR | 저장 상태 조회 및 외부 완료 상태 동기화 |
| [x] | [x] | [ ] | GET | `/clm/documents/my` | 로그인 사용자 | 본인 제출 서류 목록 조회 |
| [x] | [x] | [ ] | GET | `/clm/documents/{id}/files` | 서류 소유자·OPERATOR | 보관된 완료 PDF·감사추적인증서 목록 |
| [x] | [x] | [ ] | GET | `/clm/documents/{id}/files/{fileId}/download` | 서류 소유자·OPERATOR | 보관된 PDF 열람·다운로드 |
| [x] | [x] | [x] | POST | `/webhooks/modusign` | Modusign | 이벤트 ID 멱등·단조 상태 Webhook |
| [x] | [x] | [x] | POST | `/clm/documents/request-sign` | 약정 소유자 | 약정 공개 ID 기반 모두싸인 요청 |
| [ ] | [ ] | [ ] | POST | `/commitments/{id}/signature-requests` | 약정 소유자 | 모두싸인 서명 요청 |
| [ ] | [ ] | [ ] | GET | `/signature-requests/{id}` | 약정 소유자·해당 센터 관리자 | 서명 상태 조회 |
| [ ] | [ ] | [ ] | POST | `/signature-requests/{id}/retry` | 약정 소유자·해당 센터 관리자 | 실패 서명 요청 재시도 |
| [ ] | [ ] | [ ] | POST | `/signature-requests/{id}/sync` | 해당 센터 관리자·OPERATOR | 모두싸인 상태 즉시 동기화 |

현재 MVP 구현은 모두싸인 템플릿 기반 `SECURE_LINK` 방식을 사용한다. 서명 링크는 약 10분 동안만
유효하므로 영구 링크로 취급하지 않고, 사용자가 서명창을 열 때 재발급할 수 있다. 서명 완료 여부는
클라이언트가 임의로 변경하지 않는다. 운영 환경에서는 Webhook으로 확정하며, 로컬 개발처럼 외부에서
Webhook에 접근할 수 없는 환경에서는 `GET /clm/documents/{id}`가 모두싸인 문서 상태를 한 번 조회하여
완료 상태와 결과 파일을 동기화한다. 모두싸인의 순간 호출 제한을 피하기 위해 한 번의 상세 조회 결과로
상태 확인과 파일 보관을 함께 처리한다.
로컬·사설 주소는 공개 HTTPS iframe에서 접근할 수 없으므로 `redirectUrl`로 전달하지 않는다.
프론트는 사용자가 서명을 마친 뒤 완료 확인 버튼을 눌렀을 때 상태를 동기화한다. 서명 도중의 확인이나
모두싸인 호출 제한은 오류 팝업으로 처리하지 않고, 서명 모달 안에 재시도 안내를 표시한다.
배포 환경에서 공개 HTTPS `MODUSIGN_REDIRECT_URL`을 설정한 경우에만 모두싸인에 복귀 주소를 전달한다.
`document_all_signed` 이벤트가 수신되면 문서 상세의 `file.downloadUrl`과
`auditTrail.downloadUrl`을 즉시 내려받아 `app.storage.path/clm`에 영구 보관하고,
DB에는 파일 유형·경로·크기·SHA-256 해시를 기록한다. 임시 다운로드 URL은 문자열 재인코딩 없이
그대로 요청하고, 응답이 실제 PDF 매직 바이트(`%PDF-`)로 시작할 때만 저장한다.

필수 서버 환경변수:

- `MODUSIGN_USER_EMAIL`: API 키를 발급한 모두싸인 계정 이메일
- `MODUSIGN_API_KEY`: 서버 전용 API 키
- `MODUSIGN_TEMPLATE_ID`: 신청 약정서 템플릿 ID
- `MODUSIGN_PARTICIPANT_ROLE`: 템플릿에 설정한 참여자 역할과 정확히 같은 문자열
- `MODUSIGN_REDIRECT_URL`: 서명 후 돌아올 픽셀케어 주소
- `MODUSIGN_WEBHOOK_SECRET`: 모두싸인 Webhook 사용자 지정 헤더에 함께 등록할 비밀값

서명 요청:

```http
POST /api/v1/commitments/72/signature-requests
Idempotency-Key: commitment-72-version-1
```

```json
{
  "documentIds": [501, 502, 503],
  "signer": {
    "name": "권윤재",
    "email": "user@example.com",
    "phone": "01012345678"
  },
  "returnUrl": "https://pixelcare.example.com/my-records/72"
}
```

테스트 방법:

- 승인된 최신 문서 서명 요청 → `201`, 외부 문서·요청·서명자 ID 저장
- 필수 문서 누락·검증 실패 문서 → `400` 또는 `409`
- 같은 `Idempotency-Key` 재호출 → 새 외부 요청을 만들지 않고 기존 결과 반환
- 다른 key로 동일 약정 중복 요청 → `409`
- 모두싸인 `4xx` → 매핑된 `400/409`, `5xx`·timeout → `503`
- 상태 조회 성공 → `200`, 외부 응답 오류 → `502`
- 로컬 Webhook 미수신 상태에서 완료 문서 조회 → `SIGNED` 동기화 및 완료 PDF 2종 보관
- 모두싸인 순간 호출 제한 → 중복 상세 조회 없이 1회 호출, 초과 시 `429` 원인 메시지 반환
- 임시 다운로드 URL 응답이 PDF가 아님 → 파일 저장하지 않고 `502`
- 실패 요청 재시도 → `200`, 성공·진행 중 요청 재시도 → `409`
- Webhook 서명 검증 실패 → `401`
- 유효 Webhook → `200`, 내부 상태 전이·완료 시각 저장
- 같은 이벤트 중복 수신 → 항상 `200`, DB·알림·문서 다운로드는 한 번만 처리
- 순서가 뒤바뀐 과거 상태 Webhook → `200`, 상태 역행 없음
- 완료 Webhook 후 PDF·감사추적인증서 저장 실패 → 재시도 큐 기록, 서명 완료 상태 보존

---

## 11. 센터 신청자·활동 이행 관리

### 11.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [ ] | [ ] | [ ] | POST | `/manager/applications/{id}/attendance` | 해당 센터 관리자 | 출석·결석 처리 |
| [ ] | [ ] | [ ] | POST | `/manager/applications/{id}/complete` | 해당 센터 관리자 | 활동 완료·시간 입력 |
| [ ] | [ ] | [ ] | POST | `/manager/activity-records/{id}/verify` | 해당 센터 관리자 | 활동 인증 승인 |
| [ ] | [ ] | [ ] | GET | `/activity-records/me` | USER | 내 활동 기록 |
| [ ] | [ ] | [ ] | GET | `/activity-records/{id}` | 기록 소유자·해당 센터 관리자 | 활동 상세 |
| [ ] | [ ] | [ ] | POST | `/activity-records/{id}/verification-files` | 기록 소유자 | 인증 자료 업로드 연결 |

활동 완료:

```json
{
  "attendanceStatus": "ATTENDED",
  "attendanceTime": "2026-08-10T09:01:00+09:00",
  "recognizedHours": 3,
  "note": "전체 활동 참여"
}
```

테스트 방법:

- 승인된 신청 출석 처리 → `200`
- 미승인·취소 신청 출석 처리 → `409`
- 결석 처리 후 완료 처리 → `409`
- 음수·활동 시간 초과 인정시간 → `400`
- 완료 처리 → `200`, Application·Commitment·ActivityRecord 상태 일관성 확인
- 다른 센터 관리자 처리 → `403`
- 사용자 본인 기록 조회 → `200`, 타 사용자 → `403`
- 인증 승인 → `200`, 검증자·시각 저장 및 커뮤니티 인증 가능
- 같은 활동 중복 완료·인증 → `409`

---

## 12. 내 기록·변경·해지·갱신

### 12.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [ ] | [ ] | [ ] | GET | `/my-records/summary` | USER | 활동·약정 요약 |
| [ ] | [ ] | [ ] | GET | `/my-records/documents` | USER | 내 증빙·서명 문서 |
| [ ] | [ ] | [ ] | POST | `/commitments/{id}/change-requests` | 약정 소유자 | 변경·해지·갱신 요청 |
| [ ] | [ ] | [ ] | GET | `/commitments/{id}/versions` | 약정 소유자·해당 센터 관리자 | 약정 버전 이력 |
| [ ] | [ ] | [ ] | GET | `/manager/organizations/{id}/renewals` | 해당 센터 관리자 | 갱신 예정 약정 |
| [ ] | [ ] | [ ] | POST | `/manager/change-requests/{id}/approve` | 해당 센터 관리자 | 변경 요청 승인 |
| [ ] | [ ] | [ ] | POST | `/manager/change-requests/{id}/reject` | 해당 센터 관리자 | 변경 요청 거절 |

변경 요청:

```json
{
  "requestType": "RENEWAL",
  "reason": "후원 기간과 금액을 변경하고 싶습니다.",
  "requestedChanges": {
    "amount": 50000,
    "endDate": "2027-08-01"
  }
}
```

테스트 방법:

- 본인 기록·문서만 조회 → `200`
- 타인 문서 ID 변조 → `403`
- 활성 약정 변경·해지·갱신 요청 → `201`
- 완료·취소 약정 변경 요청 → `409`
- 요청 승인 → 새 약정 버전 생성, 이전 버전 보존, 필요 시 재서명 상태로 전환
- 거절 → 상태·사유 저장
- 다른 센터 관리자 승인·거절 → `403`
- 동일 유형 대기 요청 중복 → `409`

---

## 13. 센터 계약 문서·템플릿

### 13.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [ ] | [ ] | [ ] | GET | `/manager/organizations/{id}/documents` | 해당 센터 관리자 | 센터 전체 계약 문서 검색 |
| [ ] | [ ] | [ ] | POST | `/manager/organizations/{id}/templates` | 해당 센터 관리자 | 계약 템플릿 등록 |
| [ ] | [ ] | [ ] | GET | `/manager/organizations/{id}/templates` | 해당 센터 관리자 | 템플릿 목록 |
| [ ] | [ ] | [ ] | GET | `/manager/templates/{id}` | 해당 센터 관리자 | 템플릿 상세·버전 |
| [ ] | [ ] | [ ] | POST | `/manager/templates/{id}/parse` | 해당 센터 관리자 | Document Parse 분석 |
| [ ] | [ ] | [ ] | POST | `/manager/templates/{id}/versions` | 해당 센터 관리자 | 새 템플릿 버전 |
| [ ] | [ ] | [ ] | POST | `/manager/templates/{id}/activate` | 해당 센터 관리자 | 템플릿 활성화 |

테스트 방법:

- 센터 문서 상태·유형·신청자·기간 필터 → `200`
- 다른 센터 문서 조회 → `403`
- PDF·이미지 템플릿 등록 → `201`
- 허용하지 않은 파일·용량 초과 → `415/413`
- Document Parse 성공 → `200` 또는 `202`, 필드 구조 저장
- 파싱 불가·필수 필드 미탐지 → `422`
- 새 버전 생성 후 이전 버전 불변 확인 → `201`
- 필수 필드가 유효한 버전만 활성화 → `200`, 미완성 버전 → `409`

---

# Phase 2 — 커뮤니티·운영·확장 API

## 14. 커뮤니티·댓글·신고

### 14.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [ ] | [ ] | [ ] | GET | `/community/posts` | Public | 공개 게시글 목록·검색 |
| [ ] | [ ] | [ ] | POST | `/community/posts` | USER | 게시글 작성 |
| [ ] | [ ] | [ ] | GET | `/community/posts/{id}` | Public | 게시글 상세 |
| [ ] | [ ] | [ ] | PATCH | `/community/posts/{id}` | 작성자 | 게시글 수정 |
| [x] | [x] | [x] | DELETE | `/api/posts/{id}` | 작성자 또는 OPERATOR | 현재 커뮤니티 목록·상세 화면에서 게시글 소프트 삭제 |
| [x] | [x] | [x] | GET | `/api/posts/me` | USER | 로그인 사용자가 작성한 커뮤니티 글 목록 |
| [ ] | [ ] | [ ] | POST | `/community/posts/{id}/reactions` | USER | 응원 추가·취소 |
| [ ] | [ ] | [ ] | GET | `/community/posts/{id}/comments` | Public | 댓글 목록 |
| [ ] | [ ] | [ ] | POST | `/community/posts/{id}/comments` | USER | 댓글 작성 |
| [ ] | [ ] | [ ] | DELETE | `/community/comments/{id}` | 작성자 | 댓글 삭제 |
| [ ] | [ ] | [ ] | POST | `/community/posts/{id}/reports` | USER | 게시글 신고 |

커뮤니티 삭제 권한 테스트:

- 성공: 게시글 작성자 토큰 또는 운영진 토큰으로 삭제하면 `200`
- 권한 실패: 작성자가 아닌 일반 사용자 토큰이면 `403`
- 인증 실패: 토큰이 없거나 유효하지 않으면 `401`
- 대상 없음: 존재하지 않거나 이미 삭제된 게시글이면 `404`
| [ ] | [ ] | [ ] | POST | `/community/comments/{id}/reports` | USER | 댓글 신고 |
| [ ] | [ ] | [ ] | POST | `/activity-records/{id}/community-draft` | 활동 소유자 | AI 후기 초안 생성 |

게시글 작성:

```json
{
  "activityRecordId": 301,
  "content": "해운대 플로깅 활동을 마쳤어요.",
  "imageFileIds": [1101],
  "visibility": "PUBLIC",
  "shareDonationAmount": false
}
```

테스트 방법:

- 인증 완료 활동 연결 게시글 → `201`, 인증 배지 표시
- 타인 활동 연결 → `403`
- 개인정보·기부금은 공개 동의 없으면 응답에서 제거
- 소프트 삭제 게시글은 목록·검색·상세에서 제외 → `404`
- 작성자 수정·삭제 → `200/204`, 타 사용자 → `403`
- 응원 중복 요청은 토글 또는 `409` 중 정책대로 일관되게 처리
- 삭제 글 댓글·응원·신고 → `404`
- 동일 사용자의 동일 대상 중복 신고 → `409`
- AI 후기 초안에서 개인정보·금액 노출 방지, Upstage 실패 → `503`

---

## 15. 운영진 승인·삭제·제재·감사

### 15.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [ ] | [ ] | [ ] | GET | `/operator/dashboard` | OPERATOR | 운영 대시보드 |
| [ ] | [ ] | [ ] | GET | `/operator/users` | OPERATOR | 사용자 검색·상태 조회 |
| [ ] | [ ] | [ ] | POST | `/operator/users/{id}/suspend` | OPERATOR | 사용자 정지 |
| [ ] | [ ] | [ ] | POST | `/operator/users/{id}/restore` | OPERATOR | 사용자 복구 |
| [ ] | [ ] | [ ] | GET | `/operator/organizations` | OPERATOR | 전체 센터 조회 |
| [ ] | [ ] | [ ] | POST | `/operator/organizations/{id}/suspend` | OPERATOR | 센터 정지 |
| [ ] | [ ] | [ ] | POST | `/operator/organizations/{id}/restore` | OPERATOR | 센터 복구 |
| [x] | [x] | [x] | GET | `/operator/community/posts` | OPERATOR | 커뮤니티 관리 목록 |
| [x] | [x] | [x] | PATCH | `/operator/community/posts/{id}` | OPERATOR | 커뮤니티 글 강제 수정 |
| [x] | [x] | [x] | DELETE | `/operator/community/posts/{id}` | OPERATOR | 커뮤니티 글 소프트 삭제 |
| [ ] | [ ] | [ ] | DELETE | `/operator/community/comments/{id}` | OPERATOR | 댓글 소프트 삭제 |
| [ ] | [ ] | [ ] | GET | `/operator/reports` | OPERATOR | 신고 목록·필터 |
| [ ] | [ ] | [ ] | POST | `/operator/reports/{id}/resolve` | OPERATOR | 신고 처리 |
| [ ] | [ ] | [ ] | GET | `/operator/audit-logs` | OPERATOR | 감사 로그 검색 |

삭제·정지 요청:

```json
{
  "reason": "허위 모집 및 개인정보 노출 확인"
}
```

테스트 방법:

- 운영진 대시보드·검색 → `200`, 일반 사용자 → `403`
- 사용자 정지 → `200`, 이후 로그인·보호 API 접근 차단
- 센터 정지 → `200`, 공개 모집글 목록에서 즉시 제외, 새 모집글 작성 차단
- 복구 → `200`, 복구 가능한 이전 상태만 회복
- 게시글 삭제 → `200`, `isDeleted`, 삭제자·시각 저장
- 삭제 후 목록·검색·상세 → `404`
- 모든 운영 작업에 `AdminAuditLog` 생성
- 이유 누락 → `400`
- 이미 삭제·정지된 대상 재처리 → `409`
- 존재하지 않는 대상 → `404`
- 운영진이 자기 계정 또는 마지막 최고 운영진을 정지하는 위험 작업은 정책에 따라 `409`

### 15.2 전체 제출 서류 조회

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [x] | [x] | [x] | GET | `/operator/documents` | OPERATOR | 전체 신청·약정·필수서류 목록 |
| [x] | [x] | [x] | GET | `/operator/documents/{applicationPublicId}` | OPERATOR | 신청자별 제출 서류 상세 |

운영 화면에서는 응답을 `organizationId → opportunityId → applicantUserId` 순서로 묶어
`센터 → 모집글 → 신청자 → 약정서·필수서류` 흐름으로 표시한다.

테스트 방법:

- 운영진 전체 조회 → `200`, 센터·모집글·신청자와 약정 내용 포함
- 센터 관리자 또는 일반 사용자 조회 → `403`
- 존재하지 않는 신청 공개 ID 상세 → `404`
- 숫자 내부 PK가 아닌 신청 `publicId(UUID)`를 상세 주소에 사용

---

## 16. 알림

### 16.1 엔드포인트 체크리스트

| 구현 | 성공 | 실패 | Method | Path | 권한 | 설명 |
|---|---|---|---|---|---|---|
| [ ] | [ ] | [ ] | GET | `/notifications` | USER | 내 알림 목록 |
| [ ] | [ ] | [ ] | GET | `/notifications/unread-count` | USER | 읽지 않은 알림 수 |
| [ ] | [ ] | [ ] | POST | `/notifications/{id}/read` | 알림 소유자 | 알림 읽음 |
| [ ] | [ ] | [ ] | POST | `/notifications/read-all` | USER | 전체 읽음 |

테스트 방법:

- 본인 알림만 조회 → `200`
- 타인 알림 ID 읽음 처리 → `403`
- 없는 알림 → `404`
- 전체 읽음 → `200`, unread count `0`
- 승인·거절·서명 대기·갱신 예정 이벤트에서 알림 한 건만 생성
- 중복 Webhook이 중복 알림을 만들지 않는지 확인

---

## 17. 외부 연동 어댑터 내부 테스트

아래는 외부에 공개하는 Controller API가 아니라 백엔드 어댑터 테스트 항목이다.

### 17.1 Upstage

- [ ] Solar 정상 응답 파싱
- [ ] JSON Schema 검증 실패
- [ ] Information Extract 필드 일치·불일치
- [ ] Document Parse 성공·파싱 불가
- [ ] `401`, `429`, `5xx`, timeout
- [ ] API 키와 개인정보가 로그에 출력되지 않음

### 17.2 모두싸인

- [ ] 템플릿 기반 `SECURE_LINK` 서명 요청 정상 생성
- [ ] Basic 인증이 `Base64(계정 이메일:API 키)` 형식인지 확인
- [ ] 응답의 문서 ID와 참여자 ID 저장
- [ ] 만료된 보안 서명 링크 재발급
- [ ] idempotency key 재호출
- [ ] 상태 조회
- [ ] 완료 PDF·감사추적인증서 다운로드
- [ ] 다운로드 URL 만료 전에 로컬 저장소 보관 및 SHA-256 검증
- [ ] Webhook 사용자 지정 비밀 헤더 검증
- [ ] 중복·역순 Webhook
- [ ] `4xx`, `5xx`, timeout 후 재조회

---

## 18. E2E 시나리오 체크리스트

### 18.1 센터 관리자 전환

- [ ] USER 회원가입
- [ ] 센터 관리자 권한 신청
- [ ] OPERATOR 승인
- [ ] 사용자 역할에 `CENTER_MANAGER` 추가
- [ ] 승인 전 센터 API `403`, 승인 후 접근 성공

### 18.2 센터 등록·모집글 공개

- [ ] 센터 등록 요청
- [ ] OPERATOR 센터 승인
- [ ] 센터 관리자가 모집글 초안 작성
- [ ] 모집글 공개
- [ ] 일반 `/opportunities`에 자동 노출

### 18.3 AI 상담·봉사 신청·전자서명

- [ ] AI 상담 시작
- [ ] 누락값 후속 질문
- [ ] 의향 최종 확인
- [ ] 추천 활동 선택
- [ ] 봉사 신청
- [ ] 약정·필수 동의서 생성
- [ ] Information Extract 재검증
- [ ] 모두싸인 서명 요청
- [ ] Webhook으로 `SIGNED` 반영
- [ ] 센터 승인
- [ ] 사용자 내 기록과 센터 문서 목록에서 조회

### 18.4 활동 완료·커뮤니티 인증

- [ ] 센터 출석 처리
- [ ] 활동시간과 완료 처리
- [ ] 활동 인증
- [ ] 인증 활동 기반 커뮤니티 글 작성
- [ ] 공개 금지 개인정보·기부금 미노출

### 18.5 운영진 소프트 삭제

- [ ] 운영진이 삭제 사유 입력
- [ ] 모집글 또는 커뮤니티 글 소프트 삭제
- [ ] 일반 목록·검색·상세에서 즉시 제외
- [ ] 삭제자·시각·사유 저장
- [ ] 감사 로그 생성

### 18.6 정기후원 갱신

- [ ] `RENEWAL_DUE` 약정 조회
- [ ] 사용자가 갱신·금액 변경 요청
- [ ] 센터 승인
- [ ] 새 버전 문서 생성
- [ ] 재서명
- [ ] 이전·신규 버전 모두 조회

---

## 19. 구현 순서

의존성을 고려해 다음 순서로 구현한다.

1. 공통 응답·예외 처리·인증·역할
2. 사용자·파일 업로드
3. 센터 관리자 권한 신청과 운영진 승인
4. 센터 등록과 운영진 승인
5. 모집글 작성·공개·공개 조회
6. AI 상담·의향 구조화·추천
7. 봉사·기부 신청과 센터 신청자 관리
8. 약정·동의·문서 생성
9. 모두싸인 요청·Webhook·증빙 보관
10. 출석·활동 완료·내 기록
11. 변경·해지·갱신·버전 관리
12. 커뮤니티·신고·운영 삭제
13. 전체 E2E·동시성·보안 테스트

---

## 20. 완료 정의

API 하나를 완료로 체크하려면 다음을 모두 만족해야 한다.

- 요청·응답 DTO와 Validation 구현
- 역할과 소유권 검사
- 허용된 상태 전이 검사
- 성공 응답 코드·본문 테스트
- `400`, `401`, `403`, `404`, `409` 중 해당하는 실패 테스트
- 외부 API가 있으면 timeout·`4xx`·`5xx` 테스트
- DB 변경과 트랜잭션 롤백 검증
- 민감정보 로그 미노출 확인
- Swagger/OpenAPI 또는 이 문서와 실제 구현 일치
- 테스트 통과 후 체크박스 갱신
