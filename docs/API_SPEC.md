# 📡 Pixel Care REST API 명세서 (API_SPEC.md)

프론트엔드와 백엔드 간 통신을 위한 REST API 명세서입니다. (Phase 1 최우선 메인 기능 ➔ Phase 2 CLM 확장)

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

## 🔥 [Phase 1 최우선 개발] 1. 🤝 봉사 & 기부 API (`/api/volunteers`)

### 1.1 봉사/기부 목록 조회 (`GET /api/volunteers`)
- **Query Params**: `category` (`VOLUNTEER` | `DONATION`)
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

---

## 🔥 [Phase 1 최우선 개발] 2. 💬 픽셀 커뮤니티 API (`/api/posts`)

### 2.1 게시글 목록 조회 (`GET /api/posts`)
- **Query Params**: `category` (`REVIEW` | `RECRUIT` | `GENERAL`), `keyword`
- **Response Format (`200 OK`)**:
```json
[
  {
    "id": 1,
    "title": "🌊 해운대 플로깅 봉사 함께 다녀왔어요!",
    "content": "오늘 주말에 해운대 해변 플로깅 봉사를 신청해서 다녀왔습니다!",
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
  "content": "이번 주 토요일 오전 부산 북구 유기견 보육원 봉사 같이 가실 분 구합니다!",
  "author": "동네 픽셀 기사",
  "category": "RECRUIT"
}
```

### 2.3 게시글 응원 하트/좋아요 (`POST /api/posts/{id}/like`)
- **Response Format (`200 OK`)**: `likes` +1 반영된 객체 반환.

---

## 🔥 [Phase 1 최우선 개발] 3. 🤖 Upstage AI 픽셀 메이트 API (`/api/ai`)

### 3.1 AI 맞춤형 봉사 추천 (`POST /api/ai/recommend`)
- **Request Body**:
```json
{
  "userPrompt": "부산 해운대 근처에서 주말에 3시간 동안 환경 봉사하고 싶어"
}
```
- **Response Format (`200 OK`)**:
```json
{
  "aiMessage": "안녕하세요 픽셀용사님! 🌊 [해운대 해변 픽셀 플로깅 정화 활동]을 추천합니다!",
  "recommendedVolunteerId": 2
}
```

---

## 📄 [Phase 2 CLM 확장] 4. 📜 약정 & 모두싸인 서명 API (`/api/commitments`)

### 4.1 약정서 생성 및 모두싸인 서명 요청 (`POST /api/commitments`)
- Phase 1 메인 기능 완성 후 순차 구현 예정.
