# 📡 Pixel Care REST API 명세서 (API_SPEC.md)

프론트엔드와 백엔드 간 통신을 위한 REST API 명세서입니다.  
계정 역할(`USER`, `CENTER_MANAGER`, `OPERATOR`)에 따른 권한 및 기능별 엔드포인트를 규정합니다.

---

## 🌐 공통 응답 규약 (Response Format)

```json
{
  "success": true,
  "data": { ... },
  "message": "성공 메세지"
}
```

---

## 🙋‍♂️ 1. 일반 사용자 API (`USER`)

### 1.1 내 신청 봉사 및 이력 조회 (`GET /api/users/me/activities`)
- **Response (`200 OK`)**:
```json
{
  "userId": 1,
  "roles": ["USER"],
  "temperature": 36.5,
  "unlockedBadges": ["LV1_SEED", "LV2_WARMTH"],
  "appliedVolunteers": [
    {
      "applicationId": 101,
      "opportunityId": 1,
      "title": "🐕 부산 북구 유기견 보육원 주말 봉사",
      "status": "APPROVED",
      "appliedAt": "2026-07-28"
    }
  ]
}
```

### 1.2 봉사/기부 간편 신청 (`POST /api/volunteers/{id}/apply`)
- **Request Body**:
```json
{
  "userId": 1,
  "message": "열심히 참여하겠습니다!"
}
```

### 1.3 센터 관리자 권한 신청 (`POST /api/users/manager-applications`)
- **Request Body**:
```json
{
  "organizationName": "부산희망봉사센터",
  "position": "봉사담당자",
  "reason": "지역 어르신 도시락 봉사 모집글 작성을 위함",
  "contact": "010-1234-5678",
  "evidenceFileUrl": "https://example.com/proof.pdf"
}
```

---

## 🏢 2. 센터 관리자 API (`CENTER_MANAGER`)

### 2.1 센터 등록 신청 (`POST /api/center/organizations`)
- **Request Body**:
```json
{
  "name": "부산희망봉사센터",
  "type": "NPO",
  "registrationNumber": "123-82-00000",
  "address": "부산광역시 금정구 부산대학로 63",
  "contact": "051-123-4567",
  "is1365Provider": true
}
```

### 2.2 봉사 모집글 작성 (`POST /api/center/opportunities`)
- **Request Body**:
```json
{
  "organizationId": 10,
  "title": "🍲 금정구 독거어르신 온기 도시락 배달 봉사",
  "description": "지역 어르신들께 따뜻한 도시락을 전달합니다.",
  "region": "BUSAN_GEUMJEONG",
  "recruitmentStartDate": "2026-08-01",
  "recruitmentEndDate": "2026-08-10",
  "activityStartDate": "2026-08-15",
  "activityEndDate": "2026-08-15",
  "status": "PUBLISHED"
}
```

### 2.3 센터 모집글별 신청자 관리 & 승인/출석 완료 (`GET/PUT /api/center/applications`)
- **신청자 승인**: `PUT /api/center/applications/{id}/status` `{"status": "APPROVED"}`
- **출석 완료 및 시간 승인**: `POST /api/center/applications/{id}/complete` `{"recognizedHours": 4}`

---

## 🛡️ 3. 운영진 API (`OPERATOR`)

### 3.1 센터 관리자 요청 목록 조회 및 승인/거절 (`GET/PUT /api/operator/manager-applications`)
- **승인/거절**: `PUT /api/operator/manager-applications/{id}/status`
- **Request Body**:
```json
{
  "status": "APPROVED",
  "rejectionReason": null
}
```

### 3.2 센터 등록 요청 목록 조회 및 승인/거절 (`GET/PUT /api/operator/organization-applications`)
- **승인/거절**: `PUT /api/operator/organization-applications/{id}/status` `{"status": "APPROVED"}`

### 3.3 모집글 및 커뮤니티 게시글 소프트 삭제 (`DELETE /api/operator/posts/{id}`)
- **Request Body**:
```json
{
  "reason": "허위 봉사 모집글로 확인되어 운영진 직권 삭제"
}
```
- **Response (`200 OK`)**: DB `isDeleted = true`, `status = DELETED` 변경 및 `AdminAuditLog` 생성.

### 3.4 감사 로그 조회 (`GET /api/operator/audit-logs`)
- 운영진의 모든 삭제, 승인/거절 이력을 조회합니다.

---

## 🤖 4. Upstage AI & 커뮤니티 API (`/api/ai`, `/api/posts`)

- `POST /api/ai/recommend`: Upstage Solar LLM 기반 사용자 의향 파싱 및 맞춤 봉사 추천
- `GET /api/posts`: 커뮤니티 게시글 목록 조회
- `POST /api/posts`: 커뮤니티 게시글 작성
