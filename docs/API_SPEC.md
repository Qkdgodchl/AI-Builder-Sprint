# 📡 Pixel Care REST API 명세서 (API_SPEC.md)

프론트엔드와 백엔드 간 데이터를 주고받기 위한 REST API 명세서입니다.

---

## 🤝 1. 봉사 & 기부 API (`/api/volunteers`)

### 1.1 봉사/기부 목록 조회 (`GET /api/volunteers`)
- **Query Params**:
  - `category` (optional): `VOLUNTEER` | `DONATION`
- **Response Format (`200 OK`)**:
```json
[
  {
    "id": 1,
    "title": "🌊 해운대 해변 픽셀 플로깅 정화 활동",
    "category": "VOLUNTEER",
    "location": "부산 해운대 구남로 광장",
    "organizer": "그린 픽셀 에코 클럽",
    "targetAmount": null,
    "currentAmount": 0,
    "tags": ["1365 연동", "3시간 인정", "환경정화"],
    "link1365": "https://www.1365.go.kr/vols/1365/act/volsList.do?searchKeyword=플로깅"
  }
]
```

### 1.2 신규 봉사/기부 프로젝트 등록 (`POST /api/volunteers`)
- **Request Body**:
```json
{
  "title": "🍲 금정구 어르신 도시락 픽셀 펀딩",
  "category": "DONATION",
  "location": "부산 금정구 종합복지관",
  "organizer": "사랑의 픽셀 이웃",
  "targetAmount": 500000,
  "tags": ["기부", "독거어르신"]
}
```
- **Response Format (`201 Created`)**: 등록된 봉사/기부 객체 반환.

---

## 💬 2. 픽셀 커뮤니티 API (`/api/posts`)

### 2.1 게시글 목록 조회 (`GET /api/posts`)
- **Query Params**:
  - `category` (optional): `REVIEW` | `RECRUIT` | `GENERAL`
  - `keyword` (optional): 검색어
- **Response Format (`200 OK`)**:
```json
[
  {
    "id": 1,
    "title": "🌊 해운대 플로깅 봉사 함께 다녀왔어요!",
    "content": "오늘 주말에 해운대 해변 플로깅 봉사를 신청해서 다녀왔습니다! 쓰레기 3kg이나 줍고 깨끗해져서 뿌듯하네요.",
    "author": "해운대 픽셀용사",
    "category": "REVIEW",
    "likes": 15,
    "views": 42,
    "createdAt": "2026-07-28T19:35:51"
  }
]
```

### 2.2 신규 게시글 작성 (`POST /api/posts`)
- **Request Body**:
```json
{
  "title": "🐕 주말 유기견 보육원 봉사 같이 가실 분!",
  "content": "이번 주 토요일 오전 부산 북구 유기견 보육원 봉사 카풀해서 가실 분 구합니다!",
  "author": "동네 픽셀 기사",
  "category": "RECRUIT"
}
```
- **Response Format (`201 Created`)**: 생성된 게시글 객체 반환.

### 2.3 게시글 응원 하트/좋아요 (`POST /api/posts/{id}/like`)
- **Response Format (`200 OK`)**: 업데이트된 게시글 객체 반환 (`likes` +1).

### 2.4 댓글 목록 조회 (`GET /api/posts/{postId}/comments`)
- **Response Format (`200 OK`)**:
```json
[
  {
    "id": 1,
    "postId": 1,
    "author": "센텀 온기용사",
    "content": "우와 수고 많으셨습니다! 다음엔 저도 같이 참여하고 싶어요!",
    "createdAt": "2026-07-28T19:40:00"
  }
]
```

### 2.5 댓글 작성 (`POST /api/posts/{postId}/comments`)
- **Request Body**:
```json
{
  "author": "센텀 온기용사",
  "content": "우와 수고 많으셨습니다!"
}
```

---

## 🤖 3. Upstage AI 추천 API (`/api/ai`)

### 3.1 맞춤형 봉사 추천 대화 (`POST /api/ai/recommend`)
- **Request Body**:
```json
{
  "userPrompt": "부산 해운대 근처에서 주말에 3시간 동안 환경 봉사하고 싶어"
}
```
- **Response Format (`200 OK`)**:
```json
{
  "aiMessage": "안녕하세요 픽셀용사님! 🌊 해운대 구남로 광장에서 진행되는 [해운대 해변 픽셀 플로깅 정화 활동]을 추천합니다! 3시간 봉사 실적이 인정되는 추천 미션이에요!",
  "recommendedVolunteerId": 2
}
```
