# SMIM, 자연스럽게 스며드는 단어장

SMIM은 뉴스 기사 및 웹 글의 URL을 입력하면 본문을 자동으로 크롤링하고, Google Gemini AI를 활용하여 영어 학습에 유용한 추천 단어와 문장을 추출해주는 서비스입니다. 사용자는 추출된 단어를 개인 단어장에 저장하고, 커스텀 단어장을 생성하여 체계적으로 어휘를 관리할 수 있습니다.

> **Note**: 이 프로젝트는 현재 개발 진행 중이며, 일부 기능은 변경될 수 있습니다.

## 🌟 주요 기능

* **URL 크롤링 및 본문 추출**: 뉴스/웹 글의 URL을 입력하면 본문을 자동으로 가져옵니다.
* **AI 기반 단어/문장 추천**: Google Gemini API를 활용하여 학습 가치가 높은 단어와 예문을 자동으로 분석 및 추출합니다.
* **나만의 단어장**: 추출된 단어를 저장하고 관리할 수 있는 개인화된 단어장을 제공합니다.
* **사용자 맞춤형 학습**: CEFR 레벨 등급에 기반하여 사용자 수준에 맞는 아티클과 단어를 추천합니다. (개발 중)
* **체계적인 복습 시스템**: 에빙하우스 망각 곡선 이론을 적용한 복습 알림 및 퀴즈 기능을 제공합니다. (개발 중)
* **소셜 로그인**: Google, Kakao 로그인을 지원하여 간편하게 시작할 수 있습니다.

## 🛠 기술 스택 (Tech Stack)

### Backend

* **Language**: Java 17
* **Framework**: Spring Boot 3.x (PRD 기준 4.0.1이나 현재 생태계 고려 시 최신 버전 사용)
* **Build Tool**: Gradle
* **Database**: PostgreSQL (Production), H2 (Test)
* **ORM**: JPA (Hibernate)
* **Cache**: Redis (Rate Limiting, Caching)
* **Security**: Spring Security, OAuth2 Client, JWT
* **AI**: Google Gemini API
* **Crawling**: Jsoup

### Tools & Infra

* **CI/CD**: GitHub Actions
* **API Documentation**: [API Specification](./docs/API_SPECIFICATION.md)

## 🚀 시작하기 (Getting Started)

### 사전 요구 사항 (Prerequisites)

* Java 17 이상
* PostgreSQL (Local 또는 Docker)
* Redis (Local 또는 Docker)

### 환경 변수 설정 (Configuration)

프로젝트 실행을 위해 `application.yaml`에 정의된 다음 환경 변수들을 설정해야 합니다. IDE의 실행 설정(Run Configuration) 또는 시스템 환경 변수로 등록해주세요.

| 환경 변수명 | 설명 | 기본값 예시 |
|-------------|------|-------------|
| `GOOGLE_CLIENT_ID` | Google OAuth 클라이언트 ID | - |
| `GOOGLE_CLIENT_SECRET` | Google OAuth 클라이언트 Secret | - |
| `KAKAO_CLIENT_ID` | Kakao OAuth 클라이언트 ID | - |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth 클라이언트 Secret | - |
| `DB_HOST` | 데이터베이스 호스트 | localhost |
| `DB_PORT` | 데이터베이스 포트 | 5433 |
| `DB_NAME` | 데이터베이스 이름 | smim_dev |
| `DB_USERNAME` | 데이터베이스 사용자명 | postgres |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | postgres |
| `REDIS_HOST` | Redis 호스트 | localhost |
| `REDIS_PORT` | Redis 포트 | 6379 |
| `JWT_SECRET` | JWT 서명용 비밀키 (32자 이상) | - |
| `GEMINI_API_KEY` | Google Gemini API 키 | - |
| `FRONTEND_URL` | 프론트엔드 URL (Redirect용) | <http://localhost:5173> |

### 실행 방법 (Running the App)

1. **저장소 클론**

   ```bash
   git clone https://github.com/your-username/smim-backend.git
   cd smim-backend
   ```

2. **외부 서비스 실행 (Docker Compose 권장)**
   (Docker Compose 파일이 제공되는 경우)

   ```bash
   docker-compose up -d
   ```

3. **애플리케이션 실행**

   ```bash
   ./gradlew bootRun
   ```

4. **테스트 실행**

   ```bash
   ./gradlew test
   ```

## 📂 프로젝트 구조

```
smim-backend/
├── src/main/java/com/smim/backend
│   ├── common/          # 공통 유틸리티, 예외 처리, Response 포맷
│   ├── config/          # 설정 파일 (Security, WebMvc 등)
│   ├── domain/          # 도메인별 엔티티, 레포지토리, 서비스
│   │   ├── auth/        # 인증/인가 관련
│   │   ├── user/        # 사용자 관련
│   │   ├── article/     # 기사 및 크롤링 관련
│   │   ├── vocabulary/  # 단어장 관련
│   │   └── ...
│   └── external/        # 외부 API 연동 (Gemini, OAuth 등)
├── src/main/resources
│   ├── application.yaml # 애플리케이션 설정
│   └── ...
├── docs/                # 프로젝트 문서 (PRD, Spec 등)
└── ...
```

## 📚 문서 (Documentation)

더 자세한 내용은 `docs` 디렉토리의 문서를 참고하세요.

* [**Product Requirements Document (PRD)**](./docs/PRD.md): 제품 기획 및 요구사항 정의서
* [**Functional Specification**](./docs/FUNCTIONAL_SPEC.md): 상세 기능 명세서
* [**API Specification**](./docs/API_SPECIFICATION.md): API 엔드포인트 및 명세

---
**SMIM Team**
