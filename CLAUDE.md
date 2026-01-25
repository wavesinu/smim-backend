# SMIM Backend

Spring Boot 기반의 백엔드 애플리케이션으로, 뉴스 기사 크롤링 및 AI 요약 기능을 제공합니다.

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
│   └── user/                  # 사용자 도메인
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

## API Endpoints

### Article
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/articles/crawl` | URL 크롤링 및 아티클 생성 | Required |

### Auth (OAuth2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/oauth2/authorize/{provider}` | OAuth2 인증 시작 (google, kakao) |
| GET | `/login/oauth2/code/{provider}` | OAuth2 콜백 |

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
| `AU` | Auth (인증) | `AU001`, `AU002` |
| `US` | User (사용자) | `US001`, `US002` |
| `AR` | Article (아티클) | `AR001`, `AR002` |

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
