# SMIM Backend API Specification

> **Version**: 1.1.0
> **Base URL**: `https://api.smim.io` (Production) / `http://localhost:8080` (Development)
> **Last Updated**: 2026-01-30
> **OpenAPI Spec Compatible**: 3.1.0  

---

## Table of Contents

1. [Overview](#1-overview)
2. [Authentication](#2-authentication)
3. [Common Response Format](#3-common-response-format)
4. [Error Handling](#4-error-handling)
5. [Rate Limiting](#5-rate-limiting)
6. [API Endpoints](#6-api-endpoints)
   - [6.1 Auth APIs](#61-auth-apis)
   - [6.2 User APIs](#62-user-apis)
   - [6.3 Article APIs](#63-article-apis)
   - [6.4 Vocabulary Book APIs](#64-vocabulary-book-apis)
   - [6.5 Usage APIs](#65-usage-apis)
   - [6.6 Article Recommendation APIs](#66-article-recommendation-apis)
   - [6.7 Learning & Review APIs](#67-learning--review-apis)
   - [6.8 Notification APIs](#68-notification-apis)
7. [Data Models](#7-data-models)
8. [Appendix](#appendix)

---

## 1. Overview

### 1.1 About SMIM API

SMIM(Smart Media Insight Manager) API는 뉴스 기사 및 웹 글의 URL을 크롤링하고, Google Gemini AI를 활용하여 영어 학습에 유용한 단어와 문장을 추출하는 서비스의 백엔드 API입니다.

### 1.2 Key Features

| Feature | Description |
|---------|-------------|
| **Authentication** | OAuth2 (Google, Kakao) + Email/Password + JWT |
| **URL Crawling** | Jsoup 기반 웹 페이지 본문 추출 |
| **AI Vocabulary Extraction** | Google Gemini API 기반 비동기 단어 추출 |
| **Vocabulary Management** | 커스텀 단어장 CRUD 및 단어 관리 |
| **Rate Limiting** | Redis 기반 사용자별 일일 사용량 제한 |
| **CEFR-based Recommendations** | 사용자 영어 수준에 맞는 아티클 추천 |
| **Learning & Review** | SM-2 알고리즘 기반 복습 스케줄링 및 퀴즈 |
| **Notifications** | 이메일/카카오톡 복습 알림 |

### 1.3 Technical Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.1 |
| Database | PostgreSQL |
| Cache | Redis |
| AI | Google Gemini API |

---

## 2. Authentication

### 2.1 Authentication Methods

SMIM API는 두 가지 인증 방식을 지원합니다:

| Method | Description |
|--------|-------------|
| **JWT Bearer Token** | API 요청 시 Authorization 헤더에 Bearer 토큰 포함 |
| **OAuth2** | Google, Kakao 소셜 로그인 |

### 2.2 JWT Token Structure

```http
Authorization: Bearer <access_token>
```

| Token Type | Expiry | Storage |
|------------|--------|---------|
| Access Token | 30 minutes | Client-side (Memory/LocalStorage) |
| Refresh Token | 14 days | Redis (Server-side) |

### 2.3 Token Refresh Flow

```
Access Token 만료 → POST /api/auth/refresh (Refresh Token) → 새 Token Pair 발급
```

---

## 3. Common Response Format

### 3.1 Success Response

모든 성공 응답은 다음 형식을 따릅니다:

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { ... }
}
```

### 3.2 Error Response

모든 에러 응답은 다음 형식을 따릅니다:

```json
{
  "isSuccess": false,
  "code": "AR003",
  "message": "아티클을 찾을 수 없습니다.",
  "data": null
}
```

### 3.3 Pagination Response

페이지네이션이 지원되는 API의 응답 형식:

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "content": [ ... ],
    "totalElements": 100,
    "totalPages": 10,
    "currentPage": 0,
    "size": 10
  }
}
```

---

## 4. Error Handling

### 4.1 Error Code Reference

#### Common Errors (CO)

| Code | Message | HTTP Status | Description |
|------|---------|-------------|-------------|
| CO001 | 서버 내부 오류가 발생했습니다. | 500 | Internal Server Error |
| CO002 | 잘못된 요청입니다. | 400 | Invalid Request |
| CO003 | 리소스를 찾을 수 없습니다. | 404 | Resource Not Found |
| CO004 | 지원하지 않는 HTTP 메서드입니다. | 405 | Method Not Allowed |
| CO005 | 입력값 검증에 실패했습니다. | 400 | Validation Failed |

#### Auth Errors (AU)

| Code | Message | HTTP Status | Description |
|------|---------|-------------|-------------|
| AU001 | 인증되지 않은 요청입니다. | 401 | Unauthorized |
| AU002 | 유효하지 않은 토큰입니다. | 401 | Invalid Token |
| AU003 | 만료된 토큰입니다. | 401 | Expired Token |
| AU004 | 접근 권한이 없습니다. | 403 | Access Denied |
| AU005 | 지원하지 않는 소셜 로그인입니다. | 400 | Unsupported Provider |
| AU006 | 비밀번호가 올바르지 않습니다. | 401 | Invalid Password |
| AU007 | 소셜 로그인으로 가입된 계정입니다. | 401 | Social Account Login Required |

#### User Errors (US)

| Code | Message | HTTP Status | Description |
|------|---------|-------------|-------------|
| US001 | 사용자를 찾을 수 없습니다. | 404 | User Not Found |
| US002 | 이미 등록된 이메일입니다. | 409 | Duplicate Email |

#### Article Errors (AR)

| Code | Message | HTTP Status | Description |
|------|---------|-------------|-------------|
| AR001 | 유효하지 않은 URL입니다. | 400 | Invalid URL |
| AR002 | 크롤링에 실패했습니다. | 400 | Crawling Failed |
| AR003 | 아티클을 찾을 수 없습니다. | 404 | Article Not Found |
| AR004 | 단어 추출이 진행 중입니다. | 409 | Extraction In Progress |
| AR005 | 해당 아티클에 대한 접근 권한이 없습니다. | 403 | Article Access Denied |

#### Vocabulary Book Errors (VB)

| Code | Message | HTTP Status | Description |
|------|---------|-------------|-------------|
| VB001 | 단어장을 찾을 수 없습니다. | 404 | Vocabulary Book Not Found |
| VB002 | 기본 단어장은 삭제할 수 없습니다. | 400 | Default Book Cannot Be Deleted |
| VB003 | 이미 존재하는 단어장 이름입니다. | 409 | Duplicate Book Name |
| VB004 | 단어장 개수가 최대 한도를 초과했습니다. | 400 | Max Books Exceeded (Max: 20) |
| VB005 | 이미 단어장에 존재하는 단어입니다. | 409 | Duplicate Entry |

#### Rate Limit Errors (RL)

| Code | Message | HTTP Status | Description |
|------|---------|-------------|-------------|
| RL001 | 일일 사용 한도를 초과했습니다. | 429 | Daily Limit Exceeded |

---

## 5. Rate Limiting

### 5.1 Rate Limit Policy

| Role | Daily Limit | Reset Time |
|------|-------------|------------|
| USER | 5 requests | 00:00:00 KST |
| ADMIN | Unlimited | N/A |

### 5.2 Rate Limited Endpoints

| Endpoint | Description |
|----------|-------------|
| `POST /api/articles/crawl` | URL 크롤링 |
| `POST /api/articles/{id}/re-extract` | 단어 재추출 |

### 5.3 Rate Limit Response Headers

```http
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 3
X-RateLimit-Reset: 2026-01-30T00:00:00+09:00
```

### 5.4 Rate Limit Exceeded Response

```json
{
  "isSuccess": false,
  "code": "RL001",
  "message": "일일 사용 한도를 초과했습니다.",
  "data": null
}
```

---

## 6. API Endpoints

---

### 6.1 Auth APIs

#### 6.1.1 Email Signup

이메일과 비밀번호로 회원가입합니다.

```
POST /api/auth/signup
```

**Authentication**: Not Required

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "SecurePass1!",
  "name": "홍길동"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| email | String | ✅ | RFC 5322 format, max 255 chars |
| password | String | ✅ | Min 8 chars, uppercase + lowercase + number + special char |
| name | String | ✅ | Min 2 chars, max 50 chars |

**Success Response** `201 Created`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 409 | US002 | 이미 등록된 이메일 |
| 400 | CO005 | 유효성 검증 실패 |

---

#### 6.1.2 Email Login

이메일과 비밀번호로 로그인합니다.

```
POST /api/auth/login
```

**Authentication**: Not Required

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "SecurePass1!"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| email | String | ✅ | Valid email format |
| password | String | ✅ | Not empty |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 401 | AU001 | 이메일/비밀번호 불일치 |
| 401 | AU007 | 소셜 로그인 전용 계정 |

---

#### 6.1.3 OAuth2 Social Login

OAuth2 기반 소셜 로그인을 시작합니다.

```
GET /oauth2/authorize/{provider}
```

**Authentication**: Not Required

**Path Parameters**

| Parameter | Type | Required | Values |
|-----------|------|----------|--------|
| provider | String | ✅ | `google`, `kakao` |

**Response**: OAuth2 Provider의 인증 페이지로 리다이렉트

**Callback URL**

```
GET /login/oauth2/code/{provider}
```

인증 성공 시 프론트엔드 URL로 리다이렉트되며, 쿼리 파라미터로 토큰이 전달됩니다.

```
{FRONTEND_URL}/oauth/callback?accessToken=...&refreshToken=...
```

---

#### 6.1.4 Token Refresh

Refresh Token으로 새로운 토큰 쌍을 발급받습니다.

```
POST /api/auth/refresh
```

**Authentication**: Not Required

**Request Body**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 401 | AU002 | 유효하지 않은 Refresh Token |
| 401 | AU003 | 만료된 Refresh Token |

---

#### 6.1.5 Logout

현재 세션을 로그아웃합니다.

```
POST /api/auth/logout
```

**Authentication**: Bearer Token Required

**Success Response**: `204 No Content`

---

### 6.2 User APIs

#### 6.2.1 Get My Profile

현재 로그인한 사용자의 프로필을 조회합니다.

```
GET /api/users/me
```

**Authentication**: Bearer Token Required

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "profileImage": "https://example.com/profile.jpg",
    "provider": "GOOGLE",
    "role": "USER",
    "createdAt": "2026-01-15T10:30:00"
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 401 | AU001 | 인증되지 않은 요청 |
| 404 | US001 | 사용자를 찾을 수 없음 |

---

#### 6.2.2 Update My Profile

현재 로그인한 사용자의 프로필을 수정합니다.

```
PATCH /api/users/me
```

**Authentication**: Bearer Token Required

**Request Body**

```json
{
  "name": "새이름",
  "profileImage": "https://example.com/new-profile.jpg"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| name | String | ❌ | Min 2 chars, max 50 chars |
| profileImage | String | ❌ | Valid URL, max 500 chars |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "새이름",
    "profileImage": "https://example.com/new-profile.jpg",
    "provider": "GOOGLE",
    "role": "USER",
    "createdAt": "2026-01-15T10:30:00"
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 400 | CO005 | 유효성 검증 실패 |
| 404 | US001 | 사용자를 찾을 수 없음 |

---

### 6.3 Article APIs

#### 6.3.1 Crawl URL and Create Article

URL을 크롤링하여 아티클을 생성하고, AI 단어 추출을 시작합니다.

```
POST /api/articles/crawl
```

**Authentication**: Bearer Token Required  
**Rate Limited**: ✅ (USER: 5/day)

**Request Body**

```json
{
  "url": "https://www.bbc.com/news/example-article"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| url | String | ✅ | Valid HTTP/HTTPS URL, max 2000 chars |

**Success Response** `201 Created`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "title": "Example News Article",
    "content": "Article content...",
    "originalUrl": "https://www.bbc.com/news/example-article",
    "sourceDomain": "bbc.com",
    "isCompleted": false,
    "vocabularyList": [],
    "createdAt": "2026-01-29T14:30:00"
  }
}
```

**Response Headers**

```http
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 4
X-RateLimit-Reset: 2026-01-30T00:00:00+09:00
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 400 | AR001 | 유효하지 않은 URL |
| 400 | AR002 | 크롤링 실패 (타임아웃, 접근 불가 등) |
| 429 | RL001 | 일일 사용량 초과 |

---

#### 6.3.2 Get My Articles

현재 사용자의 아티클 목록을 조회합니다.

```
GET /api/articles
```

**Authentication**: Bearer Token Required

**Query Parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | 페이지 번호 (0-based) |
| size | int | 10 | 페이지 크기 (max: 50) |
| sort | String | createdAt,desc | 정렬 기준 |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Example News Article",
        "originalUrl": "https://www.bbc.com/news/example",
        "sourceDomain": "bbc.com",
        "isCompleted": true,
        "vocabularyCount": 10,
        "createdAt": "2026-01-29T14:30:00"
      }
    ],
    "totalElements": 25,
    "totalPages": 3,
    "currentPage": 0,
    "size": 10
  }
}
```

---

#### 6.3.3 Get Article Detail

특정 아티클의 상세 정보를 조회합니다.

```
GET /api/articles/{articleId}
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| articleId | Long | ✅ | 아티클 ID |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "title": "Example News Article",
    "content": "Full article content...",
    "originalUrl": "https://www.bbc.com/news/example",
    "sourceDomain": "bbc.com",
    "isCompleted": true,
    "vocabularyList": [
      {
        "id": 101,
        "word": "unprecedented",
        "definition": "never done or known before",
        "contextSentence": "The country faced unprecedented challenges."
      }
    ],
    "createdAt": "2026-01-29T14:30:00"
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 404 | AR003 | 아티클을 찾을 수 없음 |
| 403 | AU004 | 접근 권한 없음 |

---

#### 6.3.4 Delete Article

아티클과 연관된 AI 추출 단어를 삭제합니다.

```
DELETE /api/articles/{articleId}
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| articleId | Long | ✅ | 아티클 ID |

**Success Response**: `204 No Content`

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 404 | AR003 | 아티클을 찾을 수 없음 |
| 403 | AU004 | 접근 권한 없음 |

---

#### 6.3.5 Re-extract Vocabulary

아티클의 단어를 재추출합니다.

```
POST /api/articles/{articleId}/re-extract
```

**Authentication**: Bearer Token Required  
**Rate Limited**: ✅ (USER: 5/day, 크롤링과 합산)

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| articleId | Long | ✅ | 아티클 ID |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "articleId": 1,
    "status": "EXTRACTING",
    "message": "단어 재추출이 시작되었습니다."
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 404 | AR003 | 아티클을 찾을 수 없음 |
| 403 | AU004 | 접근 권한 없음 |
| 409 | AR004 | 이미 추출 진행 중 |
| 429 | RL001 | 일일 사용량 초과 |

---

#### 6.3.6 Get Extraction Status

아티클의 AI 단어 추출 상태를 조회합니다.

```
GET /api/articles/{articleId}/extraction-status
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| articleId | Long | ✅ | 아티클 ID |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "articleId": 1,
    "status": "COMPLETED",
    "vocabularyCount": 10
  }
}
```

**Status Values**

| Status | Description |
|--------|-------------|
| EXTRACTING | AI 단어 추출 진행 중 |
| COMPLETED | 추출 완료 |
| FAILED | 추출 실패 (생성 후 5분 이상 경과) |

---

### 6.4 Vocabulary Book APIs

#### 6.4.1 Create Vocabulary Book

새 단어장을 생성합니다.

```
POST /api/vocabulary-books
```

**Authentication**: Bearer Token Required

**Request Body**

```json
{
  "name": "TOEIC 필수 단어",
  "description": "TOEIC 시험에 자주 출제되는 단어 모음"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| name | String | ✅ | Min 1 char, max 50 chars |
| description | String | ❌ | Max 200 chars |

**Success Response** `201 Created`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 2,
    "name": "TOEIC 필수 단어",
    "description": "TOEIC 시험에 자주 출제되는 단어 모음",
    "isDefault": false,
    "wordCount": 0,
    "createdAt": "2026-01-29T15:00:00"
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 409 | VB003 | 동일 이름의 단어장 존재 |
| 400 | VB004 | 단어장 개수 초과 (max: 20) |

---

#### 6.4.2 Get Vocabulary Books

현재 사용자의 모든 단어장을 조회합니다.

```
GET /api/vocabulary-books
```

**Authentication**: Bearer Token Required

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "id": 1,
      "name": "기본 단어장",
      "description": null,
      "isDefault": true,
      "wordCount": 45,
      "createdAt": "2026-01-15T10:30:00"
    },
    {
      "id": 2,
      "name": "TOEIC 필수 단어",
      "description": "TOEIC 시험에 자주 출제되는 단어 모음",
      "isDefault": false,
      "wordCount": 12,
      "createdAt": "2026-01-29T15:00:00"
    }
  ]
}
```

---

#### 6.4.3 Update Vocabulary Book

단어장 정보를 수정합니다.

```
PATCH /api/vocabulary-books/{bookId}
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| bookId | Long | ✅ | 단어장 ID |

**Request Body**

```json
{
  "name": "수정된 단어장 이름",
  "description": "수정된 설명"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| name | String | ❌ | Min 1 char, max 50 chars |
| description | String | ❌ | Max 200 chars |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 2,
    "name": "수정된 단어장 이름",
    "description": "수정된 설명",
    "isDefault": false,
    "wordCount": 12,
    "createdAt": "2026-01-29T15:00:00"
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 404 | VB001 | 단어장을 찾을 수 없음 |
| 409 | VB003 | 수정 후 이름 중복 |
| 403 | AU004 | 접근 권한 없음 |
| 400 | CO005 | 기본 단어장 이름 변경 시도 |

---

#### 6.4.4 Delete Vocabulary Book

단어장과 포함된 모든 단어를 삭제합니다.

```
DELETE /api/vocabulary-books/{bookId}
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| bookId | Long | ✅ | 단어장 ID |

**Success Response**: `204 No Content`

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 404 | VB001 | 단어장을 찾을 수 없음 |
| 400 | VB002 | 기본 단어장 삭제 시도 |
| 403 | AU004 | 접근 권한 없음 |

---

#### 6.4.5 Add Entries to Vocabulary Book

아티클에서 추출된 단어를 단어장에 저장합니다.

```
POST /api/vocabulary-books/{bookId}/entries
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| bookId | Long | ✅ | 단어장 ID |

**Request Body - Selective Save**

```json
{
  "articleId": 1,
  "vocabularyIds": [101, 103, 105]
}
```

**Request Body - Save All**

```json
{
  "articleId": 1,
  "saveAll": true
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| articleId | Long | ✅ | 출처 아티클 ID |
| vocabularyIds | List\<Long\> | ❌ | 저장할 ArticleVocabulary ID 목록 |
| saveAll | boolean | ❌ | 전체 저장 여부 (default: false) |

**Success Response** `201 Created`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "savedCount": 3,
    "duplicateCount": 0,
    "bookId": 2,
    "bookName": "TOEIC 필수 단어"
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 404 | VB001 | 단어장을 찾을 수 없음 |
| 404 | AR003 | 아티클을 찾을 수 없음 |
| 403 | AU004 | 접근 권한 없음 |
| 400 | CO002 | vocabularyIds와 saveAll 모두 없음 |
| 400 | CO002 | 단어 추출 미완료 아티클 |

---

#### 6.4.6 Get Vocabulary Book Entries

단어장의 단어 목록을 조회합니다.

```
GET /api/vocabulary-books/{bookId}/entries
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| bookId | Long | ✅ | 단어장 ID |

**Query Parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | 페이지 번호 (0-based) |
| size | int | 20 | 페이지 크기 (max: 100) |
| sort | String | createdAt,desc | 정렬 기준 |
| keyword | String | null | 단어 검색 키워드 |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "content": [
      {
        "id": 201,
        "word": "unprecedented",
        "definition": "never done or known before",
        "contextSentence": "The country faced unprecedented challenges.",
        "sourceArticle": {
          "id": 1,
          "title": "Example News Article"
        },
        "createdAt": "2026-01-29T15:30:00"
      }
    ],
    "totalElements": 45,
    "totalPages": 3,
    "currentPage": 0,
    "size": 20
  }
}
```

---

#### 6.4.7 Delete Entry from Vocabulary Book

단어장에서 특정 단어를 삭제합니다.

```
DELETE /api/vocabulary-books/{bookId}/entries/{entryId}
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| bookId | Long | ✅ | 단어장 ID |
| entryId | Long | ✅ | 단어 항목 ID |

**Success Response**: `204 No Content`

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 404 | VB001 | 단어장을 찾을 수 없음 |
| 404 | CO003 | 단어 항목을 찾을 수 없음 |
| 403 | AU004 | 접근 권한 없음 |

---

#### 6.4.8 Move Entries Between Vocabulary Books

단어를 다른 단어장으로 이동합니다.

```
POST /api/vocabulary-books/{sourceBookId}/entries/move
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| sourceBookId | Long | ✅ | 출발 단어장 ID |

**Request Body**

```json
{
  "targetBookId": 3,
  "entryIds": [201, 202, 203]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| targetBookId | Long | ✅ | 대상 단어장 ID |
| entryIds | List\<Long\> | ✅ | 이동할 단어 항목 ID 목록 |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "movedCount": 3,
    "duplicateCount": 0,
    "sourceBookId": 2,
    "targetBookId": 3
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 404 | VB001 | 단어장을 찾을 수 없음 |
| 403 | AU004 | 접근 권한 없음 |
| 400 | CO002 | 출발/대상 단어장 동일 |
| 400 | CO002 | entryIds가 비어있음 |

---

### 6.5 Usage APIs

#### 6.5.1 Get Daily Usage

현재 사용자의 오늘 사용량을 조회합니다.

```
GET /api/usage/daily
```

**Authentication**: Bearer Token Required

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",  
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "used": 3,
    "limit": 5,
    "remaining": 2,
    "isUnlimited": false,
    "resetAt": "2026-01-30T00:00:00+09:00"
  }
}
```

**Response Fields**

| Field | Type | Description |
|-------|------|-------------|
| used | int | 오늘 사용한 횟수 |
| limit | int | 일일 한도 (ADMIN: -1) |
| remaining | int | 남은 횟수 (ADMIN: -1) |
| isUnlimited | boolean | 무제한 여부 (ADMIN: true) |
| resetAt | String | 카운터 리셋 시각 (ISO 8601) |

---

### 6.6 Article Recommendation APIs

#### 6.6.1 Set User CEFR Level

사용자의 영어 수준(CEFR 레벨)을 설정합니다.

```
PATCH /api/users/me/cefr-level
```

**Authentication**: Bearer Token Required

**Request Body**

```json
{
  "cefrLevel": "B1"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| cefrLevel | String | ✅ | One of: A1, A2, B1, B2, C1, C2 |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "cefrLevel": "B1",
    "updatedAt": "2026-01-29T10:00:00Z"
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 400 | CO005 | 유효하지 않은 CEFR 레벨 |

---

#### 6.6.2 Get Article Difficulty

특정 아티클의 예상 난이도(CEFR 레벨)를 조회합니다.

```
GET /api/articles/{articleId}/difficulty
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| articleId | Long | ✅ | 아티클 ID |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "articleId": 1,
    "cefrLevel": "B2",
    "averageWordDifficulty": 3.5,
    "complexSentenceRatio": 0.25,
    "analyzedAt": "2026-01-29T09:00:00Z"
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 404 | AR003 | 아티클을 찾을 수 없음 |
| 403 | AU004 | 접근 권한 없음 |

---

#### 6.6.3 Get Recommended Articles

사용자의 CEFR 레벨에 맞는 아티클을 추천합니다.

```
GET /api/articles/recommendations
```

**Authentication**: Bearer Token Required

**Query Parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | 페이지 번호 (0-based) |
| size | int | 10 | 페이지 크기 (max: 50) |
| levelRange | int | 1 | 허용 레벨 범위 (±1이면 B1 사용자에게 A2~B2 추천) |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Example News Article",
        "originalUrl": "https://www.bbc.com/news/example",
        "sourceDomain": "bbc.com",
        "cefrLevel": "B1",
        "vocabularyCount": 10,
        "createdAt": "2026-01-29T14:30:00"
      }
    ],
    "totalElements": 25,
    "totalPages": 3,
    "currentPage": 0,
    "size": 10
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 400 | CO002 | 사용자 CEFR 레벨 미설정 |

---

### 6.7 Learning & Review APIs

#### 6.7.1 Update Entry Learning Status

단어 항목의 학습 상태를 업데이트합니다.

```
PATCH /api/vocabulary-books/{bookId}/entries/{entryId}/status
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| bookId | Long | ✅ | 단어장 ID |
| entryId | Long | ✅ | 단어 항목 ID |

**Request Body**

```json
{
  "learningStatus": "LEARNING"
}
```

| Field | Type | Required | Values |
|-------|------|----------|--------|
| learningStatus | String | ✅ | NEW, LEARNING, REVIEWING, MASTERED |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "entryId": 201,
    "learningStatus": "LEARNING",
    "updatedAt": "2026-01-29T16:00:00Z"
  }
}
```

---

#### 6.7.2 Get Review Words

복습이 필요한 단어 목록을 조회합니다.

```
GET /api/learning/review-words
```

**Authentication**: Bearer Token Required

**Query Parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| limit | int | 20 | 조회 개수 (max: 50) |
| bookId | Long | - | 특정 단어장으로 필터링 (optional) |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "reviewWords": [
      {
        "entryId": 101,
        "word": "ubiquitous",
        "definition": "어디에나 있는",
        "contextSentence": "Smartphones have become ubiquitous in modern society.",
        "reviewCount": 3,
        "nextReviewAt": "2026-01-29T14:00:00Z",
        "bookName": "TOEIC 필수"
      }
    ],
    "totalCount": 15
  }
}
```

---

#### 6.7.3 Generate Quiz

단어장의 단어를 기반으로 퀴즈를 생성합니다.

```
POST /api/quiz/generate
```

**Authentication**: Bearer Token Required

**Request Body**

```json
{
  "bookId": 1,
  "quizType": "MULTIPLE_CHOICE",
  "count": 10
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| bookId | Long | ✅ | 대상 단어장 ID |
| quizType | String | ✅ | MULTIPLE_CHOICE, FILL_IN_BLANK, MEANING_MATCH |
| count | int | ❌ | 문제 개수 (default: 10, max: 30) |

**Success Response** `201 Created`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "quizSessionId": "uuid-session-id",
    "questions": [
      {
        "questionId": 1,
        "questionType": "MULTIPLE_CHOICE",
        "questionText": "'ubiquitous'의 의미는?",
        "options": ["희귀한", "어디에나 있는", "관대한", "즉각적인"],
        "entryId": 101
      }
    ],
    "totalQuestions": 10
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 404 | VB001 | 단어장을 찾을 수 없음 |
| 400 | CO002 | 단어장에 단어가 4개 미만 |

---

#### 6.7.4 Submit Quiz

퀴즈 응답을 제출하고 결과를 확인합니다.

```
POST /api/quiz/{sessionId}/submit
```

**Authentication**: Bearer Token Required

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| sessionId | String | ✅ | 퀴즈 세션 ID |

**Request Body**

```json
{
  "answers": [
    { "questionId": 1, "selectedIndex": 1 },
    { "questionId": 2, "selectedIndex": 0 }
  ]
}
```

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "correctCount": 8,
    "totalCount": 10,
    "accuracy": 0.8,
    "results": [
      { "questionId": 1, "isCorrect": true, "correctAnswer": 1 },
      { "questionId": 2, "isCorrect": false, "correctAnswer": 2, "userAnswer": 0 }
    ],
    "updatedSchedules": [
      { "entryId": 102, "nextReviewAt": "2026-02-01T10:00:00Z" }
    ]
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 404 | CO003 | 퀴즈 세션을 찾을 수 없음 |
| 400 | CO002 | 퀴즈 세션이 만료됨 (30분) |

---

#### 6.7.5 Get Learning Stats

학습 통계를 조회합니다.

```
GET /api/learning/stats
```

**Authentication**: Bearer Token Required

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "totalWords": 150,
    "masteredWords": 45,
    "learningWords": 80,
    "newWords": 25,
    "averageAccuracy": 0.82,
    "totalQuizzesTaken": 23,
    "studyStreak": 7
  }
}
```

---

### 6.8 Notification APIs

#### 6.8.1 Get Notification Settings

복습 알림 설정을 조회합니다.

```
GET /api/notifications/settings
```

**Authentication**: Bearer Token Required

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "emailEnabled": true,
    "kakaoEnabled": false,
    "preferredTime": "09:00",
    "timezone": "Asia/Seoul",
    "minWordsForNotification": 3
  }
}
```

---

#### 6.8.2 Update Notification Settings

복습 알림 설정을 수정합니다.

```
PATCH /api/notifications/settings
```

**Authentication**: Bearer Token Required

**Request Body**

```json
{
  "emailEnabled": true,
  "kakaoEnabled": false,
  "preferredTime": "09:00",
  "timezone": "Asia/Seoul"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| emailEnabled | boolean | ❌ | 이메일 알림 활성화 |
| kakaoEnabled | boolean | ❌ | 카카오톡 알림 활성화 |
| preferredTime | String | ❌ | 선호 알림 시간 (HH:mm) |
| timezone | String | ❌ | 타임존 (default: Asia/Seoul) |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "emailEnabled": true,
    "kakaoEnabled": false,
    "preferredTime": "09:00",
    "timezone": "Asia/Seoul",
    "updatedAt": "2026-01-29T17:00:00Z"
  }
}
```

**Error Responses**

| Status | Code | Condition |
|--------|------|-----------|
| 400 | CO002 | 카카오톡 알림은 카카오 로그인 사용자만 가능 |

---

#### 6.8.3 Send Test Notification

테스트 알림을 발송합니다.

```
POST /api/notifications/test
```

**Authentication**: Bearer Token Required

**Request Body**

```json
{
  "channel": "EMAIL"
}
```

| Field | Type | Required | Values |
|-------|------|----------|--------|
| channel | String | ✅ | EMAIL, KAKAO |

**Success Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "sent": true,
    "channel": "EMAIL",
    "sentAt": "2026-01-29T17:05:00Z"
  }
}
```

---

## 7. Data Models

### 7.1 User

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | Long | - | 사용자 고유 ID |
| email | String | ✅ | 이메일 주소 (Unique) |
| name | String | ✅ | 사용자 이름 |
| profileImage | String | ❌ | 프로필 이미지 URL |
| provider | Enum | ✅ | `GOOGLE`, `KAKAO`, `LOCAL` |
| providerId | String | ❌ | OAuth Provider ID |
| role | Enum | ✅ | `USER`, `ADMIN` |
| password | String | ❌ | BCrypt 해시 (LOCAL만) |
| cefrLevel | String | ❌ | CEFR 레벨 (A1~C2) |
| createdAt | DateTime | - | 생성 일시 |
| updatedAt | DateTime | - | 수정 일시 |

### 7.2 Article

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | Long | - | 아티클 고유 ID |
| userId | Long | ✅ | 소유자 ID (FK) |
| title | String | ✅ | 아티클 제목 |
| content | Text | ✅ | 본문 내용 (max: 50,000 chars) |
| originalUrl | String | ✅ | 원본 URL (max: 2,000 chars) |
| sourceDomain | String | ✅ | 출처 도메인 |
| isCompleted | boolean | ✅ | AI 추출 완료 여부 |
| cefrLevel | String | ❌ | 아티클 예상 CEFR 레벨 (A1~C2) |
| averageWordDifficulty | Double | ❌ | 평균 어휘 난이도 (1.0~5.0) |
| complexSentenceRatio | Double | ❌ | 복문 비율 (0.0~1.0) |
| createdAt | DateTime | - | 생성 일시 |
| updatedAt | DateTime | - | 수정 일시 |

### 7.3 ArticleVocabulary

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | Long | - | 단어 고유 ID |
| articleId | Long | ✅ | 아티클 ID (FK) |
| word | String | ✅ | 추천 단어 |
| definition | String | ✅ | 단어 정의 (영영 사전) |
| contextSentence | String | ❌ | 문맥 예문 |
| cefrLevel | String | ❌ | 단어 예상 CEFR 레벨 (A1~C2) |
| difficultyScore | Integer | ❌ | 난이도 점수 (1~5) |

### 7.4 VocabularyBook

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | Long | - | 단어장 고유 ID |
| userId | Long | ✅ | 소유자 ID (FK) |
| name | String | ✅ | 단어장 이름 (max: 50 chars) |
| description | String | ❌ | 단어장 설명 (max: 200 chars) |
| isDefault | boolean | ✅ | 기본 단어장 여부 |
| wordCount | int | ✅ | 저장된 단어 수 |
| createdAt | DateTime | - | 생성 일시 |
| updatedAt | DateTime | - | 수정 일시 |

### 7.5 VocabularyBookEntry

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | Long | - | 엔트리 고유 ID |
| vocabularyBookId | Long | ✅ | 단어장 ID (FK) |
| word | String | ✅ | 단어 (max: 100 chars) |
| definition | String | ✅ | 단어 정의 (max: 500 chars) |
| contextSentence | Text | ❌ | 문맥 예문 |
| sourceArticleId | Long | ❌ | 출처 아티클 ID (FK, nullable) |
| learningStatus | Enum | ✅ | 학습 상태 (NEW, LEARNING, REVIEWING, MASTERED) |
| reviewCount | Integer | ✅ | 복습 횟수 (default: 0) |
| reviewInterval | Integer | ✅ | 복습 간격 (일 단위, default: 1) |
| nextReviewAt | DateTime | ❌ | 다음 복습 예정일 |
| lastReviewedAt | DateTime | ❌ | 마지막 복습 일시 |
| correctCount | Integer | ✅ | 정답 횟수 (default: 0) |
| incorrectCount | Integer | ✅ | 오답 횟수 (default: 0) |
| createdAt | DateTime | - | 생성 일시 |
| updatedAt | DateTime | - | 수정 일시 |

---

## Appendix

### A. API Summary Table

| Category | Method | Endpoint | Auth | Rate Limited |
|----------|--------|----------|------|--------------|
| **Auth** | POST | /api/auth/signup | ❌ | ❌ |
| | POST | /api/auth/login | ❌ | ❌ |
| | GET | /oauth2/authorize/{provider} | ❌ | ❌ |
| | POST | /api/auth/refresh | ❌ | ❌ |
| | POST | /api/auth/logout | ✅ | ❌ |
| **User** | GET | /api/users/me | ✅ | ❌ |
| | PATCH | /api/users/me | ✅ | ❌ |
| | PATCH | /api/users/me/cefr-level | ✅ | ❌ |
| **Article** | POST | /api/articles/crawl | ✅ | ✅ |
| | GET | /api/articles | ✅ | ❌ |
| | GET | /api/articles/{id} | ✅ | ❌ |
| | DELETE | /api/articles/{id} | ✅ | ❌ |
| | POST | /api/articles/{id}/re-extract | ✅ | ✅ |
| | GET | /api/articles/{id}/extraction-status | ✅ | ❌ |
| | GET | /api/articles/{id}/difficulty | ✅ | ❌ |
| | GET | /api/articles/recommendations | ✅ | ❌ |
| **VocabularyBook** | POST | /api/vocabulary-books | ✅ | ❌ |
| | GET | /api/vocabulary-books | ✅ | ❌ |
| | PATCH | /api/vocabulary-books/{id} | ✅ | ❌ |
| | DELETE | /api/vocabulary-books/{id} | ✅ | ❌ |
| | POST | /api/vocabulary-books/{id}/entries | ✅ | ❌ |
| | GET | /api/vocabulary-books/{id}/entries | ✅ | ❌ |
| | DELETE | /api/vocabulary-books/{bid}/entries/{eid} | ✅ | ❌ |
| | POST | /api/vocabulary-books/{id}/entries/move | ✅ | ❌ |
| | PATCH | /api/vocabulary-books/{bid}/entries/{eid}/status | ✅ | ❌ |
| **Learning** | GET | /api/learning/review-words | ✅ | ❌ |
| | POST | /api/quiz/generate | ✅ | ❌ |
| | POST | /api/quiz/{sid}/submit | ✅ | ❌ |
| | GET | /api/learning/stats | ✅ | ❌ |
| **Notification** | GET | /api/notifications/settings | ✅ | ❌ |
| | PATCH | /api/notifications/settings | ✅ | ❌ |
| | POST | /api/notifications/test | ✅ | ❌ |
| **Usage** | GET | /api/usage/daily | ✅ | ❌ |

### B. HTTP Status Codes

| Status | Description |
|--------|-------------|
| 200 | OK - 요청 성공 |
| 201 | Created - 리소스 생성 성공 |
| 204 | No Content - 요청 성공 (본문 없음) |
| 400 | Bad Request - 잘못된 요청 |
| 401 | Unauthorized - 인증 필요 |
| 403 | Forbidden - 권한 없음 |
| 404 | Not Found - 리소스 없음 |
| 409 | Conflict - 충돌 (중복 등) |
| 429 | Too Many Requests - Rate Limit 초과 |
| 500 | Internal Server Error - 서버 오류 |

### C. Changelog

| Version | Date | Description |
|---------|------|-------------|
| 1.1.0 | 2026-01-30 | Added CEFR-based article recommendations, learning/review APIs, and notification APIs |
| 1.0.0 | 2026-01-29 | Initial API Specification |

---

> **Document maintained by**: SMIM Backend Team  
> **Contact**: <backend@smim.io>
