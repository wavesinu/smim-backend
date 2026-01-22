package com.smim.backend.domain.article.service;

import com.smim.backend.domain.article.Article;
import com.smim.backend.domain.article.ArticleRepository;
import com.smim.backend.domain.article.dto.ArticleResponse;
import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ArticleService 통합 테스트
 * 실제 DB와 크롤링 서비스를 사용하여 전체 플로우를 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ArticleService 통합 테스트")
class ArticleServiceIntegrationTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .email("integration-test@example.com")
                .name("Integration Test User")
                .provider(Provider.GOOGLE)
                .providerId("integration-test-123")
                .role(Role.USER)
                .build();

        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("실제 BBC 뉴스 크롤링 및 DB 저장 성공")
    void crawlAndSaveArticle_RealBbcNews_Success() {
        // given
        String bbcUrl = "https://www.bbc.com/news/articles/cgezx40r7d7o";

        // when
        ArticleResponse response = articleService.crawlAndSaveArticle(bbcUrl, testUser.getId());

        // then - 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isNotBlank();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getOriginalUrl()).isEqualTo(bbcUrl);
        assertThat(response.getSourceDomain()).isEqualTo("www.bbc.com");
        assertThat(response.isCompleted()).isFalse();
        assertThat(response.getCreatedAt()).isNotNull();

        // 콘솔 출력
        System.out.println("=== BBC 뉴스 크롤링 및 DB 저장 결과 ===");
        System.out.println("Article ID: " + response.getId());
        System.out.println("제목: " + response.getTitle());
        System.out.println("도메인: " + response.getSourceDomain());
        System.out.println("본문 길이: " + response.getContent().length() + "자");
        System.out.println("URL: " + response.getOriginalUrl());
        System.out.println("생성 시각: " + response.getCreatedAt());
        System.out.println("======================================");

        // DB 저장 검증
        Article savedArticle = articleRepository.findById(response.getId()).orElse(null);
        assertThat(savedArticle).isNotNull();
        assertThat(savedArticle.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedArticle.getTitle()).isEqualTo(response.getTitle());
        assertThat(savedArticle.getContent()).isEqualTo(response.getContent());
        assertThat(savedArticle.getOriginalUrl()).isEqualTo(bbcUrl);
        assertThat(savedArticle.getSourceDomain()).isEqualTo("www.bbc.com");
        assertThat(savedArticle.isCompleted()).isFalse();
        assertThat(savedArticle.getCreatedAt()).isNotNull();
        assertThat(savedArticle.getUpdatedAt()).isNotNull();

        // 사용자 연관관계 검증
        assertThat(savedArticle.getUser()).isNotNull();
        assertThat(savedArticle.getUser().getEmail()).isEqualTo("integration-test@example.com");
    }

    @Test
    @DisplayName("같은 URL을 여러 번 저장 가능 (중복 허용)")
    void crawlAndSaveArticle_DuplicateUrl_Allowed() {
        // given
        String url = "https://www.bbc.com/news/articles/cgezx40r7d7o";

        // when - 같은 URL을 두 번 저장
        ArticleResponse response1 = articleService.crawlAndSaveArticle(url, testUser.getId());
        ArticleResponse response2 = articleService.crawlAndSaveArticle(url, testUser.getId());

        // then - 두 개의 다른 Article이 생성됨
        assertThat(response1.getId()).isNotEqualTo(response2.getId());

        // DB 검증
        long count = articleRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("본문 길이가 긴 아티클도 정상 저장")
    void crawlAndSaveArticle_LongContent_Success() {
        // given
        String bbcUrl = "https://www.bbc.com/news/articles/cgezx40r7d7o";

        // when
        ArticleResponse response = articleService.crawlAndSaveArticle(bbcUrl, testUser.getId());

        // then
        assertThat(response.getContent().length()).isGreaterThan(1000); // 최소 1000자 이상

        // DB 저장 확인
        Article savedArticle = articleRepository.findById(response.getId()).orElse(null);
        assertThat(savedArticle).isNotNull();
        assertThat(savedArticle.getContent()).hasSize(response.getContent().length());
    }

    @Test
    @DisplayName("여러 사용자가 같은 아티클 저장 가능")
    void crawlAndSaveArticle_MultipleUsers_Success() {
        // given
        User anotherUser = User.builder()
                .email("another-user@example.com")
                .name("Another User")
                .provider(Provider.KAKAO)
                .providerId("another-test-456")
                .role(Role.USER)
                .build();
        anotherUser = userRepository.save(anotherUser);

        String url = "https://www.bbc.com/news/articles/cgezx40r7d7o";

        // when
        ArticleResponse response1 = articleService.crawlAndSaveArticle(url, testUser.getId());
        ArticleResponse response2 = articleService.crawlAndSaveArticle(url, anotherUser.getId());

        // then
        assertThat(response1.getId()).isNotEqualTo(response2.getId());

        Article article1 = articleRepository.findById(response1.getId()).orElse(null);
        Article article2 = articleRepository.findById(response2.getId()).orElse(null);

        assertThat(article1).isNotNull();
        assertThat(article2).isNotNull();
        assertThat(article1.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(article2.getUser().getId()).isEqualTo(anotherUser.getId());
    }
}
