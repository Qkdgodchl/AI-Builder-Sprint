# 📡 Pixel Care REST API 명세서 (API_SPEC.md)

프론트엔드와 백엔드 간 통신을 위한 REST API 명세서입니다. (봉사 직접 관리 & 단체 전용 기금 모금 파이프라인)

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

## 🔥 1. 🤝 봉사 공고 & 단체 기금 API (`/api/volunteers`)

### 1.1 봉사 공고 / 단체 기금 목록 조회 (`GET /api/volunteers`)
- **Query Params**: `category` (`VOLUNTEER` | `DONATION`)
- **Response Format (`200 OK`)**:
```json
[
  {
    "id": 1,
    "title": "🐕 부산 북구 유기견 보육원 주말 봉사",
    "category": "VOLUNTEER",
    "location": "부산 북구 동물보호센터",
    "organizer": "부산 동네 온기 봉사단",
    "targetAmount": null,
    "currentAmount": 0,
    "tags": ["자체 관리", "4시간 인정", "주말"]
  },
  {
    "id": 2,
    "title": "🍲 금정구 독거어르신 온기 도시락 전용 기금",
    "category": "DONATION",
    "location": "부산 금정구 종합복지관",
    "organizer": "사랑의 픽셀 이웃",
    "targetAmount": 1000000,
    "currentAmount": 650000,
    "tags": ["전용기금", "독거어르신"]
  }
]
```

### 1.2 봉사 공고 / 단체 기금 직접 등록 (`POST /api/volunteers`)
- **Request Body**:
```json
{
  "title": "🌊 해운대 해변 플로깅 정화 봉사단 모집",
  "category": "VOLUNTEER",
  "location": "부산 해운대 구남로 광장",
  "organizer": "그린 픽셀 에코 클럽",
  "targetAmount": null,
  "tags": ["플로깅", "환경정화"]
}
```

### 1.3 단체 전용 기금 후원하기 (`POST /api/volunteers/{id}/donate`)
- **Request Body**:
```json
{
  "donorName": "권윤재",
  "amount": 50000
}
```
- **Response Format (`200 OK`)**: 누적 모금액(`currentAmount`) 및 모금 달성률(%) 업데이트 반환.

---

## 🔥 2. 💬 픽셀 커뮤니티 API (`/api/posts`)

### 2.1 게시글 목록 조회 (`GET /api/posts`)
- **Query Params**: `category` (`REVIEW` | `RECRUIT` | `GENERAL`), `keyword`
- **Response Format (`200 OK`)**: 목록 및 작성자 반환.

---

## 🔥 3. 🤖 Upstage AI 픽셀 메이트 API (`/api/ai`)

### 3.1 AI 맞춤형 봉사/기금 큐레이션 (`POST /api/ai/recommend`)
- **Request Body**:
```json
{
  "userPrompt": "부산 해운대 근처에서 주말에 3시간 동안 할 수 있는 봉사 알려줘"
}
```
- **Response Format (`200 OK`)**:
```json
{
  "aiMessage": "안녕하세요 픽셀용사님! 🌊 플랫폼에 직접 등록된 [해운대 해변 픽셀 플로깅 정화 활동] 봉사를 추천합니다!",
  "recommendedVolunteerId": 1
}
```
