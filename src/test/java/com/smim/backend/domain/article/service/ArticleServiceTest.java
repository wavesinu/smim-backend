package com.smim.backend.domain.article.service;

import com.smim.backend.domain.article.Article;
import com.smim.backend.domain.article.ArticleRepository;
import com.smim.backend.domain.article.dto.ArticleResponse;
import com.smim.backend.domain.article.dto.ArticleSummaryResponse;
import com.smim.backend.domain.user.CefrLevel;
import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.global.error.exception.BusinessException;
import com.smim.backend.global.error.exception.InvalidUrlException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleService 테스트")
class ArticleServiceTest {

    @InjectMocks
    private ArticleService articleService;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CrawlingService crawlingService;

    @Mock
    private VocabularyExtractorService vocabularyExtractorService;

    @Mock
    private ArticleExtractionService articleExtractionService;

    @Mock
    private ArticleDifficultyAnalyzer articleDifficultyAnalyzer;

    @Test
    @DisplayName("아티클 크롤링 및 저장 성공")
    void crawlAndSaveArticle_Success() {
        // given
        Long userId = 1L;
        String url = "https://example.com/article";

        User user = User.builder()
            .email("test@example.com")
            .name("Test User")
            .provider(Provider.GOOGLE)
            .providerId("12345")
            .role(Role.USER)
            .build();

        CrawlingService.CrawledArticle crawled = new CrawlingService.CrawledArticle(
            "Test Article Title",
            "This is the article content with many words.",
            "example.com"
        );

        Article savedArticle = Article.builder()
            .user(user)
            .title(crawled.getTitle())
            .content(crawled.getContent())
            .originalUrl(url)
            .sourceDomain(crawled.getDomain())
            .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(crawlingService.crawl(url)).willReturn(crawled);
        given(articleRepository.save(any(Article.class))).willReturn(savedArticle);

        // when
        ArticleResponse response = articleService.crawlAndSaveArticle(url, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Article Title");
        assertThat(response.getContent()).isEqualTo("This is the article content with many words.");
        assertThat(response.getOriginalUrl()).isEqualTo(url);
        assertThat(response.getSourceDomain()).isEqualTo("example.com");
        assertThat(response.isCompleted()).isFalse();

        verify(userRepository, times(1)).findById(userId);
        verify(crawlingService, times(1)).crawl(url);
        verify(articleRepository, times(1)).save(any(Article.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 - 예외 발생")
    void crawlAndSaveArticle_UserNotFound_ThrowsException() {
        // given
        Long userId = 999L;
        String url = "https://example.com/article";

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> articleService.crawlAndSaveArticle(url, userId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("사용자를 찾을 수 없습니다");

        verify(userRepository, times(1)).findById(userId);
        verify(crawlingService, times(0)).crawl(any());
        verify(articleRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("크롤링 실패 시 예외 전파")
    void crawlAndSaveArticle_CrawlingFails_ThrowsException() {
        // given
        Long userId = 1L;
        String url = "https://invalid-url.com";

        User user = User.builder()
            .email("test@example.com")
            .name("Test User")
            .provider(Provider.GOOGLE)
            .providerId("12345")
            .role(Role.USER)
            .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(crawlingService.crawl(url)).willThrow(new InvalidUrlException("유효하지 않은 URL입니다."));

        // when & then
        assertThatThrownBy(() -> articleService.crawlAndSaveArticle(url, userId))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("유효하지 않은 URL입니다");

        verify(userRepository, times(1)).findById(userId);
        verify(crawlingService, times(1)).crawl(url);
        verify(articleRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("Article 엔티티가 올바른 데이터로 생성됨")
    void crawlAndSaveArticle_ArticleEntityCreatedCorrectly() {
        // given
        Long userId = 1L;
        String url = "https://example.com/test-article";

        User user = User.builder()
            .email("test@example.com")
            .name("Test User")
            .provider(Provider.KAKAO)
            .providerId("67890")
            .role(Role.USER)
            .build();

        CrawlingService.CrawledArticle crawled = new CrawlingService.CrawledArticle(
            "New Article",
            "Article content here",
            "example.com"
        );

        Article savedArticle = Article.builder()
            .user(user)
            .title(crawled.getTitle())
            .content(crawled.getContent())
            .originalUrl(url)
            .sourceDomain(crawled.getDomain())
            .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(crawlingService.crawl(url)).willReturn(crawled);
        given(articleRepository.save(any(Article.class))).willReturn(savedArticle);

        // when
        articleService.crawlAndSaveArticle(url, userId);

        // then
        ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).save(articleCaptor.capture());

        Article capturedArticle = articleCaptor.getValue();
        assertThat(capturedArticle.getUser()).isEqualTo(user);
        assertThat(capturedArticle.getTitle()).isEqualTo("New Article");
        assertThat(capturedArticle.getContent()).isEqualTo("Article content here");
        assertThat(capturedArticle.getOriginalUrl()).isEqualTo(url);
        assertThat(capturedArticle.getSourceDomain()).isEqualTo("example.com");
        assertThat(capturedArticle.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("트렌딩 아티클 조회 - 전체 레벨")
    void getTrendingArticles_withoutCefrFilter() {
        Article article = createCompletedArticle("Trending 1");
        PageRequest pageRequest = PageRequest.of(0, 10);
        given(articleRepository.findByIsCompletedTrue(pageRequest))
                .willReturn(new PageImpl<>(List.of(article), pageRequest, 1));

        Page<ArticleSummaryResponse> result = articleService.getTrendingArticles(pageRequest, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Trending 1");
        verify(articleRepository, times(1)).findByIsCompletedTrue(pageRequest);
    }

    @Test
    @DisplayName("트렌딩 아티클 조회 - CEFR 필터 적용")
    void getTrendingArticles_withCefrFilter() {
        Article article = createCompletedArticle("Trending A2");
        article.updateDifficulty(CefrLevel.A2, 1.2, 0.1, java.time.Instant.now());

        PageRequest pageRequest = PageRequest.of(0, 10);
        given(articleRepository.findByIsCompletedTrueAndCefrLevel(CefrLevel.A2, pageRequest))
                .willReturn(new PageImpl<>(List.of(article), pageRequest, 1));

        Page<ArticleSummaryResponse> result = articleService.getTrendingArticles(pageRequest, CefrLevel.A2);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getCefrLevel()).isEqualTo(CefrLevel.A2);
        verify(articleRepository, times(1)).findByIsCompletedTrueAndCefrLevel(CefrLevel.A2, pageRequest);
    }

    private Article createCompletedArticle(String title) {
        User user = User.builder()
                .email("owner@example.com")
                .name("Owner")
                .provider(Provider.LOCAL)
                .providerId(null)
                .role(Role.USER)
                .password("encoded")
                .build();
        Article article = Article.builder()
                .user(user)
                .title(title)
                .content("content")
                .originalUrl("https://example.com/article")
                .sourceDomain("example.com")
                .build();
        article.markAsCompleted();
        return article;
    }
}
