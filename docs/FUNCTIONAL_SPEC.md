# SMIM Backend 기능 명세서 (Functional Specification Document)

> **문서 버전**: 1.0
> **작성일**: 2026-01-29
> **프로젝트**: SMIM Backend
> **기술 스택**: Java 17, Spring Boot 4.0.1, PostgreSQL, Redis, Google Gemini API

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [시스템 아키텍처](#2-시스템-아키텍처)
3. [엔티티 관계도](#3-엔티티-관계도)
4. [기능 명세](#4-기능-명세)
   - [4.1 인증 시스템 (Auth)](#41-인증-시스템-auth)
   - [4.2 사용자 관리 (User)](#42-사용자-관리-user)
   - [4.3 URL 크롤링 및 아티클 관리 (Article)](#43-url-크롤링-및-아티클-관리-article)
   - [4.4 AI 단어 추출 (Vocabulary Extraction)](#44-ai-단어-추출-vocabulary-extraction)
   - [4.5 단어장 관리 (Vocabulary Book)](#45-단어장-관리-vocabulary-book)
   - [4.6 일일 사용량 제한 (Rate Limiting)](#46-일일-사용량-제한-rate-limiting)
5. [신규 엔티티 설계](#5-신규-엔티티-설계)
6. [ErrorCode 확장](#6-errorcode-확장)
7. [권한 매트릭스](#7-권한-매트릭스)
8. [비기능 요구사항](#8-비기능-요구사항)

---

## 1. 프로젝트 개요

SMIM은 뉴스 기사 및 웹 글의 URL을 입력하면 본문을 크롤링하고, Google Gemini AI를 활용하여 영어 학습에 유용한 추천 단어와 문장을 추출하는 서비스입니다.

### 1.1 핵심 가치

- 영어 학습자가 실제 영문 기사를 통해 문맥 기반 어휘를 학습할 수 있도록 지원합니다.
- AI 기반 단어 추출로 학습 효율을 극대화합니다.
- 개인 맞춤형 단어장 관리를 통해 체계적인 어휘 학습을 가능하게 합니다.

### 1.2 현재 구현 현황

| 구분 | 상태 | 설명 |
|------|------|------|
| OAuth2 소셜 로그인 | 구현 완료 | Google, Kakao |
| JWT 인증 | 구현 완료 | Access Token + Refresh Token (Redis) |
| URL 크롤링 | 구현 완료 | Jsoup, SSRF 방지, 10초 타임아웃 |
| AI 단어 추출 | 구현 완료 | Gemini API, 비동기 처리 |
| 이메일 회원가입/로그인 | 미구현 | 신규 개발 필요 |
| 아티클 목록/상세/삭제 | 미구현 | 신규 개발 필요 |
| 단어장 관리 | 미구현 | 신규 도메인 |
| 일일 사용량 제한 | 미구현 | 신규 기능 |

---

## 2. 시스템 아키텍처

```
┌─────────────┐     ┌──────────────────────────────────────────────────┐
│   Client     │     │                 SMIM Backend                     │
│  (Frontend)  │────>│                                                  │
└─────────────┘     │  ┌────────────┐  ┌─────────────┐                │
                    │  │ Security   │  │ Rate Limit  │                │
                    │  │ Filter     │──│ Check       │                │
                    │  │ (JWT/OAuth)│  │ (Redis)     │                │
                    │  └────────────┘  └─────────────┘                │
                    │        │                                         │
                    │  ┌─────┴──────────────────────────────┐         │
                    │  │          REST API Layer             │         │
                    │  │  Auth │ User │ Article │ VocabBook  │         │
                    │  └─────┬──────────────────────────────┘         │
                    │        │                                         │
                    │  ┌─────┴──────────────────────────────┐         │
                    │  │         Service Layer               │         │
                    │  │  AuthService │ ArticleService │ ... │         │
                    │  └─────┬──────────────────────────────┘         │
                    │        │         │                               │
                    │  ┌─────┴───┐  ┌──┴──────────┐                   │
                    │  │   JPA   │  │  Async       │                  │
                    │  │  Repos  │  │  Executor    │                  │
                    │  └────┬────┘  └──────┬───────┘                  │
                    └───────┼──────────────┼──────────────────────────┘
                            │              │
                    ┌───────┴───┐   ┌──────┴──────┐   ┌──────────┐
                    │PostgreSQL │   │ Gemini API  │   │  Redis   │
                    │  (Data)   │   │  (AI)       │   │ (Cache/  │
                    │           │   │             │   │  Token)  │
                    └───────────┘   └─────────────┘   └──────────┘
```

---

## 3. 엔티티 관계도

```
┌──────────────────┐
│      User        │
│──────────────────│
│ id (PK)          │
│ email (UK)       │
│ name             │
│ profileImage     │
│ provider         │
│ providerId       │
│ role             │
│ password         │
│ createdAt        │
│ updatedAt        │
└──────┬───────────┘
       │ 1
       │
       ├────────────────────────────────┐
       │ N                              │ N
┌──────┴───────────┐          ┌────────┴──────────┐
│    Article       │          │  VocabularyBook    │
│──────────────────│          │───────────────────-│
│ id (PK)          │          │ id (PK)            │
│ user_id (FK)     │          │ user_id (FK)       │
│ title            │          │ name               │
│ content          │          │ description        │
│ originalUrl      │          │ isDefault          │
│ sourceDomain     │          │ wordCount          │
│ isCompleted      │          │ createdAt          │
│ createdAt        │          │ updatedAt          │
│ updatedAt        │          └────────┬───────────┘
└──────┬───────────┘                   │ 1
       │ 1                             │
       │                               │ N
       │ N                    ┌────────┴───────────────┐
┌──────┴───────────────┐     │  VocabularyBookEntry    │
│  ArticleVocabulary   │     │────────────────────────-│
│──────────────────────│     │ id (PK)                 │
│ id (PK)              │     │ vocabularyBook_id (FK)  │
│ article_id (FK)      │     │ word                    │
│ word                 │     │ definition              │
│ definition           │     │ contextSentence         │
│ contextSentence      │     │ sourceArticle_id (FK)   │
│                      │     │ createdAt               │
└──────────────────────┘     │ updatedAt               │
                             └─────────────────────────┘
```

**관계 요약:**

| 관계 | 설명 |
|------|------|
| User 1 : N Article | 사용자는 여러 아티클을 생성할 수 있습니다 |
| Article 1 : N ArticleVocabulary | 아티클은 AI가 추출한 여러 단어를 포함합니다 |
| User 1 : N VocabularyBook | 사용자는 여러 커스텀 단어장을 생성할 수 있습니다 |
| VocabularyBook 1 : N VocabularyBookEntry | 단어장은 여러 단어 항목을 포함합니다 |
| Article 1 : N VocabularyBookEntry | 단어 항목은 출처 아티클을 참조할 수 있습니다 (nullable) |

---

## 4. 기능 명세

---

### 4.1 인증 시스템 (Auth)

#### 4.1.1 이메일 회원가입

**기능 설명**
이메일과 비밀번호를 통한 일반 회원가입을 지원합니다. 가입 완료 시 JWT 토큰 쌍(Access Token, Refresh Token)을 즉시 발급합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `POST` |
| Endpoint | `/api/auth/signup` |
| Auth Required | No |
| Content-Type | `application/json` |

**입력 (Request Body)**

```json
{
  "email": "user@example.com",
  "password": "SecurePass1!",
  "name": "홍길동"
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|-----------|
| email | String | Y | 유효한 이메일 형식, 최대 255자 |
| password | String | Y | 최소 8자, 영문 + 숫자 + 특수문자 조합 |
| name | String | Y | 최소 2자, 최대 50자 |

**출력 (Response Body)**

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

**선행 조건**
- 없음 (비인증 API)

**비즈니스 규칙**
- `BR-AUTH-001`: 비밀번호는 BCrypt로 해시하여 저장합니다.
- `BR-AUTH-002`: `provider` 필드는 `LOCAL`로 설정합니다. (Provider enum에 `LOCAL` 추가 필요)
- `BR-AUTH-003`: `providerId` 필드는 `null`로 설정합니다.
- `BR-AUTH-004`: 기본 역할은 `USER`로 설정합니다.
- `BR-AUTH-005`: 회원가입 완료 시 기본 단어장("기본 단어장")을 자동 생성합니다.
- `BR-AUTH-006`: Refresh Token은 Redis에 저장합니다. (Key: `refresh_token:{userId}`)

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| US002 (DUPLICATE_EMAIL) | 이미 등록된 이메일인 경우 | 409 Conflict |
| CO005 (VALIDATION_FAILED) | 비밀번호 정책 미충족 | 400 Bad Request |
| CO005 (VALIDATION_FAILED) | 이메일 형식 오류 | 400 Bad Request |
| CO005 (VALIDATION_FAILED) | 이름 길이 제한 초과 | 400 Bad Request |

**관련 엔티티 변경 사항**
- `Provider` enum에 `LOCAL` 값 추가
- `User` 엔티티의 `password` 필드 활용 (기존 nullable, 변경 불필요)

---

#### 4.1.2 이메일 로그인

**기능 설명**
등록된 이메일과 비밀번호로 로그인하여 JWT 토큰 쌍을 발급받습니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `POST` |
| Endpoint | `/api/auth/login` |
| Auth Required | No |
| Content-Type | `application/json` |

**입력 (Request Body)**

```json
{
  "email": "user@example.com",
  "password": "SecurePass1!"
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|-----------|
| email | String | Y | 유효한 이메일 형식 |
| password | String | Y | 비어 있지 않을 것 |

**출력 (Response Body)**

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

**선행 조건**
- 이메일 회원가입이 완료된 사용자여야 합니다.

**비즈니스 규칙**
- `BR-AUTH-010`: BCrypt를 사용하여 입력 비밀번호와 저장된 해시를 비교합니다.
- `BR-AUTH-011`: 소셜 로그인 사용자(`provider != LOCAL`)는 이메일 로그인이 불가합니다.
- `BR-AUTH-012`: 로그인 성공 시 Refresh Token을 Redis에 저장/갱신합니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| AU001 (UNAUTHORIZED) | 이메일이 존재하지 않는 경우 | 401 Unauthorized |
| AU001 (UNAUTHORIZED) | 비밀번호가 일치하지 않는 경우 | 401 Unauthorized |
| AU001 (UNAUTHORIZED) | 소셜 로그인 전용 계정인 경우 | 401 Unauthorized |

> **보안 참고**: 이메일 미존재와 비밀번호 불일치를 구분하지 않고 동일한 에러 응답을 반환하여, 계정 열거(Account Enumeration) 공격을 방지합니다.

---

#### 4.1.3 소셜 로그인 (기존)

**기능 설명**
Google 및 Kakao OAuth2를 통한 소셜 로그인을 지원합니다. 기존 구현을 유지합니다.

**API 정의**

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/oauth2/authorize/{provider}` | OAuth2 인증 시작 (google, kakao) |
| GET | `/login/oauth2/code/{provider}` | OAuth2 콜백 처리 |

**비즈니스 규칙**
- `BR-AUTH-020`: 최초 소셜 로그인 시 `User` 엔티티를 자동 생성합니다.
- `BR-AUTH-021`: 최초 소셜 로그인 시 기본 단어장("기본 단어장")을 자동 생성합니다. (신규)
- `BR-AUTH-022`: 기존 소셜 로그인 사용자는 프로필 정보(name, profileImage)를 업데이트합니다.
- `BR-AUTH-023`: 소셜 로그인 성공 시 프론트엔드 URL로 리다이렉트하며, 쿼리 파라미터로 토큰을 전달합니다.

---

#### 4.1.4 토큰 갱신 (기존)

**기능 설명**
만료된 Access Token을 Refresh Token으로 갱신합니다. 기존 구현을 유지합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `POST` |
| Endpoint | `/api/auth/refresh` |
| Auth Required | No |

**입력 (Request Body)**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**출력 (Response Body)**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**비즈니스 규칙**
- `BR-AUTH-030`: Refresh Token Rotation 전략을 사용합니다. (갱신 시 새로운 Refresh Token 발급)
- `BR-AUTH-031`: Redis에 저장된 Refresh Token과 일치하는지 검증합니다.
- `BR-AUTH-032`: 갱신 후 이전 Refresh Token은 무효화됩니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| AU002 (INVALID_TOKEN) | Refresh Token 형식이 유효하지 않은 경우 | 401 Unauthorized |
| AU003 (EXPIRED_TOKEN) | Refresh Token이 만료된 경우 | 401 Unauthorized |
| AU002 (INVALID_TOKEN) | Redis에 저장된 토큰과 불일치하는 경우 | 401 Unauthorized |

---

#### 4.1.5 로그아웃 (기존)

**기능 설명**
Redis에 저장된 Refresh Token을 삭제하여 세션을 무효화합니다. 기존 구현을 유지합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `POST` |
| Endpoint | `/api/auth/logout` |
| Auth Required | Yes (JWT) |

**출력**: `204 No Content`

**비즈니스 규칙**
- `BR-AUTH-040`: Redis에서 `refresh_token:{userId}` 키를 삭제합니다.
- `BR-AUTH-041`: Access Token은 만료 시까지 유효하므로, 클라이언트 측에서 토큰을 폐기해야 합니다.

---

### 4.2 사용자 관리 (User)

#### 4.2.1 내 프로필 조회

**기능 설명**
현재 로그인한 사용자의 프로필 정보를 조회합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `GET` |
| Endpoint | `/api/users/me` |
| Auth Required | Yes (JWT) |

**출력 (Response Body)**

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

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.

**비즈니스 규칙**
- `BR-USER-001`: JWT에서 추출한 userId로 사용자를 조회합니다.
- `BR-USER-002`: `password` 필드는 응답에 포함하지 않습니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| AU001 (UNAUTHORIZED) | 유효하지 않은 토큰 | 401 Unauthorized |
| US001 (USER_NOT_FOUND) | 사용자가 삭제된 경우 | 404 Not Found |

---

#### 4.2.2 프로필 수정

**기능 설명**
현재 로그인한 사용자의 프로필 정보를 수정합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `PATCH` |
| Endpoint | `/api/users/me` |
| Auth Required | Yes (JWT) |
| Content-Type | `application/json` |

**입력 (Request Body)**

```json
{
  "name": "새이름",
  "profileImage": "https://example.com/new-profile.jpg"
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|-----------|
| name | String | N | 최소 2자, 최대 50자 |
| profileImage | String | N | 유효한 URL 형식, 최대 500자 |

**출력 (Response Body)**

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

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.

**비즈니스 규칙**
- `BR-USER-010`: 제공된 필드만 업데이트합니다. (Partial Update)
- `BR-USER-011`: `email`, `role`, `provider` 필드는 수정 불가합니다.
- `BR-USER-012`: 빈 문자열("")은 유효하지 않은 입력으로 처리합니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| CO005 (VALIDATION_FAILED) | 이름 길이 제한 초과 | 400 Bad Request |
| US001 (USER_NOT_FOUND) | 사용자가 존재하지 않는 경우 | 404 Not Found |

---

#### 4.2.3 역할 기반 권한 매트릭스

다음 표는 ADMIN과 USER 역할의 권한 차이를 정의합니다.

| 기능 | USER | ADMIN |
|------|------|-------|
| 아티클 크롤링 | 하루 5회 제한 | 무제한 |
| 아티클 조회/삭제 | 본인 것만 | 모든 사용자 것 |
| 단어 재추출 | 하루 5회에 포함 | 무제한 |
| 단어장 CRUD | 본인 것만 | 본인 것만 |
| 사용자 관리 | 불가 | 가능 (향후) |
| 프로필 조회/수정 | 본인 것만 | 본인 것만 |

---

### 4.3 URL 크롤링 및 아티클 관리 (Article)

#### 4.3.1 URL 크롤링 및 아티클 생성 (기존 확장)

**기능 설명**
외부 URL을 크롤링하여 아티클을 생성하고, 비동기로 AI 단어 추출을 시작합니다. 기존 구현에 일일 사용량 체크 로직을 추가합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `POST` |
| Endpoint | `/api/articles/crawl` |
| Auth Required | Yes (JWT) |
| Content-Type | `application/json` |

**입력 (Request Body)**

```json
{
  "url": "https://www.bbc.com/news/example-article"
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|-----------|
| url | String | Y | 유효한 HTTP/HTTPS URL, 최대 2000자 |

**출력 (Response Body)**

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

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.
- 일일 사용량이 한도 내에 있어야 합니다. (USER: 5회 / ADMIN: 무제한)

**비즈니스 규칙**
- `BR-ART-001`: 크롤링 전 일일 사용량을 확인합니다. (Redis 기반)
- `BR-ART-002`: ADMIN 역할은 일일 사용량 제한을 적용하지 않습니다.
- `BR-ART-003`: 크롤링 성공 시 일일 사용량 카운터를 증가시킵니다.
- `BR-ART-004`: SSRF(Server-Side Request Forgery) 방지를 위해 private IP 대역을 차단합니다. (기존)
- `BR-ART-005`: 크롤링 타임아웃은 10초입니다. (기존)
- `BR-ART-006`: 아티클 저장 후 비동기로 AI 단어 추출을 시작합니다. (기존)
- `BR-ART-007`: `isCompleted` 필드는 초기값 `false`이며, 단어 추출 완료 시 `true`로 변경됩니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| AR001 (INVALID_URL) | URL 형식이 유효하지 않은 경우 | 400 Bad Request |
| AR002 (CRAWLING_FAILED) | 크롤링 실패 (타임아웃, 접근 불가 등) | 400 Bad Request |
| RL001 (DAILY_LIMIT_EXCEEDED) | 일일 사용량 초과 | 429 Too Many Requests |
| AU001 (UNAUTHORIZED) | 인증되지 않은 요청 | 401 Unauthorized |

---

#### 4.3.2 내 아티클 목록 조회

**기능 설명**
현재 로그인한 사용자가 생성한 아티클 목록을 페이지네이션으로 조회합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `GET` |
| Endpoint | `/api/articles` |
| Auth Required | Yes (JWT) |

**입력 (Query Parameters)**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | int | N | 0 | 페이지 번호 (0-based) |
| size | int | N | 10 | 페이지 크기 (최대 50) |
| sort | String | N | createdAt,desc | 정렬 기준 |

**출력 (Response Body)**

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

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.

**비즈니스 규칙**
- `BR-ART-010`: 본인이 생성한 아티클만 조회합니다. (ADMIN도 동일)
- `BR-ART-011`: 목록 조회 시 `content` 본문은 포함하지 않습니다. (성능 최적화)
- `BR-ART-012`: `vocabularyCount`는 해당 아티클에 추출된 단어 수를 나타냅니다.
- `BR-ART-013`: 기본 정렬은 생성일시 내림차순입니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| CO005 (VALIDATION_FAILED) | size가 50을 초과한 경우 | 400 Bad Request |

---

#### 4.3.3 아티클 상세 조회

**기능 설명**
특정 아티클의 상세 정보와 AI가 추출한 단어 목록을 함께 조회합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `GET` |
| Endpoint | `/api/articles/{articleId}` |
| Auth Required | Yes (JWT) |

**입력 (Path Variable)**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| articleId | Long | Y | 아티클 ID |

**출력 (Response Body)**

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

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.
- 해당 아티클이 본인 소유이거나, 요청자가 ADMIN 역할이어야 합니다.

**비즈니스 규칙**
- `BR-ART-020`: 본인의 아티클만 조회 가능합니다. (ADMIN은 모든 아티클 조회 가능)
- `BR-ART-021`: `vocabularyList`는 아티클에 연결된 `ArticleVocabulary` 목록입니다.
- `BR-ART-022`: `isCompleted`가 `false`인 경우, AI 단어 추출이 아직 진행 중임을 의미합니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| AR003 (ARTICLE_NOT_FOUND) | 아티클이 존재하지 않는 경우 | 404 Not Found |
| AU004 (ACCESS_DENIED) | 본인 소유가 아닌 아티클에 접근한 경우 | 403 Forbidden |

---

#### 4.3.4 아티클 삭제

**기능 설명**
특정 아티클과 연관된 `ArticleVocabulary`를 함께 삭제합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `DELETE` |
| Endpoint | `/api/articles/{articleId}` |
| Auth Required | Yes (JWT) |

**입력 (Path Variable)**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| articleId | Long | Y | 아티클 ID |

**출력**: `204 No Content`

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.
- 해당 아티클이 본인 소유이거나, 요청자가 ADMIN 역할이어야 합니다.

**비즈니스 규칙**
- `BR-ART-030`: 본인의 아티클만 삭제 가능합니다. (ADMIN은 모든 아티클 삭제 가능)
- `BR-ART-031`: `ArticleVocabulary`는 Cascade 삭제됩니다. (기존 `CascadeType.ALL`, `orphanRemoval = true`)
- `BR-ART-032`: 해당 아티클을 출처로 참조하는 `VocabularyBookEntry`의 `sourceArticle`은 `null`로 설정됩니다. (참조 무결성 유지)
- `BR-ART-033`: 삭제된 아티클의 일일 사용량 카운터는 감소시키지 않습니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| AR003 (ARTICLE_NOT_FOUND) | 아티클이 존재하지 않는 경우 | 404 Not Found |
| AU004 (ACCESS_DENIED) | 본인 소유가 아닌 아티클을 삭제하려는 경우 | 403 Forbidden |

---

### 4.4 AI 단어 추출 (Vocabulary Extraction)

#### 4.4.1 비동기 단어 추출 (기존)

**기능 설명**
아티클 생성 시 비동기로 Google Gemini AI를 호출하여 영어 학습에 유용한 단어와 문장을 추출합니다. 기존 구현을 유지합니다.

**처리 흐름**

```
아티클 저장 ──> @Async 메서드 호출 ──> Gemini API 요청 ──> JSON 파싱
                                                              │
                                                              v
                                          ArticleVocabulary 엔티티 저장
                                                              │
                                                              v
                                           isCompleted = true 업데이트
```

**비즈니스 규칙**
- `BR-VOC-001`: 비동기 Thread Pool 설정: Core 2, Max 5, Queue 10 (기존)
- `BR-VOC-002`: 아티클당 최대 10개의 단어를 추출합니다. (기존)
- `BR-VOC-003`: 추출 실패 시 예외를 로그에 기록하고, 서비스는 계속 동작합니다. (기존)
- `BR-VOC-004`: 추출 완료 시 `Article.isCompleted`를 `true`로 변경합니다.

---

#### 4.4.2 단어 재추출

**기능 설명**
아티클의 기존 추출 단어를 삭제하고, AI 단어 추출을 다시 수행합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `POST` |
| Endpoint | `/api/articles/{articleId}/re-extract` |
| Auth Required | Yes (JWT) |

**입력 (Path Variable)**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| articleId | Long | Y | 아티클 ID |

**출력 (Response Body)**

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

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.
- 해당 아티클이 본인 소유여야 합니다.
- 일일 사용량이 한도 내에 있어야 합니다. (USER: 5회 / ADMIN: 무제한)

**비즈니스 규칙**
- `BR-VOC-010`: 재추출 요청은 일일 사용량에 포함됩니다.
- `BR-VOC-011`: 기존 `ArticleVocabulary` 레코드를 모두 삭제합니다. (`Article.updateVocabulary()` 활용)
- `BR-VOC-012`: `Article.isCompleted`를 `false`로 초기화합니다.
- `BR-VOC-013`: 비동기로 새로운 단어 추출을 시작합니다.
- `BR-VOC-014`: 이미 추출 진행 중인 경우(`isCompleted == false`), 재추출 요청을 거부합니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| AR003 (ARTICLE_NOT_FOUND) | 아티클이 존재하지 않는 경우 | 404 Not Found |
| AU004 (ACCESS_DENIED) | 본인 소유가 아닌 아티클 | 403 Forbidden |
| RL001 (DAILY_LIMIT_EXCEEDED) | 일일 사용량 초과 | 429 Too Many Requests |
| AR004 (EXTRACTION_IN_PROGRESS) | 이미 추출이 진행 중인 경우 | 409 Conflict |

**관련 엔티티 변경 사항**
- `ErrorCode` enum에 `AR004` 추가: `EXTRACTION_IN_PROGRESS("AR004", "단어 추출이 진행 중입니다.", HttpStatus.CONFLICT)`

---

#### 4.4.3 추출 상태 조회

**기능 설명**
아티클의 AI 단어 추출 진행 상태를 조회합니다. 클라이언트에서 Polling 방식으로 호출하여 추출 완료 여부를 확인합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `GET` |
| Endpoint | `/api/articles/{articleId}/extraction-status` |
| Auth Required | Yes (JWT) |

**출력 (Response Body)**

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

| status 값 | 설명 |
|-----------|------|
| `EXTRACTING` | AI 단어 추출 진행 중 (`isCompleted == false`, vocabularyList 비어있음) |
| `COMPLETED` | 추출 완료 (`isCompleted == true`) |
| `FAILED` | 추출 실패 (`isCompleted == false`, 일정 시간 경과) |

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.
- 해당 아티클이 본인 소유여야 합니다.

**비즈니스 규칙**
- `BR-VOC-020`: `isCompleted == true`이면 `COMPLETED` 상태를 반환합니다.
- `BR-VOC-021`: `isCompleted == false`이고 생성 후 5분 이내이면 `EXTRACTING` 상태를 반환합니다.
- `BR-VOC-022`: `isCompleted == false`이고 생성 후 5분 이상 경과하면 `FAILED` 상태를 반환합니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| AR003 (ARTICLE_NOT_FOUND) | 아티클이 존재하지 않는 경우 | 404 Not Found |
| AU004 (ACCESS_DENIED) | 본인 소유가 아닌 아티클 | 403 Forbidden |

---

### 4.5 단어장 관리 (Vocabulary Book)

> **신규 도메인**: `domain/vocabularybook/` 패키지에 구현합니다.

#### 4.5.1 커스텀 단어장 생성

**기능 설명**
사용자가 새로운 커스텀 단어장을 생성합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `POST` |
| Endpoint | `/api/vocabulary-books` |
| Auth Required | Yes (JWT) |
| Content-Type | `application/json` |

**입력 (Request Body)**

```json
{
  "name": "TOEIC 필수 단어",
  "description": "TOEIC 시험에 자주 출제되는 단어 모음"
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|-----------|
| name | String | Y | 최소 1자, 최대 50자 |
| description | String | N | 최대 200자 |

**출력 (Response Body)**

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

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.

**비즈니스 규칙**
- `BR-VB-001`: 사용자당 최대 20개의 단어장을 생성할 수 있습니다. (기본 단어장 포함)
- `BR-VB-002`: 동일 사용자 내에서 단어장 이름은 중복될 수 없습니다.
- `BR-VB-003`: 생성되는 단어장의 `isDefault`는 `false`입니다.
- `BR-VB-004`: 회원가입 시 자동 생성되는 "기본 단어장"만 `isDefault = true`입니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| VB003 (DUPLICATE_BOOK_NAME) | 동일 이름의 단어장이 이미 존재하는 경우 | 409 Conflict |
| VB004 (MAX_BOOKS_EXCEEDED) | 단어장 개수가 20개를 초과한 경우 | 400 Bad Request |
| CO005 (VALIDATION_FAILED) | 이름 길이 제한 초과 | 400 Bad Request |

---

#### 4.5.2 단어장 목록 조회

**기능 설명**
현재 로그인한 사용자의 모든 단어장 목록을 조회합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `GET` |
| Endpoint | `/api/vocabulary-books` |
| Auth Required | Yes (JWT) |

**출력 (Response Body)**

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

**비즈니스 규칙**
- `BR-VB-010`: 기본 단어장이 항상 첫 번째로 정렬됩니다.
- `BR-VB-011`: 나머지 단어장은 생성일시 내림차순으로 정렬됩니다.
- `BR-VB-012`: `wordCount`는 denormalized 필드로, 단어 추가/삭제 시 갱신됩니다.

---

#### 4.5.3 단어장 수정

**기능 설명**
단어장의 이름과 설명을 수정합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `PATCH` |
| Endpoint | `/api/vocabulary-books/{bookId}` |
| Auth Required | Yes (JWT) |
| Content-Type | `application/json` |

**입력 (Request Body)**

```json
{
  "name": "수정된 단어장 이름",
  "description": "수정된 설명"
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|-----------|
| name | String | N | 최소 1자, 최대 50자 |
| description | String | N | 최대 200자 |

**출력 (Response Body)**

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

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.
- 해당 단어장이 본인 소유여야 합니다.

**비즈니스 규칙**
- `BR-VB-020`: 기본 단어장의 이름은 수정할 수 없습니다. (설명은 수정 가능)
- `BR-VB-021`: 동일 사용자 내에서 수정된 이름이 기존 다른 단어장과 중복되면 안 됩니다.
- `BR-VB-022`: 제공된 필드만 업데이트합니다. (Partial Update)

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| VB001 (VOCABULARY_BOOK_NOT_FOUND) | 단어장이 존재하지 않는 경우 | 404 Not Found |
| VB003 (DUPLICATE_BOOK_NAME) | 수정 후 이름이 중복되는 경우 | 409 Conflict |
| AU004 (ACCESS_DENIED) | 본인 소유가 아닌 단어장 | 403 Forbidden |
| CO005 (VALIDATION_FAILED) | 기본 단어장의 이름 변경 시도 | 400 Bad Request |

---

#### 4.5.4 단어장 삭제

**기능 설명**
단어장과 포함된 모든 단어 항목을 삭제합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `DELETE` |
| Endpoint | `/api/vocabulary-books/{bookId}` |
| Auth Required | Yes (JWT) |

**출력**: `204 No Content`

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.
- 해당 단어장이 본인 소유여야 합니다.
- 기본 단어장이 아니어야 합니다.

**비즈니스 규칙**
- `BR-VB-030`: 기본 단어장(`isDefault == true`)은 삭제할 수 없습니다.
- `BR-VB-031`: 단어장 삭제 시 포함된 모든 `VocabularyBookEntry`가 Cascade 삭제됩니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| VB001 (VOCABULARY_BOOK_NOT_FOUND) | 단어장이 존재하지 않는 경우 | 404 Not Found |
| VB002 (DEFAULT_BOOK_CANNOT_BE_DELETED) | 기본 단어장 삭제 시도 | 400 Bad Request |
| AU004 (ACCESS_DENIED) | 본인 소유가 아닌 단어장 | 403 Forbidden |

---

#### 4.5.5 단어장에 단어 저장

**기능 설명**
아티클에서 AI가 추출한 단어를 단어장에 저장합니다. 선택 저장과 전체 저장을 모두 지원합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `POST` |
| Endpoint | `/api/vocabulary-books/{bookId}/entries` |
| Auth Required | Yes (JWT) |
| Content-Type | `application/json` |

**입력 (Request Body) - 선택 저장**

```json
{
  "articleId": 1,
  "vocabularyIds": [101, 103, 105]
}
```

**입력 (Request Body) - 전체 저장**

```json
{
  "articleId": 1,
  "saveAll": true
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| articleId | Long | Y | 출처 아티클 ID |
| vocabularyIds | List\<Long\> | N | 저장할 ArticleVocabulary ID 목록 (선택 저장) |
| saveAll | boolean | N | 전체 저장 여부 (기본값: false) |

**출력 (Response Body)**

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

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.
- 해당 단어장과 아티클이 모두 본인 소유여야 합니다.
- 아티클의 단어 추출이 완료된 상태여야 합니다. (`isCompleted == true`)

**비즈니스 규칙**
- `BR-VB-040`: `saveAll == true`인 경우, 해당 아티클의 모든 `ArticleVocabulary`를 단어장에 저장합니다.
- `BR-VB-041`: `vocabularyIds`가 지정된 경우, 해당 ID에 해당하는 `ArticleVocabulary`만 저장합니다.
- `BR-VB-042`: 이미 단어장에 동일한 `word`가 존재하는 경우, 해당 항목은 건너뛰고 `duplicateCount`에 반영합니다.
- `BR-VB-043`: `VocabularyBookEntry`의 `word`, `definition`, `contextSentence` 값은 `ArticleVocabulary`에서 복사합니다.
- `BR-VB-044`: `VocabularyBookEntry.sourceArticle`에 출처 아티클을 참조합니다.
- `BR-VB-045`: 저장 후 `VocabularyBook.wordCount`를 갱신합니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| VB001 (VOCABULARY_BOOK_NOT_FOUND) | 단어장이 존재하지 않는 경우 | 404 Not Found |
| AR003 (ARTICLE_NOT_FOUND) | 아티클이 존재하지 않는 경우 | 404 Not Found |
| AU004 (ACCESS_DENIED) | 본인 소유가 아닌 단어장 또는 아티클 | 403 Forbidden |
| CO002 (INVALID_REQUEST) | `vocabularyIds`와 `saveAll`이 모두 없는 경우 | 400 Bad Request |
| CO002 (INVALID_REQUEST) | 단어 추출이 완료되지 않은 아티클 | 400 Bad Request |

---

#### 4.5.6 단어장 단어 목록 조회

**기능 설명**
특정 단어장에 저장된 단어 항목 목록을 조회합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `GET` |
| Endpoint | `/api/vocabulary-books/{bookId}/entries` |
| Auth Required | Yes (JWT) |

**입력 (Query Parameters)**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | int | N | 0 | 페이지 번호 (0-based) |
| size | int | N | 20 | 페이지 크기 (최대 100) |
| sort | String | N | createdAt,desc | 정렬 기준 |
| keyword | String | N | null | 단어 검색 키워드 |

**출력 (Response Body)**

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

**비즈니스 규칙**
- `BR-VB-050`: 본인의 단어장만 조회 가능합니다.
- `BR-VB-051`: `keyword`가 지정된 경우, `word` 필드에 대해 부분 일치(LIKE) 검색을 수행합니다.
- `BR-VB-052`: `sourceArticle`이 삭제된 경우 `null`로 반환합니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| VB001 (VOCABULARY_BOOK_NOT_FOUND) | 단어장이 존재하지 않는 경우 | 404 Not Found |
| AU004 (ACCESS_DENIED) | 본인 소유가 아닌 단어장 | 403 Forbidden |

---

#### 4.5.7 단어장에서 단어 삭제

**기능 설명**
단어장에서 특정 단어 항목을 삭제합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `DELETE` |
| Endpoint | `/api/vocabulary-books/{bookId}/entries/{entryId}` |
| Auth Required | Yes (JWT) |

**출력**: `204 No Content`

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.
- 해당 단어장이 본인 소유여야 합니다.

**비즈니스 규칙**
- `BR-VB-060`: 삭제 후 `VocabularyBook.wordCount`를 감소시킵니다.
- `BR-VB-061`: 해당 `entryId`가 지정된 `bookId`에 속하는지 검증합니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| VB001 (VOCABULARY_BOOK_NOT_FOUND) | 단어장이 존재하지 않는 경우 | 404 Not Found |
| CO003 (RESOURCE_NOT_FOUND) | 단어 항목이 존재하지 않는 경우 | 404 Not Found |
| AU004 (ACCESS_DENIED) | 본인 소유가 아닌 단어장 | 403 Forbidden |

---

#### 4.5.8 단어장 간 단어 이동

**기능 설명**
단어 항목을 현재 단어장에서 다른 단어장으로 이동합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `POST` |
| Endpoint | `/api/vocabulary-books/{sourceBookId}/entries/move` |
| Auth Required | Yes (JWT) |
| Content-Type | `application/json` |

**입력 (Request Body)**

```json
{
  "targetBookId": 3,
  "entryIds": [201, 202, 203]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| targetBookId | Long | Y | 이동 대상 단어장 ID |
| entryIds | List\<Long\> | Y | 이동할 단어 항목 ID 목록 |

**출력 (Response Body)**

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

**선행 조건**
- 유효한 JWT Access Token이 필요합니다.
- 출발 단어장과 대상 단어장이 모두 본인 소유여야 합니다.

**비즈니스 규칙**
- `BR-VB-070`: 이동 시 대상 단어장에 동일한 `word`가 이미 존재하면 해당 항목은 건너뛰고 `duplicateCount`에 반영합니다.
- `BR-VB-071`: 이동 완료 후 출발 단어장과 대상 단어장의 `wordCount`를 각각 갱신합니다.
- `BR-VB-072`: 출발 단어장과 대상 단어장이 동일한 경우 에러를 반환합니다.
- `BR-VB-073`: `entryIds`에 포함된 모든 항목이 출발 단어장에 속하는지 검증합니다.

**예외 처리**

| ErrorCode | 조건 | HTTP Status |
|-----------|------|-------------|
| VB001 (VOCABULARY_BOOK_NOT_FOUND) | 출발 또는 대상 단어장이 존재하지 않는 경우 | 404 Not Found |
| AU004 (ACCESS_DENIED) | 본인 소유가 아닌 단어장 | 403 Forbidden |
| CO002 (INVALID_REQUEST) | 출발과 대상 단어장이 동일한 경우 | 400 Bad Request |
| CO002 (INVALID_REQUEST) | `entryIds`가 비어 있는 경우 | 400 Bad Request |

---

### 4.6 일일 사용량 제한 (Rate Limiting)

#### 4.6.1 Redis 기반 일일 카운터

**기능 설명**
일반 사용자의 아티클 크롤링 및 단어 재추출 요청 횟수를 Redis를 통해 관리합니다.

**설계 상세**

| 항목 | 값 |
|------|-----|
| Redis Key | `daily_limit:{userId}:{yyyy-MM-dd}` |
| Value | 요청 횟수 (Integer) |
| TTL | 다음 자정까지 남은 시간 (동적 계산) |
| 기본 한도 | USER: 5회 / ADMIN: 무제한 |

**처리 흐름**

```
요청 수신 ──> ADMIN 확인 ──(Y)──> 제한 없이 진행
                │
               (N)
                │
                v
           Redis GET ──> 한도 초과 여부 확인
                │                    │
              (미초과)             (초과)
                │                    │
                v                    v
           Redis INCR         429 에러 반환
           요청 진행
```

**비즈니스 규칙**
- `BR-RL-001`: Redis Key의 날짜는 서버 시간 기준 `yyyy-MM-dd` 형식입니다. (타임존: `Asia/Seoul`)
- `BR-RL-002`: TTL은 다음 자정(00:00:00)까지 남은 시간으로 설정하여, 자정에 자동 리셋됩니다.
- `BR-RL-003`: `ADMIN` 역할은 카운터 확인 없이 요청을 즉시 진행합니다.
- `BR-RL-004`: 크롤링과 단어 재추출은 동일한 카운터를 공유합니다.
- `BR-RL-005`: Redis INCR 연산을 사용하여 원자적(atomic)으로 카운터를 증가시킵니다.
- `BR-RL-006`: Redis 연결 실패 시 요청을 허용합니다. (Fail-Open 전략)

---

#### 4.6.2 사용량 조회 API

**기능 설명**
현재 로그인한 사용자의 오늘 사용량과 남은 횟수를 조회합니다.

**API 정의**

| 항목 | 값 |
|------|-----|
| Method | `GET` |
| Endpoint | `/api/usage/daily` |
| Auth Required | Yes (JWT) |

**출력 (Response Body)**

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

| 필드 | 설명 |
|------|------|
| used | 오늘 사용한 횟수 |
| limit | 일일 한도 (ADMIN: -1) |
| remaining | 남은 횟수 (ADMIN: -1) |
| isUnlimited | 무제한 여부 (ADMIN: true) |
| resetAt | 카운터 리셋 시각 (다음 자정) |

**비즈니스 규칙**
- `BR-RL-010`: ADMIN 사용자는 `isUnlimited: true`, `limit: -1`, `remaining: -1`을 반환합니다.
- `BR-RL-011`: Redis Key가 존재하지 않는 경우 `used: 0`으로 반환합니다.

---

#### 4.6.3 ADMIN 무제한 예외 처리

**비즈니스 규칙**
- `BR-RL-020`: Rate Limit 체크 로직에서 `User.role == ADMIN`인 경우 카운터 확인을 건너뜁니다.
- `BR-RL-021`: ADMIN 사용자도 사용량은 Redis에 기록합니다. (모니터링 목적, 선택적)
- `BR-RL-022`: Rate Limit 체크는 Service Layer 또는 AOP(@Annotation)로 구현합니다.

**권장 구현 방식**

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
}

@Aspect
@Component
public class RateLimitAspect {
    // AOP로 @RateLimited 어노테이션이 붙은 메서드에 일일 사용량 체크 적용
}
```

---

## 5. 신규 엔티티 설계

### 5.1 VocabularyBook (단어장)

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vocabulary_books",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_user_book_name",
           columnNames = {"user_id", "name"}
       ))
public class VocabularyBook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "word_count", nullable = false)
    private int wordCount;

    @OneToMany(mappedBy = "vocabularyBook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VocabularyBookEntry> entries = new ArrayList<>();

    @Builder
    public VocabularyBook(User user, String name, String description, boolean isDefault) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.isDefault = isDefault;
        this.wordCount = 0;
    }

    public void updateName(String name) {
        if (this.isDefault) {
            throw new IllegalStateException("기본 단어장의 이름은 변경할 수 없습니다.");
        }
        this.name = name;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void incrementWordCount() {
        this.wordCount++;
    }

    public void decrementWordCount() {
        if (this.wordCount > 0) {
            this.wordCount--;
        }
    }

    public void adjustWordCount(int delta) {
        this.wordCount = Math.max(0, this.wordCount + delta);
    }
}
```

### 5.2 VocabularyBookEntry (단어장 항목)

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vocabulary_book_entries",
       indexes = {
           @Index(name = "idx_entry_book_id", columnList = "vocabulary_book_id"),
           @Index(name = "idx_entry_word", columnList = "word")
       })
public class VocabularyBookEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_book_id", nullable = false)
    private VocabularyBook vocabularyBook;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(nullable = false, length = 500)
    private String definition;

    @Column(columnDefinition = "TEXT")
    private String contextSentence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_article_id")
    private Article sourceArticle;

    @Builder
    public VocabularyBookEntry(VocabularyBook vocabularyBook, String word,
                                String definition, String contextSentence,
                                Article sourceArticle) {
        this.vocabularyBook = vocabularyBook;
        this.word = word;
        this.definition = definition;
        this.contextSentence = contextSentence;
        this.sourceArticle = sourceArticle;
    }

    public void moveToBook(VocabularyBook targetBook) {
        this.vocabularyBook = targetBook;
    }
}
```

### 5.3 Provider enum 변경

```java
public enum Provider {
    GOOGLE,
    KAKAO,
    LOCAL    // 신규: 이메일 로그인 사용자
}
```

---

## 6. ErrorCode 확장

### 6.1 기존 ErrorCode에 추가할 항목

```java
// 아티클 에러 (AR: Article) - 추가
EXTRACTION_IN_PROGRESS("AR004", "단어 추출이 진행 중입니다.", HttpStatus.CONFLICT),
ARTICLE_ACCESS_DENIED("AR005", "해당 아티클에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

// 인증 에러 (AU: Auth) - 추가
INVALID_PASSWORD("AU006", "비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
SOCIAL_ACCOUNT_LOGIN_REQUIRED("AU007", "소셜 로그인으로 가입된 계정입니다.", HttpStatus.UNAUTHORIZED),

// 단어장 에러 (VB: VocabularyBook) - 신규
VOCABULARY_BOOK_NOT_FOUND("VB001", "단어장을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
DEFAULT_BOOK_CANNOT_BE_DELETED("VB002", "기본 단어장은 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
DUPLICATE_BOOK_NAME("VB003", "이미 존재하는 단어장 이름입니다.", HttpStatus.CONFLICT),
MAX_BOOKS_EXCEEDED("VB004", "단어장 개수가 최대 한도를 초과했습니다.", HttpStatus.BAD_REQUEST),
DUPLICATE_ENTRY("VB005", "이미 단어장에 존재하는 단어입니다.", HttpStatus.CONFLICT),

// 사용량 제한 에러 (RL: Rate Limit) - 신규
DAILY_LIMIT_EXCEEDED("RL001", "일일 사용 한도를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS),
```

### 6.2 ErrorCode Prefix 체계

| Prefix | 도메인 | 범위 |
|--------|--------|------|
| CO | Common (공통) | CO001 ~ CO099 |
| AU | Auth (인증) | AU001 ~ AU099 |
| US | User (사용자) | US001 ~ US099 |
| AR | Article (아티클) | AR001 ~ AR099 |
| VB | VocabularyBook (단어장) | VB001 ~ VB099 |
| RL | RateLimit (사용량 제한) | RL001 ~ RL099 |

---

## 7. 권한 매트릭스

### 7.1 API 엔드포인트별 인증/인가

| Method | Endpoint | Auth | Role | 설명 |
|--------|----------|------|------|------|
| POST | `/api/auth/signup` | - | - | 이메일 회원가입 |
| POST | `/api/auth/login` | - | - | 이메일 로그인 |
| GET | `/oauth2/authorize/{provider}` | - | - | 소셜 로그인 시작 |
| POST | `/api/auth/refresh` | - | - | 토큰 갱신 |
| POST | `/api/auth/logout` | JWT | ALL | 로그아웃 |
| GET | `/api/users/me` | JWT | ALL | 내 프로필 조회 |
| PATCH | `/api/users/me` | JWT | ALL | 프로필 수정 |
| POST | `/api/articles/crawl` | JWT | ALL | URL 크롤링 (Rate Limited) |
| GET | `/api/articles` | JWT | ALL | 아티클 목록 조회 |
| GET | `/api/articles/{id}` | JWT | ALL | 아티클 상세 조회 |
| DELETE | `/api/articles/{id}` | JWT | ALL | 아티클 삭제 |
| POST | `/api/articles/{id}/re-extract` | JWT | ALL | 단어 재추출 (Rate Limited) |
| GET | `/api/articles/{id}/extraction-status` | JWT | ALL | 추출 상태 조회 |
| POST | `/api/vocabulary-books` | JWT | ALL | 단어장 생성 |
| GET | `/api/vocabulary-books` | JWT | ALL | 단어장 목록 조회 |
| PATCH | `/api/vocabulary-books/{id}` | JWT | ALL | 단어장 수정 |
| DELETE | `/api/vocabulary-books/{id}` | JWT | ALL | 단어장 삭제 |
| POST | `/api/vocabulary-books/{id}/entries` | JWT | ALL | 단어 저장 |
| GET | `/api/vocabulary-books/{id}/entries` | JWT | ALL | 단어 목록 조회 |
| DELETE | `/api/vocabulary-books/{bid}/entries/{eid}` | JWT | ALL | 단어 삭제 |
| POST | `/api/vocabulary-books/{id}/entries/move` | JWT | ALL | 단어 이동 |
| GET | `/api/usage/daily` | JWT | ALL | 일일 사용량 조회 |

> **참고**: "ALL"은 USER + ADMIN 모두 접근 가능하나, 리소스 소유권 검증은 Service Layer에서 수행합니다.

---

## 8. 비기능 요구사항

### 8.1 성능

| 항목 | 목표 |
|------|------|
| API 응답 시간 (P95) | 200ms 이내 (크롤링 제외) |
| 크롤링 타임아웃 | 10초 |
| AI 단어 추출 | 비동기 처리, 30초 이내 완료 |
| 동시 접속자 | 100명 이상 |
| 비동기 Thread Pool | Core: 2, Max: 5, Queue: 10 |

### 8.2 보안

| 항목 | 상세 |
|------|------|
| 비밀번호 해싱 | BCrypt (strength: 10) |
| JWT 서명 | HMAC SHA-256 |
| SSRF 방지 | Private IP 대역 차단 |
| CORS | 프론트엔드 도메인만 허용 |
| 세션 관리 | Stateless (JWT 기반) |
| Rate Limiting | Redis 기반 일일 카운터 |

### 8.3 가용성

| 항목 | 상세 |
|------|------|
| Redis 장애 대응 | Fail-Open (Rate Limit 통과 허용) |
| AI 추출 실패 | 서비스 지속 동작 (에러 로그 기록) |
| DB 트랜잭션 | Spring @Transactional 관리 |

### 8.4 데이터 정합성

| 항목 | 전략 |
|------|------|
| wordCount 동기화 | 단어 추가/삭제/이동 시 즉시 갱신 |
| 아티클 삭제 시 단어장 참조 | sourceArticle을 null로 설정 |
| ArticleVocabulary 삭제 | Cascade (orphanRemoval = true) |
| 중복 단어 방지 | 단어장 내 word 필드 기준 중복 체크 |

---

> **문서 이력**
>
> | 버전 | 날짜 | 작성자 | 변경 내용 |
> |------|------|--------|-----------|
> | 1.0 | 2026-01-29 | - | 초기 작성 |
