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

## 🤖 4. Upstage AI API (`/api/ai`)

### 4.1 Upstage Solar LLM 기반 봉사/기부 큐레이션 (`POST /api/ai/recommend`)
- **Request Body**:
```json
{
  "userInput": "주말에 부산 금정구 근처에서 유기견 관련 봉사하고 싶어"
}
```
- **Response (`200 OK`)**:
```json
{
  "success": true,
  "data": {
    "reply": "금정구 근처의 유기견 봉사활동 2건을 추천해드릴게요!",
    "recommendedCards": [
      {
        "opportunityId": 1,
        "title": "🐕 부산 북구 유기견 보육원 주말 봉사",
        "region": "부산 북구",
        "badgeReward": "LV1_SEED"
      }
    ]
  }
}
```

---

## 💬 5. 커뮤니티 API (`/api/posts`, `/api/comments`)

### 5.1 게시글 목록 조회 (`GET /api/posts`)
- **Query Parameters**: `category=REVIEW` (ALL/FREE/REVIEW/QUESTION), `sort=latest` (latest/likes), `page=0`, `size=10`
- **Response (`200 OK`)**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "author": {
          "id": 5,
          "nickname": "따뜻한픽셀",
          "badge": "LV2_WARMTH"
        },
        "category": "REVIEW",
        "title": "🐕 부산 유기견 봉사 다녀왔습니다!",
        "contentSnippet": "오늘 아이들과 함께 산책도 하고...",
        "imageUrl": "https://example.com/photo.jpg",
        "likeCount": 12,
        "commentCount": 3,
        "createdAt": "2026-07-29T10:00:00"
      }
    ],
    "page": 0,
    "totalElements": 1
  }
}
```

### 5.2 게시글 상세 조회 (`GET /api/posts/{id}`)
- **Response (`200 OK`)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "author": {
      "id": 5,
      "nickname": "따뜻한픽셀",
      "badge": "LV2_WARMTH"
    },
    "category": "REVIEW",
    "title": "🐕 부산 유기견 봉사 다녀왔습니다!",
    "content": "오늘 아이들과 함께 산책도 하고 밥도 주고 왔습니다. 정말 보람찬 하루였어요!",
    "imageUrl": "https://example.com/photo.jpg",
    "viewCount": 105,
    "likeCount": 12,
    "isLiked": true,
    "createdAt": "2026-07-29T10:00:00"
  }
}
```

### 5.3 게시글 작성 (`POST /api/posts`)
- **Request Body**:
```json
{
  "category": "REVIEW",
  "title": "🐕 부산 유기견 봉사 다녀왔습니다!",
  "content": "오늘 아이들과 함께 산책도 하고 밥도 주고 왔습니다.",
  "imageUrl": "https://example.com/photo.jpg"
}
```
- **Response (`201 Created`)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "🐕 부산 유기견 봉사 다녀왔습니다!",
    "createdAt": "2026-07-29T10:00:00"
  }
}
```

### 5.4 게시글 수정 (`PUT /api/posts/{id}`)
- **Request Body**:
```json
{
  "title": "[수정] 🐕 부산 유기견 봉사 후기",
  "content": "수정된 글 내용입니다.",
  "imageUrl": "https://example.com/photo_updated.jpg"
}
```

### 5.5 게시글 삭제 (`DELETE /api/posts/{id}`)
- **Response (`200 OK`)**:
```json
{
  "success": true,
  "message": "게시글이 삭제되었습니다."
}
```

### 5.6 좋아요 토글 (`POST /api/posts/{id}/like`)
- **Response (`200 OK`)**:
```json
{
  "success": true,
  "data": {
    "postId": 1,
    "isLiked": true,
    "likeCount": 13
  }
}
```

### 5.7 댓글 목록 조회 (`GET /api/posts/{postId}/comments`)
- **Response (`200 OK`)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "author": {
        "id": 8,
        "nickname": "행복봉사자",
        "badge": "LV1_SEED"
      },
      "content": "멋진 봉사 후기네요! 다음번에 저도 같이 가고 싶습니다.",
      "createdAt": "2026-07-29T11:20:00"
    }
  ]
}
```

### 5.8 댓글 작성 (`POST /api/posts/{postId}/comments`)
- **Request Body**:
```json
{
  "content": "멋진 봉사 후기네요! 다음번에 저도 같이 가고 싶습니다.",
  "parentCommentId": null
}
```

### 5.9 댓글 삭제 (`DELETE /api/comments/{commentId}`)
- **Response (`200 OK`)**:
```json
{
  "success": true,
  "message": "댓글이 삭제되었습니다."
}
```

