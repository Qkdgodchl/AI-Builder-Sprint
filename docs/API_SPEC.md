# 📡 Pixel Care REST API 명세서 (API_SPEC.md)

프론트엔드와 백엔드 간 통신을 위한 REST API 명세서입니다. (일반 참가자 / 주최측 단체 계정 권한 분리)

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

## 🙋‍♂️ 1. 일반 참가자 마이페이지 API (`ROLE_USER`)

### 1.1 내 신청 봉사 및 기부 내역 조회 (`GET /api/users/me/activities`)
- **Response Format (`200 OK`)**:
```json
{
  "userRole": "ROLE_USER",
  "temperature": 78.4,
  "unlockedBadges": ["LV1_SEED", "LV2_WARMTH", "LV3_PIXEL"],
  "appliedVolunteers": [
    {
      "applicationId": 101,
      "volunteerId": 1,
      "title": "🐕 부산 북구 유기견 보육원 주말 봉사",
      "status": "APPROVED",
      "appliedAt": "2026-07-28"
    }
  ],
  "donations": [
    {
      "donationId": 201,
      "fundTitle": "🍲 금정구 독거어르신 온기 도시락 전용 기금",
      "amount": 50000,
      "donatedAt": "2026-07-27"
    }
  ]
}
```

### 1.2 봉사 간편 신청하기 (`POST /api/volunteers/{id}/apply`)
- **Request Body**:
```json
{
  "userId": 1,
  "message": "열심히 참여하겠습니다!"
}
```

---

## 🏢 2. 봉사 주최측 / 단체 마이페이지 API (`ROLE_ORGANIZER`)

### 2.1 단체 등록 공고 목록 및 신청자 조회 (`GET /api/organizer/volunteers`)
- **Response Format (`200 OK`)**:
```json
[
  {
    "volunteerId": 1,
    "title": "🐕 부산 북구 유기견 보육원 주말 봉사",
    "totalApplicants": 5,
    "approvedApplicants": 3,
    "applicants": [
      {
        "applicationId": 101,
        "userId": 1,
        "userName": "해운대 픽셀용사",
        "status": "PENDING",
        "appliedAt": "2026-07-28"
      }
    ]
  }
]
```

### 2.2 주최측 신청자 승인 / 거절 (`PUT /api/organizer/applications/{applicationId}/status`)
- **Request Body**:
```json
{
  "status": "APPROVED"
}
```

### 2.3 봉사 활동 완료 및 출석 승인 (`POST /api/organizer/applications/{applicationId}/complete`)
- **Response Format (`200 OK`)**: 참가 유저의 봉사 완료 처리 및 온기 온도계 +0.5°C 반영.

---

## 🔥 3. 💬 픽셀 커뮤니티 API (`/api/posts`) & 🤖 Upstage AI API (`/api/ai`)
- `GET /api/posts`: 커뮤니티 게시글 목록 조회
- `POST /api/posts`: 커뮤니티 글 작성
- `POST /api/ai/recommend`: Upstage Solar LLM 파이프라인 맞춤 봉사 추천
