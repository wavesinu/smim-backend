package com.smim.backend.domain.article.service;

import com.smim.backend.domain.article.Article;
import com.smim.backend.domain.article.ArticleRepository;
import com.smim.backend.domain.article.ExtractionStatus;
import com.smim.backend.domain.article.dto.ArticleDifficultyResponse;
import com.smim.backend.domain.article.dto.ArticleExtractionStatusResponse;
import com.smim.backend.domain.article.dto.ArticleReextractResponse;
import com.smim.backend.domain.article.dto.ArticleResponse;
import com.smim.backend.domain.article.dto.ArticleSummaryResponse;
import com.smim.backend.domain.user.CefrLevel;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.global.error.ErrorCode;
import com.smim.backend.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 아티클 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final CrawlingService crawlingService;
    private final ArticleExtractionService articleExtractionService;
    private final ArticleDifficultyAnalyzer articleDifficultyAnalyzer;

    /**
     * URL을 크롤링하여 아티클을 저장합니다.
     * 저장 후 비동기로 AI 단어 추출을 시작합니다.
     *
     * @param url    크롤링할 URL
     * @param userId 사용자 ID
     * @return 저장된 아티클 응답 DTO
     */
    @Transactional
    public ArticleResponse crawlAndSaveArticle(String url, Long userId) {
        log.info("아티클 크롤링 시작 - URL: {}, UserId: {}", url, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CrawlingService.CrawledArticle crawled = crawlingService.crawl(url);

        Article article = Article.builder()
                .user(user)
                .title(crawled.getTitle())
                .content(crawled.getContent())
                .originalUrl(url)
                .sourceDomain(crawled.getDomain())
                .build();

        Article savedArticle = articleRepository.save(article);
        log.info("아티클 저장 완료 - ArticleId: {}", savedArticle.getId());

        runAfterCommit(() -> articleExtractionService.extractVocabularyAsync(savedArticle.getId()));

        return ArticleResponse.from(savedArticle);
    }

    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> getMyArticles(Long userId, Pageable pageable) {
        User user = getUserOrThrow(userId);
        Page<Article> page = articleRepository.findByUser(user, pageable);
        return page.map(ArticleSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ArticleResponse getArticleDetail(Long userId, Long articleId) {
        Article article = articleRepository.findByIdWithVocabulary(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        validateArticleOwnership(userId, article);
        return ArticleResponse.from(article);
    }

    @Transactional
    public void deleteArticle(Long userId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        validateArticleOwnership(userId, article);
        articleRepository.delete(article);
    }

    @Transactional
    public ArticleReextractResponse reextractVocabulary(Long userId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        validateArticleOwnership(userId, article);

        if (article.getExtractionStatus() == ExtractionStatus.EXTRACTING) {
            throw new BusinessException(ErrorCode.EXTRACTION_IN_PROGRESS);
        }

        article.startExtraction();
        articleRepository.save(article);
        runAfterCommit(() -> articleExtractionService.extractVocabularyAsync(article.getId()));

        return ArticleReextractResponse.builder()
                .articleId(article.getId())
                .status(article.getExtractionStatus())
                .message("단어 재추출이 시작되었습니다.")
                .build();
    }

    @Transactional
    public ArticleExtractionStatusResponse getExtractionStatus(Long userId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        validateArticleOwnership(userId, article);

        if (article.getExtractionStatus() == ExtractionStatus.EXTRACTING
                && article.getExtractionStartedAt() != null
                && Duration.between(article.getExtractionStartedAt(), Instant.now()).toMinutes() >= 5) {
            article.markExtractionFailed();
            articleRepository.save(article);
        }

        return ArticleExtractionStatusResponse.builder()
                .articleId(article.getId())
                .status(article.getExtractionStatus())
                .vocabularyCount(article.getVocabularyList().size())
                .build();
    }

    @Transactional
    public ArticleDifficultyResponse getArticleDifficulty(Long userId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        validateArticleOwnership(userId, article);

        if (article.getCefrLevel() == null || article.getAnalyzedAt() == null) {
            articleDifficultyAnalyzer.updateDifficulty(article);
        }

        return ArticleDifficultyResponse.builder()
                .articleId(article.getId())
                .cefrLevel(article.getCefrLevel())
                .averageWordDifficulty(article.getAverageWordDifficulty())
                .complexSentenceRatio(article.getComplexSentenceRatio())
                .analyzedAt(article.getAnalyzedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> getRecommendedArticles(Long userId, Pageable pageable, int levelRange) {
        User user = getUserOrThrow(userId);
        if (user.getTargetCefrLevel() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        List<CefrLevel> ranges = expandCefrRange(user.getTargetCefrLevel(), levelRange);
        Page<Article> page = articleRepository.findByCefrLevelIn(ranges, pageable);
        return page.map(ArticleSummaryResponse::from);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private void validateArticleOwnership(Long userId, Article article) {
        if (!Objects.equals(article.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.ARTICLE_ACCESS_DENIED);
        }
    }

    private List<CefrLevel> expandCefrRange(CefrLevel base, int range) {
        List<CefrLevel> levels = List.of(CefrLevel.values());
        int index = levels.indexOf(base);
        int min = Math.max(0, index - range);
        int max = Math.min(levels.size() - 1, index + range);
        return levels.subList(min, max + 1);
    }
}
