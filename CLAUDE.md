# SMIM Backend

**SMIM** (Smart Media Insight Manager)

Spring Boot 기반의 영어 학습 지원 백엔드 애플리케이션입니다. 뉴스 기사 및 웹 글의 URL을 입력하면 본문을 자동으로 크롤링하고, Google Gemini AI를 활용하여 영어 학습에 유용한 추천 단어와 문장을 추출합니다. 사용자는 추출된 단어를 개인 단어장에 저장하고, 커스텀 단어장을 생성하여 체계적으로 어휘를 관리할 수 있습니다.

## Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 4.0.1
- **Build Tool**: Gradle
- **Database**: PostgreSQL (Production), H2 (Test)
- **Cache**: Redis
- **Security**: Spring Security + JWT + OAuth2 (Google, Kakao)
- **AI Integration**: Google Gemini API
- **Web Crawling**: Jsoup

## Project Structure

```
src/main/java/com/smim/backend/
├── domain/                    # 도메인별 패키지 (DDD 스타일)
│   ├── article/               # 기사 도메인
│   │   ├── api/               # Controller
│   │   ├── dto/               # Request/Response DTO
│   │   └── service/           # Service 레이어
│   ├── user/                  # 사용자 도메인
│   │   ├── api/
│   │   ├── dto/
│   │   └── service/
│   └── vocabularybook/        # 단어장 도메인 (신규)
│       ├── api/
│       ├── dto/
│       └── service/
├── global/                    # 공통 모듈
│   ├── ai/                    # AI 서비스 (Gemini)
│   ├── auth/                  # 인증/인가
│   │   ├── jwt/               # JWT 처리
│   │   └── oauth2/            # OAuth2 처리
│   ├── common/                # 공통 응답 포맷
│   ├── config/                # 설정 클래스
│   ├── ratelimit/             # Rate Limiting (신규)
│   └── error/                 # 예외 처리
│       └── exception/         # 커스텀 예외
```

## Build & Run Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run application
./gradlew bootRun

# Clean build
./gradlew clean build
```

## Core Features

### 구현 완료
- OAuth2 소셜 로그인 (Google, Kakao) + JWT 인증/인가
- URL 크롤링 (Jsoup) 및 Article 저장
- 비동기 AI 단어 추출 (Google Gemini API)
- User, Article, ArticleVocabulary 엔티티 및 기본 API
- 에러 코드 체계 (CO, AU, US, AR, VB prefix)
- Redis 캐시 인프라, CORS 설정, 비동기 처리

### 개발 예정 (PRD 기준)
- **P0 (필수)**:
  - 일반 로그인 (이메일 + 비밀번호)
  - 일일 사용량 제한 (Rate Limiting) - USER: 5/day, ADMIN: 무제한
  - 단어 선택/전체 저장
  - 커스텀 단어장 (Vocabulary Book) CRUD
- **P1 (중요)**:
  - 단어 재추출 기능
  - 난이도별 아티클 추천 (CEFR 기반)
  - 학습/복습 기능 (Spaced Repetition)
  - 복습 알림 (이메일, 카카오톡)

## Domain Entity Relationship

```
┌──────────┐       ┌─────────────┐       ┌─────────────────────┐
│   User   │1────N │   Article   │1────N │  ArticleVocabulary  │
│          │       │             │       │  (AI 추출 결과)       │
└──────────┘       └─────────────┘       └─────────────────────┘
     │                                              │
     │1                                             │ (참조)
     │                                              ▼
     │N          ┌─────────────────┐       ┌─────────────────┐
     └──────────►│ VocabularyBook  │1────N │ VocabularyEntry  │
                 │ (커스텀 단어장)   │       │ (저장된 단어)      │
                 └─────────────────┘       └─────────────────┘
