package com.smim.backend.domain.article.service;

import com.smim.backend.domain.article.Article;
import com.smim.backend.domain.article.ArticleRepository;
import com.smim.backend.domain.article.dto.ArticleResponse;
import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL DB를 사용한 실제 통합 테스트
 * 데이터가 실제 DB에 저장되는지 확인합니다.
 */
@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("ArticleService PostgreSQL 통합 테스트")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArticleServicePostgresIntegrationTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeAll
    void setUpBeforeAll() {
        System.out.println("========================================");
        System.out.println("PostgreSQL 통합 테스트 시작");
        System.out.println("데이터베이스: smim_dev");
        System.out.println("========================================");
    }

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .email("postgres-integration-test@example.com")
                .name("PostgreSQL Integration Test User")
                .provider(Provider.GOOGLE)
                .providerId("postgres-integration-test-123")
                .role(Role.USER)
                .build();

        testUser = userRepository.save(testUser);
        System.out.println("테스트 사용자 생성: ID=" + testUser.getId());
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리 (선택사항)
        // articleRepository.deleteAll();
        // userRepository.deleteAll();
        System.out.println("테스트 완료 - 데이터는 PostgreSQL에 유지됩니다.");
    }

    @Test
    @DisplayName("실제 BBC 뉴스를 PostgreSQL DB에 저장")
    void crawlAndSaveArticle_RealBbcNews_SavedToPostgres() {
        // given
        String bbcUrl = "https://www.bbc.com/news/articles/cgezx40r7d7o";

        System.out.println("\n=== BBC 뉴스 크롤링 시작 ===");
        System.out.println("URL: " + bbcUrl);

        // when
        ArticleResponse response = articleService.crawlAndSaveArticle(bbcUrl, testUser.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isNotBlank();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getOriginalUrl()).isEqualTo(bbcUrl);
        assertThat(response.getSourceDomain()).isEqualTo("www.bbc.com");

        System.out.println("\n=== PostgreSQL DB 저장 결과 ===");
        System.out.println("✅ Article ID: " + response.getId());
        System.out.println("✅ 제목: " + response.getTitle());
        System.out.println("✅ 도메인: " + response.getSourceDomain());
        System.out.println("✅ 본문 길이: " + response.getContent().length() + "자");
        System.out.println("✅ URL: " + response.getOriginalUrl());
        System.out.println("✅ 생성 시각: " + response.getCreatedAt());
        System.out.println("✅ 사용자 ID: " + testUser.getId());

        // PostgreSQL에서 실제 데이터 확인
        Article savedArticle = articleRepository.findById(response.getId()).orElse(null);
        assertThat(savedArticle).isNotNull();
        assertThat(savedArticle.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedArticle.getTitle()).isEqualTo(response.getTitle());

        System.out.println("\n=== PostgreSQL 확인 쿼리 ===");
        System.out.println("다음 쿼리로 데이터를 확인할 수 있습니다:");
        System.out.println("docker exec smim-container psql -U postgres -d smim_dev -c \"SELECT id, title, source_domain, created_at FROM articles WHERE id = " + response.getId() + ";\"");
        System.out.println("docker exec smim-container psql -U postgres -d smim_dev -c \"SELECT id, email, name FROM users WHERE id = " + testUser.getId() + ";\"");
        System.out.println("=====================================\n");

        // 대기 시간을 주어 DB 커밋 확인
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("PostgreSQL에 저장된 데이터 카운트 확인")
    void checkArticleCount_InPostgres() {
        // given
        String bbcUrl = "https://www.bbc.com/news/articles/cgezx40r7d7o";

        // when
        long countBefore = articleRepository.count();
        System.out.println("저장 전 Article 개수: " + countBefore);

        ArticleResponse response = articleService.crawlAndSaveArticle(bbcUrl, testUser.getId());

        long countAfter = articleRepository.count();
        System.out.println("저장 후 Article 개수: " + countAfter);

        // then
        assertThat(countAfter).isGreaterThan(countBefore);
        assertThat(response.getId()).isNotNull();

        System.out.println("✅ 새로운 Article이 PostgreSQL에 저장되었습니다.");
    }
}
