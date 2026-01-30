# SMIM Database Schema

> **Version**: 1.0
> **Last Updated**: 2026-01-30
> **Database**: PostgreSQL 14+
> **Naming Convention**: snake_case

---

## 목차

1. [전체 ERD](#1-전체-erd)
2. [테이블 상세 스키마](#2-테이블-상세-스키마)
3. [인덱스 전략](#3-인덱스-전략)
4. [외래 키 제약조건](#4-외래-키-제약조건)
5. [비즈니스 규칙과 DB 제약사항](#5-비즈니스-규칙과-db-제약사항)
6. [마이그레이션 가이드](#6-마이그레이션-가이드)

---

## 1. 전체 ERD

```
┌──────────────────┐
│      users       │
│──────────────────│
│ id (PK)          │
│ email (UK)       │
│ name             │
│ profile_image    │
│ provider         │
│ provider_id      │
│ role             │
│ password         │
│ created_at       │
│ updated_at       │
└──────┬───────────┘
       │ 1
       │
       ├────────────────────────────────┐
       │ N                              │ N
┌──────┴───────────┐          ┌────────┴──────────┐
│    articles      │          │ vocabulary_books   │
│──────────────────│          │────────────────────│
│ id (PK)          │          │ id (PK)            │
│ user_id (FK)     │          │ user_id (FK)       │
│ title            │          │ name               │
│ content          │          │ description        │
│ original_url     │          │ is_default         │
│ source_domain    │          │ word_count         │
│ is_completed     │          │ created_at         │
│ created_at       │          │ updated_at         │
│ updated_at       │          └────────┬───────────┘
└──────┬───────────┘                   │ 1
       │ 1                             │
       │                               │ N
       │ N                    ┌────────┴───────────────────┐
┌──────┴──────────────────┐  │  vocabulary_entries        │
│ article_vocabularies    │  │────────────────────────────│
│─────────────────────────│  │ id (PK)                    │
│ id (PK)                 │  │ vocabulary_book_id (FK)    │
│ article_id (FK)         │  │ word                       │
│ word                    │  │ definition                 │
│ definition              │  │ context_sentence           │
│ context_sentence        │  │ source_article_id (FK, N)  │
│ created_at              │  │ created_at                 │
│ updated_at              │  │ updated_at                 │
└─────────────────────────┘  └────────────────────────────┘
```

**관계 요약:**

| 관계 | 설명 | Cascade |
|------|------|---------|
| users 1:N articles | 사용자는 여러 아티클을 생성 | DELETE CASCADE |
| articles 1:N article_vocabularies | 아티클은 AI 추출 단어를 포함 | DELETE CASCADE |
| users 1:N vocabulary_books | 사용자는 여러 단어장을 생성 | DELETE CASCADE |
| vocabulary_books 1:N vocabulary_entries | 단어장은 여러 단어를 포함 | DELETE CASCADE |
| articles 1:N vocabulary_entries | 단어는 출처 아티클을 참조 (nullable) | SET NULL |

---

## 2. 테이블 상세 스키마

### 2.1 users (사용자)

**용도**: 서비스 사용자 정보 관리

| 컬럼명 | 타입 | Null | 기본값 | 제약조건 | 설명 |
|--------|------|:----:|--------|----------|------|
| `id` | BIGSERIAL | N | AUTO | PK | 사용자 ID |
| `email` | VARCHAR(255) | N | - | UNIQUE | 이메일 (중복 불가) |
| `name` | VARCHAR(50) | N | - | - | 사용자 이름 |
| `profile_image` | VARCHAR(500) | Y | NULL | - | 프로필 이미지 URL |
| `provider` | VARCHAR(20) | N | - | CHECK | 인증 제공자 (GOOGLE, KAKAO, LOCAL) |
| `provider_id` | VARCHAR(100) | Y | NULL | - | 소셜 로그인 제공자 ID |
| `role` | VARCHAR(20) | N | 'USER' | CHECK | 역할 (USER, ADMIN) |
| `password` | VARCHAR(255) | Y | NULL | - | BCrypt 해시 비밀번호 (LOCAL만 사용) |
| `created_at` | TIMESTAMP | N | NOW() | - | 생성일시 |
| `updated_at` | TIMESTAMP | N | NOW() | - | 수정일시 |

**제약조건:**
```sql
CHECK (provider IN ('GOOGLE', 'KAKAO', 'LOCAL'))
CHECK (role IN ('USER', 'ADMIN'))
CHECK ((provider = 'LOCAL' AND password IS NOT NULL) OR (provider != 'LOCAL'))
UNIQUE (email)
```

**비즈니스 규칙:**
- `BR-AUTH-001`: 비밀번호는 BCrypt로 해시하여 저장
- `BR-AUTH-002`: LOCAL provider는 password 필수
- `BR-AUTH-004`: 기본 역할은 USER
- `BR-AUTH-005`: 회원가입 시 기본 단어장 자동 생성

---

### 2.2 articles (아티클)

**용도**: 크롤링된 뉴스/웹 글 저장

| 컬럼명 | 타입 | Null | 기본값 | 제약조건 | 설명 |
|--------|------|:----:|--------|----------|------|
| `id` | BIGSERIAL | N | AUTO | PK | 아티클 ID |
| `user_id` | BIGINT | N | - | FK → users(id) | 소유자 ID |
| `title` | VARCHAR(500) | N | - | - | 아티클 제목 |
| `content` | TEXT | N | - | - | 본문 (최대 50,000자) |
| `original_url` | VARCHAR(2000) | N | - | - | 원본 URL |
| `source_domain` | VARCHAR(255) | N | - | - | 출처 도메인 (예: bbc.com) |
| `is_completed` | BOOLEAN | N | FALSE | - | 단어 추출 완료 여부 |
| `created_at` | TIMESTAMP | N | NOW() | - | 생성일시 |
| `updated_at` | TIMESTAMP | N | NOW() | - | 수정일시 |

**제약조건:**
```sql
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
CHECK (LENGTH(content) <= 50000)
```

**비즈니스 규칙:**
- `BR-ART-007`: `is_completed`는 초기값 FALSE, 추출 완료 시 TRUE
- `BR-ART-031`: Article 삭제 시 article_vocabularies도 Cascade 삭제

---

### 2.3 article_vocabularies (AI 추출 단어)

**용도**: AI가 아티클에서 추출한 단어 저장 (읽기 전용)

| 컬럼명 | 타입 | Null | 기본값 | 제약조건 | 설명 |
|--------|------|:----:|--------|----------|------|
| `id` | BIGSERIAL | N | AUTO | PK | 추출 단어 ID |
| `article_id` | BIGINT | N | - | FK → articles(id) | 소속 아티클 ID |
| `word` | VARCHAR(100) | N | - | - | 추출된 단어 |
| `definition` | VARCHAR(500) | N | - | - | 단어 정의 (영영 사전) |
| `context_sentence` | TEXT | Y | NULL | - | 문맥 예문 |
| `created_at` | TIMESTAMP | N | NOW() | - | 생성일시 |
| `updated_at` | TIMESTAMP | N | NOW() | - | 수정일시 |

**제약조건:**
```sql
FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
```

**비즈니스 규칙:**
- `BR-VOC-002`: 아티클당 최대 10개 단어 추출
- `BR-VOC-011`: 재추출 시 기존 단어 전체 삭제 후 재생성

**확장 필드 (향후):**
- `cefr_level` VARCHAR(2): 단어 난이도 (A1~C2)
- `difficulty_score` INT: 난이도 점수 (1~5)

---

### 2.4 vocabulary_books (단어장)

**용도**: 사용자 커스텀 단어장 관리

| 컬럼명 | 타입 | Null | 기본값 | 제약조건 | 설명 |
|--------|------|:----:|--------|----------|------|
| `id` | BIGSERIAL | N | AUTO | PK | 단어장 ID |
| `user_id` | BIGINT | N | - | FK → users(id) | 소유자 ID |
| `name` | VARCHAR(50) | N | - | - | 단어장 이름 |
| `description` | VARCHAR(200) | Y | NULL | - | 단어장 설명 |
| `is_default` | BOOLEAN | N | FALSE | - | 기본 단어장 여부 |
| `word_count` | INT | N | 0 | CHECK | 단어 개수 (denormalized) |
| `created_at` | TIMESTAMP | N | NOW() | - | 생성일시 |
| `updated_at` | TIMESTAMP | N | NOW() | - | 수정일시 |

**제약조건:**
```sql
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
UNIQUE (user_id, name)
CHECK (word_count >= 0)
```

**비즈니스 규칙:**
- `BR-VB-001`: 사용자당 최대 20개 단어장 (기본 단어장 포함)
- `BR-VB-002`: 동일 사용자 내 단어장 이름 중복 불가
- `BR-VB-004`: 회원가입 시 "기본 단어장" 자동 생성 (is_default=TRUE)
- `BR-VB-030`: 기본 단어장은 삭제 불가

---

### 2.5 vocabulary_entries (단어장 항목)

**용도**: 사용자가 단어장에 저장한 개별 단어

| 컬럼명 | 타입 | Null | 기본값 | 제약조건 | 설명 |
|--------|------|:----:|--------|----------|------|
| `id` | BIGSERIAL | N | AUTO | PK | 단어 항목 ID |
| `vocabulary_book_id` | BIGINT | N | - | FK → vocabulary_books(id) | 소속 단어장 ID |
| `word` | VARCHAR(100) | N | - | - | 단어 |
| `definition` | VARCHAR(500) | N | - | - | 단어 정의 |
| `context_sentence` | TEXT | Y | NULL | - | 문맥 예문 |
| `source_article_id` | BIGINT | Y | NULL | FK → articles(id) | 출처 아티클 ID (nullable) |
| `created_at` | TIMESTAMP | N | NOW() | - | 생성일시 |
| `updated_at` | TIMESTAMP | N | NOW() | - | 수정일시 |

**제약조건:**
```sql
FOREIGN KEY (vocabulary_book_id) REFERENCES vocabulary_books(id) ON DELETE CASCADE
FOREIGN KEY (source_article_id) REFERENCES articles(id) ON DELETE SET NULL
UNIQUE (vocabulary_book_id, word)
```

**비즈니스 규칙:**
- `BR-VB-042`: 동일 단어장 내 동일 단어 중복 불가
- `BR-VB-043`: ArticleVocabulary에서 복사 시 word, definition, context_sentence 값 복사
- `BR-VB-044`: source_article_id에 출처 아티클 참조
- `BR-ART-032`: 출처 아티클 삭제 시 source_article_id는 NULL로 설정

**확장 필드 (학습 기능):**
- `learning_status` VARCHAR(20): 학습 상태 (NEW, LEARNING, REVIEWING, MASTERED)
- `review_count` INT: 복습 횟수
- `correct_count` INT: 정답 횟수
- `next_review_at` TIMESTAMP: 다음 복습 예정일
- `review_interval` INT: 복습 간격 (일)

---

## 3. 인덱스 전략

### 3.1 Primary Key 인덱스 (자동 생성)

```sql
-- 모든 테이블의 id 컬럼에 자동 생성
CREATE UNIQUE INDEX users_pkey ON users(id);
CREATE UNIQUE INDEX articles_pkey ON articles(id);
CREATE UNIQUE INDEX article_vocabularies_pkey ON article_vocabularies(id);
CREATE UNIQUE INDEX vocabulary_books_pkey ON vocabulary_books(id);
CREATE UNIQUE INDEX vocabulary_entries_pkey ON vocabulary_entries(id);
```

### 3.2 Unique 제약조건 인덱스 (자동 생성)

```sql
-- users 테이블
CREATE UNIQUE INDEX users_email_key ON users(email);

-- vocabulary_books 테이블
CREATE UNIQUE INDEX uk_user_book_name ON vocabulary_books(user_id, name);

-- vocabulary_entries 테이블
CREATE UNIQUE INDEX uk_book_word ON vocabulary_entries(vocabulary_book_id, word);
```

### 3.3 Foreign Key 인덱스 (수동 생성 권장)

```sql
-- articles 테이블
CREATE INDEX idx_articles_user_id ON articles(user_id);

-- article_vocabularies 테이블
CREATE INDEX idx_article_vocabularies_article_id ON article_vocabularies(article_id);

-- vocabulary_books 테이블
CREATE INDEX idx_vocabulary_books_user_id ON vocabulary_books(user_id);

-- vocabulary_entries 테이블
CREATE INDEX idx_vocabulary_entries_book_id ON vocabulary_entries(vocabulary_book_id);
CREATE INDEX idx_vocabulary_entries_source_article_id ON vocabulary_entries(source_article_id) WHERE source_article_id IS NOT NULL;
```

### 3.4 복합 인덱스 (쿼리 최적화)

```sql
-- 사용자별 아티클 목록 조회 (생성일 역순 정렬)
CREATE INDEX idx_articles_user_created ON articles(user_id, created_at DESC);

-- 사용자별 단어장 목록 조회 (기본 단어장 우선, 생성일 역순)
CREATE INDEX idx_vocabulary_books_user_default_created
ON vocabulary_books(user_id, is_default DESC, created_at DESC);

-- 단어장별 단어 목록 조회 (생성일 역순)
CREATE INDEX idx_vocabulary_entries_book_created
ON vocabulary_entries(vocabulary_book_id, created_at DESC);

-- 복습 대상 단어 조회 (향후)
CREATE INDEX idx_vocabulary_entries_review
ON vocabulary_entries(vocabulary_book_id, next_review_at)
WHERE learning_status IN ('LEARNING', 'REVIEWING');
```

### 3.5 부분 인덱스 (조건부 인덱스)

```sql
-- 추출 진행 중인 아티클 조회
CREATE INDEX idx_articles_incomplete ON articles(user_id, created_at) WHERE is_completed = FALSE;

-- 기본 단어장 조회
CREATE INDEX idx_vocabulary_books_default ON vocabulary_books(user_id) WHERE is_default = TRUE;
```

---

## 4. 외래 키 제약조건

### 4.1 Cascade 전략 요약

| 부모 테이블 | 자식 테이블 | ON DELETE | ON UPDATE | 사유 |
|------------|------------|-----------|-----------|------|
| `users` | `articles` | CASCADE | CASCADE | 사용자 삭제 시 아티클도 삭제 |
| `users` | `vocabulary_books` | CASCADE | CASCADE | 사용자 삭제 시 단어장도 삭제 |
| `articles` | `article_vocabularies` | CASCADE | CASCADE | 아티클 삭제 시 추출 단어도 삭제 |
| `vocabulary_books` | `vocabulary_entries` | CASCADE | CASCADE | 단어장 삭제 시 단어도 삭제 |
| `articles` | `vocabulary_entries` | **SET NULL** | CASCADE | 출처 아티클 삭제 시 참조만 해제 |

### 4.2 외래 키 DDL

```sql
-- articles.user_id → users.id
ALTER TABLE articles
ADD CONSTRAINT fk_articles_user_id
FOREIGN KEY (user_id) REFERENCES users(id)
ON DELETE CASCADE ON UPDATE CASCADE;

-- article_vocabularies.article_id → articles.id
ALTER TABLE article_vocabularies
ADD CONSTRAINT fk_article_vocabularies_article_id
FOREIGN KEY (article_id) REFERENCES articles(id)
ON DELETE CASCADE ON UPDATE CASCADE;

-- vocabulary_books.user_id → users.id
ALTER TABLE vocabulary_books
ADD CONSTRAINT fk_vocabulary_books_user_id
FOREIGN KEY (user_id) REFERENCES users(id)
ON DELETE CASCADE ON UPDATE CASCADE;

-- vocabulary_entries.vocabulary_book_id → vocabulary_books.id
ALTER TABLE vocabulary_entries
ADD CONSTRAINT fk_vocabulary_entries_book_id
FOREIGN KEY (vocabulary_book_id) REFERENCES vocabulary_books(id)
ON DELETE CASCADE ON UPDATE CASCADE;

-- vocabulary_entries.source_article_id → articles.id (nullable)
ALTER TABLE vocabulary_entries
ADD CONSTRAINT fk_vocabulary_entries_source_article_id
FOREIGN KEY (source_article_id) REFERENCES articles(id)
ON DELETE SET NULL ON UPDATE CASCADE;
```

---

## 5. 비즈니스 규칙과 DB 제약사항

### 5.1 사용자 (users)

| 규칙 코드 | 설명 | DB 제약 |
|----------|------|---------|
| `BR-AUTH-001` | 비밀번호 BCrypt 해시 저장 | Application Level |
| `BR-AUTH-002` | LOCAL provider는 password 필수 | `CHECK ((provider = 'LOCAL' AND password IS NOT NULL) OR (provider != 'LOCAL'))` |
| `BR-AUTH-004` | 기본 역할은 USER | `DEFAULT 'USER'` |
| `BR-US-002` | 이메일 중복 불가 | `UNIQUE (email)` |

### 5.2 아티클 (articles)

| 규칙 코드 | 설명 | DB 제약 |
|----------|------|---------|
| `BR-ART-005` | 본문 최대 50,000자 | `CHECK (LENGTH(content) <= 50000)` |
| `BR-ART-007` | 추출 상태 초기값 FALSE | `DEFAULT FALSE` |
| `BR-ART-031` | Article 삭제 시 ArticleVocabulary Cascade 삭제 | `ON DELETE CASCADE` |
| `BR-ART-032` | Article 삭제 시 VocabularyEntry의 source_article_id SET NULL | `ON DELETE SET NULL` |

### 5.3 AI 추출 단어 (article_vocabularies)

| 규칙 코드 | 설명 | DB 제약 |
|----------|------|---------|
| `BR-VOC-002` | 아티클당 최대 10개 단어 추출 | Application Level |
| `BR-VOC-011` | 재추출 시 기존 단어 삭제 | Application Level |

### 5.4 단어장 (vocabulary_books)

| 규칙 코드 | 설명 | DB 제약 |
|----------|------|---------|
| `BR-VB-001` | 사용자당 최대 20개 단어장 | Application Level |
| `BR-VB-002` | 동일 사용자 내 이름 중복 불가 | `UNIQUE (user_id, name)` |
| `BR-VB-004` | 기본 단어장 자동 생성 | Application Level (회원가입 시) |
| `BR-VB-012` | word_count denormalization | Trigger or Application Level |
| `BR-VB-030` | 기본 단어장 삭제 불가 | Application Level |
| `BR-VB-031` | 단어장 삭제 시 VocabularyEntry Cascade 삭제 | `ON DELETE CASCADE` |

### 5.5 단어장 항목 (vocabulary_entries)

| 규칙 코드 | 설명 | DB 제약 |
|----------|------|---------|
| `BR-VB-042` | 동일 단어장 내 단어 중복 불가 | `UNIQUE (vocabulary_book_id, word)` |
| `BR-VB-044` | 출처 아티클 참조 (nullable) | `FK source_article_id` |
| `BR-VB-045` | word_count 갱신 | Trigger or Application Level |

---

## 6. 마이그레이션 가이드

### 6.1 초기 테이블 생성 순서

```sql
-- 1. 독립 테이블 (외래 키 없음)
CREATE TABLE users (...);

-- 2. users 참조 테이블
CREATE TABLE articles (...);
CREATE TABLE vocabulary_books (...);

-- 3. articles 참조 테이블
CREATE TABLE article_vocabularies (...);

-- 4. 다중 참조 테이블 (vocabulary_books, articles)
CREATE TABLE vocabulary_entries (...);
```

### 6.2 인덱스 생성 권장 순서

1. Primary Key 인덱스 (자동 생성)
2. Unique 제약조건 인덱스 (자동 생성)
3. Foreign Key 인덱스 (수동 생성)
4. 복합 인덱스 (쿼리 패턴 분석 후)
5. 부분 인덱스 (필요 시)

### 6.3 Flyway 마이그레이션 파일 구조 (예시)

```
db/migration/
├── V1__create_users_table.sql
├── V2__create_articles_table.sql
├── V3__create_article_vocabularies_table.sql
├── V4__create_vocabulary_books_table.sql
├── V5__create_vocabulary_entries_table.sql
├── V6__create_indexes.sql
└── V7__add_learning_fields.sql (향후)
```

### 6.4 데이터 무결성 검증 쿼리

```sql
-- 1. 기본 단어장 누락 사용자 확인
SELECT u.id, u.email
FROM users u
LEFT JOIN vocabulary_books vb ON u.id = vb.user_id AND vb.is_default = TRUE
WHERE vb.id IS NULL;

-- 2. word_count 불일치 확인
SELECT vb.id, vb.name, vb.word_count, COUNT(ve.id) AS actual_count
FROM vocabulary_books vb
LEFT JOIN vocabulary_entries ve ON vb.id = ve.vocabulary_book_id
GROUP BY vb.id, vb.name, vb.word_count
HAVING vb.word_count != COUNT(ve.id);

-- 3. 고아 레코드 확인 (외래 키 제약조건이 없는 경우)
SELECT ve.id
FROM vocabulary_entries ve
LEFT JOIN vocabulary_books vb ON ve.vocabulary_book_id = vb.id
WHERE vb.id IS NULL;

-- 4. 추출 완료 상태 불일치 확인
SELECT a.id, a.title, a.is_completed, COUNT(av.id) AS vocab_count
FROM articles a
LEFT JOIN article_vocabularies av ON a.id = av.article_id
GROUP BY a.id, a.title, a.is_completed
HAVING (a.is_completed = TRUE AND COUNT(av.id) = 0);
```

---

## 부록

### A. 컬럼 명명 규칙

| 규칙 | 예시 | 설명 |
|------|------|------|
| Primary Key | `id` | 단순히 `id` 사용 |
| Foreign Key | `user_id`, `article_id` | `{참조 테이블명}_id` |
| Boolean | `is_completed`, `is_default` | `is_`, `has_` prefix |
| Timestamp | `created_at`, `updated_at` | `*_at` suffix |
| Count | `word_count` | `*_count` suffix |
| Enum | `provider`, `role` | 단수형 명사 |

### B. 테이블명 규칙

- 복수형 명사 사용 (예: `users`, `articles`)
- snake_case 사용
- 다대다 관계 테이블: `{entity1}_{entity2}` (현재 없음)

### C. 관련 문서

| 문서 | 위치 |
|------|------|
| 프로젝트 기술 문서 | `CLAUDE.md` |
| 제품 요구사항 문서 | `docs/PRD.md` |
| 기능 명세 문서 | `docs/FUNCTIONAL_SPEC.md` |

---

**작성**: Claude Code
**검토**: 필요 시 DBA 검토 권장