```

**엔티티 설명**:
- `User`: 사용자 (Role: USER, ADMIN / Provider: GOOGLE, KAKAO, LOCAL)
- `Article`: 크롤링된 뉴스/웹 글
- `ArticleVocabulary`: AI가 추출한 단어 (Article 종속, 읽기 전용)
- `VocabularyBook`: 사용자 커스텀 단어장 (최초 가입 시 "기본 단어장" 자동 생성, 최대 20개)
- `VocabularyEntry`: 단어장에 저장된 개별 단어

## User Roles & Permissions

| 역할 | 코드 | 일일 크롤링/재추출 한도 | 설명 |
|------|------|------------------------|------|
| 일반 사용자 | `USER` (`ROLE_USER`) | 5회 | 기본 사용자 |
| 관리자 | `ADMIN` (`ROLE_ADMIN`) | 무제한 | 서비스 운영 및 관리 권한 |

**Provider**: `GOOGLE`, `KAKAO`, `LOCAL` (일반 로그인)

## API Endpoints

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signup` | 이메일 회원가입 (신규) |
| POST | `/api/auth/login` | 이메일 로그인 (신규) |
| POST | `/api/auth/password/reset-request` | 비밀번호 재설정 요청 (신규) |
| POST | `/api/auth/password/reset` | 비밀번호 재설정 (신규) |
| GET | `/oauth2/authorize/{provider}` | OAuth2 인증 시작 (google, kakao) |
| GET | `/login/oauth2/code/{provider}` | OAuth2 콜백 |

### Article
| Method | Endpoint | Description | Auth | Rate Limit |
|--------|----------|-------------|------|------------|
| POST | `/api/articles/crawl` | URL 크롤링 및 아티클 생성 | Required | USER: 5/day |
| POST | `/api/articles/{id}/re-extract` | 단어 재추출 요청 (신규) | Required | USER: 5/day |
| GET | `/api/articles` | 내 아티클 목록 조회 (신규) | Required | - |
| GET | `/api/articles/{id}` | 아티클 상세 조회 (신규) | Required | - |
| DELETE | `/api/articles/{id}` | 아티클 삭제 (신규) | Required | - |

### Vocabulary Book (신규)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/vocabulary-books` | 단어장 생성 | Required |
| GET | `/api/vocabulary-books` | 내 단어장 목록 조회 | Required |
| GET | `/api/vocabulary-books/{bookId}` | 단어장 상세 조회 (단어 목록 포함) | Required |
| PUT | `/api/vocabulary-books/{bookId}` | 단어장 정보 수정 | Required |
| DELETE | `/api/vocabulary-books/{bookId}` | 단어장 삭제 | Required |

### Vocabulary Entry (신규)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/vocabulary-books/{bookId}/words/selective` | 선택 단어 저장 | Required |
| POST | `/api/vocabulary-books/{bookId}/words/bulk` | 전체 단어 저장 | Required |
| POST | `/api/vocabulary-books/{bookId}/words` | 수동 단어 추가 | Required |
| GET | `/api/vocabulary-books/{bookId}/words` | 단어 목록 조회 (Pagination) | Required |
| DELETE | `/api/vocabulary-books/{bookId}/words/{wordId}` | 단어 삭제 | Required |

## Code Conventions

### Package Structure
- `domain`: 비즈니스 도메인별로 분리 (article, user 등)
- `global`: 프로젝트 전역에서 사용되는 공통 코드

### Naming Conventions
- Entity: 단수형 명사 (e.g., `Article`, `User`)
- Repository: `{Entity}Repository`
- Service: `{Entity}Service` 또는 `{Feature}Service`
- Controller: `{Entity}Controller` 또는 `{Feature}Controller`
- DTO: `{Entity}{Action}Request`, `{Entity}Response`

### API Response Format
- 성공: `ApiResponse<T>` wrapper 사용
- 실패: `ErrorResponse` with `ErrorCode`

```java
// 성공 응답
ApiResponse.success(data);
ApiResponse.success();

// 실패 응답
ApiResponse.fail(ErrorCode.RESOURCE_NOT_FOUND);
ApiResponse.fail(ErrorCode.INVALID_REQUEST, "커스텀 메시지");
```

### Error Code Convention
에러 코드는 `{PREFIX}{NUMBER}` 형식을 따릅니다.

