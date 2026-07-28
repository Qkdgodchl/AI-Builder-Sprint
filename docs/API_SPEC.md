# 📡 Pixel Care CLM REST API 명세서 (API_SPEC.md)

선행 약정 플랫폼 (Pixel Care CLM)의 프론트엔드, 백엔드, Upstage AI, 모두싸인(Modusign) 간 REST API 통신 규약입니다.

---

## 🌐 공통 응답 규약 (Common Response Format)
```json
{
  "success": true,
  "data": { ... },
  "message": "성공 메세지"
}
```

---

## 1. 🤖 Upstage AI 선행 상담 API (`/api/ai`)

### 1.1 AI 선행 의향 상담 (`POST /api/ai/chat`)
- **Request Body**:
```json
{
  "userPrompt": "부산에서 문화유산 복원 후원을 매달 10만원씩 2026년 8월부터 하고 싶어요",
  "userId": 1
}
```
- **Response Format (`200 OK`)**:
```json
{
  "aiResponse": "픽셀용사님! 부산 문화재 복원 정기후원 의향을 다음과 같이 정리했습니다.",
  "structuredIntent": {
    "commitmentType": "CULTURAL_HERITAGE_DONATION",
    "targetOrganizationId": "ORG-102",
    "amount": 100000,
    "paymentCycle": "MONTHLY",
    "startDate": "2026-08-01",
    "taxDeductionConsent": true,
    "privacyConsent": true,
    "specialConditions": "문화재 복원 사업에 사용"
  },
  "isReadyForContract": true
}
```

---

## 2. 📑 CLM 약정 & 전자서명 API (`/api/commitments`)

### 2.1 약정서 생성 요청 (`POST /api/commitments`)
- **Request Body**:
```json
{
  "userId": 1,
  "opportunityId": 10,
  "commitmentType": "CULTURAL_HERITAGE_DONATION",
  "amount": 100000,
  "paymentCycle": "MONTHLY",
  "startDate": "2026-08-01",
  "donorName": "권윤재",
  "donorPhone": "010-1234-5678"
}
```
- **Response Format (`201 Created`)**:
```json
{
  "commitmentId": 1001,
  "status": "DRAFT",
  "contractDocumentUrl": "/documents/contract-1001.pdf",
  "createdAt": "2026-07-28T20:10:00"
}
```

### 2.2 모두싸인 서명 요청 API (`POST /api/commitments/{id}/request-signature`)
- **Response Format (`200 OK`)**:
```json
{
  "commitmentId": 1001,
  "modusignDocumentId": "doc_modu_9982",
  "signatureRequestId": "sig_req_1001",
  "signatureUrl": "https://modusign.co.kr/sign/link_sample_key",
  "status": "SIGN_REQUESTED"
}
```

### 2.3 모두싸인 서명 완료 Webhook 콜백 (`POST /api/webhooks/modusign`)
- **Request Body (Modusign Webhook Payload)**:
```json
{
  "event": "DOCUMENT_SIGNED",
  "modusignDocumentId": "doc_modu_9982",
  "status": "SIGNED",
  "signedPdfUrl": "https://storage.pixelcare.kr/signed/contract-1001.pdf",
  "auditTrailUrl": "https://storage.pixelcare.kr/audit/audit-1001.pdf"
}
```

### 2.4 내 약정서 및 증빙 목록 조회 (`GET /api/commitments/my`)
- **Response Format (`200 OK`)**:
```json
[
  {
    "commitmentId": 1001,
    "title": "부산 근현대문화유산 보존 정기후원",
    "commitmentType": "CULTURAL_HERITAGE_DONATION",
    "status": "ACTIVE",
    "amount": 100000,
    "paymentCycle": "MONTHLY",
    "signedPdfUrl": "/documents/signed-1001.pdf",
    "auditTrailUrl": "/documents/audit-1001.pdf"
  }
]
```

---

## 3. 🎁 선행 탐색 & 카탈로그 API (`/api/opportunities`)

### 3.1 선행 활동 목록 조회 (`GET /api/opportunities`)
- **Query Params**:
  - `type`: `VOLUNTEER` | `DONATION` | `GOHYANG` | `CULTURAL_HERITAGE` | `HERITAGE_WILL`
  - `region`: `BUSAN`
  - `keyword`: 검색어
- **Response Format (`200 OK`)**:
```json
[
  {
    "opportunityId": 10,
    "organizationId": "ORG-102",
    "type": "CULTURAL_HERITAGE",
    "title": "부산 근현대문화유산 보존 후원",
    "organizationName": "부산문화재단",
    "targetAmount": 10000000,
    "currentAmount": 7000000,
    "achievementRate": 70
  }
]
```

---

## 4. 💬 선행 인증 커뮤니티 API (`/api/posts`)

### 4.1 인증된 선행 커뮤니티 작성 (`POST /api/posts`)
- **Request Body**:
```json
{
  "userId": 1,
  "commitmentId": 1001,
  "title": "부산 문화유산 보존 정기후원 참여했습니다!",
  "content": "우리 지역 문화유산을 지키는 선행 약정에 참여했습니다. 따뜻한 마음이 이어지길 바랍니다.",
  "imageUrl": "/images/proof-1001.jpg",
  "isAnonymous": false
}
```
- **Response Format (`201 Created`)**:
```json
{
  "postId": 501,
  "verifiedBadge": "인증된 선행 약정",
  "likes": 0,
  "createdAt": "2026-07-28T20:14:00"
}
```

### 4.2 커뮤니티 게시글 목록 조회 (`GET /api/posts`)
- **Response Format (`200 OK`)**: 목록 및 작성자 인증 배지 반환.