| Prefix | Domain | Example |
|--------|--------|---------|
| `CO` | Common (공통) | `CO001`, `CO002` |
| `AU` | Auth (인증) | `AU001`, `AU002`, `AU006`, `AU007` |
| `US` | User (사용자) | `US001`, `US002` |
| `AR` | Article (아티클) | `AR001`, `AR002`, `AR004`, `AR005` |
| `VB` | Vocabulary Book (단어장) | `VB001`, `VB002`, `VB003`, `VB004` |

**주요 에러 코드**:
- `AU006`: 잘못된 이메일 또는 비밀번호 (401)
- `AU007`: 이메일 인증이 필요합니다 (403)
- `AR004`: 일일 사용 한도 초과 (429)
- `AR005`: 단어 추출이 진행 중입니다 (409)
- `VB001`: 단어장을 찾을 수 없습니다 (404)
- `VB002`: 단어장 생성 한도 초과 (400)
- `VB003`: 기본 단어장은 삭제할 수 없습니다 (400)
- `VB004`: 이미 단어장에 존재하는 단어입니다 (409)

새 도메인 추가 시 2글자 prefix를 정의하고 `ErrorCode` enum에 추가합니다.

### Entity 작성 규칙

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA용 기본 생성자
@Table(name = "table_name")
public class MyEntity extends BaseEntity {          // BaseEntity 상속 필수

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 필드 정의...

    @Builder  // Builder 패턴 사용
    public MyEntity(/* 필드들 */) {
        // 생성자 로직
    }

    // 비즈니스 메서드 (setter 대신 의미있는 메서드명 사용)
    public void updateStatus(Status newStatus) {
        this.status = newStatus;
    }
}
```

- `BaseEntity` 상속: `createdAt`, `updatedAt` 자동 관리
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`: 무분별한 객체 생성 방지
- `@Builder`: 명시적 객체 생성
- Setter 사용 금지: 의미있는 비즈니스 메서드로 상태 변경

### DTO 작성 규칙

**Response DTO**: Record 사용 (권장)
```java
/**
 * 조회 응답 DTO는 Record로 작성
 * - 불변성 보장
 * - 간결한 코드
 * - equals/hashCode/toString 자동 생성
 */
public record ArticleResponse(
    Long id,
    String title,
    String content,
    String originalUrl,
    String sourceDomain,
    boolean isCompleted,
    Instant createdAt,
    int vocabularyCount
) {
    // static factory method 사용 가능
    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
            article.getId(),
            article.getTitle(),
            article.getContent(),
            article.getOriginalUrl(),
            article.getSourceDomain(),
            article.isCompleted(),
            article.getCreatedAt(),
            article.getVocabularyList().size()
        );
    }
}
```

**단순 Request DTO**: Record 사용 가능
```java
/**
 * 단순한 요청 DTO (1-3개 필드, nested 객체 없음)
 * - Jakarta Validation annotations 지원
 * - Jackson deserialization 지원 (Spring Boot 3.0+)
 */
public record ArticleCrawlRequest(
    @NotBlank(message = "URL은 필수입니다.")
    @URL(message = "올바른 URL 형식이 아닙니다.")
    @Size(max = 2000, message = "URL은 2000자를 초과할 수 없습니다.")
    String url
) {}
```

**복잡한 Request DTO**: 일반 클래스 사용
```java
/**
 * 복잡한 요청 DTO는 일반 클래스 사용
 * - 다수의 필드 (8개 이상)
 * - Nested 객체 포함
 * - Builder 패턴 필요
 * - 조건부 Validation 필요
 */
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ComplexRequest {
    @NotBlank
    private String field1;

    @Valid
    private NestedObject nested;

    @Min(1)
    private Integer count;

    // 8개 이상의 필드...
}
```

**DTO 선택 기준**:
- ✅ **Response DTO → Record** (불변성, 간결성)
- ✅ **단순 Request (1-3 필드) → Record** (간결성)
- ⚠️ **복잡한 Request (8+ 필드, nested) → Class + @Builder** (가독성, 유연성)

### Exception Handling
- 커스텀 예외는 `global/error/exception/` 패키지에 정의
- `ErrorCode` enum 값을 사용하여 일관된 에러 코드 관리
- `GlobalExceptionHandler`에서 중앙 집중식 예외 처리

## Testing

### Test Types
| Suffix | Type | Database | Description |
|--------|------|----------|-------------|
| `*Test` | Unit Test | Mock/H2 | 단위 테스트 |
| `*IntegrationTest` | Integration Test | H2 | 통합 테스트 (인메모리) |
| `*PostgresIntegrationTest` | Integration Test | PostgreSQL | PostgreSQL 특화 테스트 |

### Test Naming Convention
```java
@Test
@DisplayName("설명: 한글로 테스트 목적 명시")
void methodName_조건_기대결과() {
    // given
    // when
    // then
}
```

### Test Configuration
- 테스트 설정: `src/test/resources/application-test.yaml`
- H2 인메모리 데이터베이스 사용 (테스트 환경)

## CI/CD

### GitHub Actions
- **Workflow**: `.github/workflows/gradle.yml`
- **Trigger**: `develop` 브랜치 push/PR
- **Jobs**:
  - `build`: Gradle 빌드 및 테스트
  - `dependency-submission`: Dependabot 의존성 그래프 생성

### PR Merge 전 체크사항
- [ ] `./gradlew build` 성공
- [ ] 모든 테스트 통과
- [ ] 코드 리뷰 완료

## New Domain Checklist

새로운 도메인(기능) 추가 시 생성해야 할 파일:

```
domain/{domain-name}/
├── {Entity}.java                    # 엔티티 (BaseEntity 상속)
├── {Entity}Repository.java          # JPA Repository
├── api/
│   └── {Entity}Controller.java      # REST Controller
├── dto/
│   ├── {Entity}Request.java         # 요청 DTO
│   └── {Entity}Response.java        # 응답 DTO
└── service/
    └── {Entity}Service.java         # 비즈니스 로직

global/error/
└── ErrorCode.java                   # 새 에러 코드 추가 (PREFIX 정의)

test/.../domain/{domain-name}/
├── {Entity}Test.java                # 엔티티 단위 테스트
├── {Entity}RepositoryTest.java      # Repository 테스트
└── service/
    ├── {Entity}ServiceTest.java     # Service 단위 테스트
    └── {Entity}ServiceIntegrationTest.java  # 통합 테스트
```

**예시: VocabularyBook 도메인**
- Entity: `VocabularyBook.java`, `VocabularyEntry.java`
- Repository: `VocabularyBookRepository.java`, `VocabularyEntryRepository.java`
- Controller: `VocabularyBookController.java`
- Service: `VocabularyBookService.java`
- DTO: `VocabularyBookCreateRequest.java`, `VocabularyBookResponse.java`, etc.
- Error Code: `VB` prefix 추가 (`VB001`, `VB002`, ...)

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL 호스트 | localhost |
| `DB_PORT` | PostgreSQL 포트 | 5433 |
| `DB_NAME` | 데이터베이스 이름 | smim_dev |
| `DB_USERNAME` | DB 사용자명 | postgres |
| `DB_PASSWORD` | DB 비밀번호 | postgres |
| `REDIS_HOST` | Redis 호스트 | localhost |
| `REDIS_PORT` | Redis 포트 | 6379 |
| `JWT_SECRET` | JWT 서명 키 | (dev only) |
| `GOOGLE_CLIENT_ID` | Google OAuth2 클라이언트 ID | - |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 클라이언트 시크릿 | - |
| `KAKAO_CLIENT_ID` | Kakao OAuth2 클라이언트 ID | - |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth2 클라이언트 시크릿 | - |
| `GEMINI_API_KEY` | Gemini API 키 | - |
| `FRONTEND_URL` | 프론트엔드 URL | http://localhost:5173 |

## Linear Project

- Backend 관련 이슈 생성 시 반드시 `project: "SMIM-Backend"` 지정
