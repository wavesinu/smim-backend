package com.smim.backend.domain.article.service;

import com.smim.backend.domain.article.Article;
import com.smim.backend.domain.article.ArticleRepository;
import com.smim.backend.domain.article.ArticleVocabulary;
import com.smim.backend.domain.article.dto.ArticleResponse;
import com.smim.backend.domain.article.dto.ArticleVocabularyResponse;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.global.error.ErrorCode;
import com.smim.backend.global.error.exception.CrawlingFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final VocabularyExtractorService vocabularyExtractorService;

    /**
     * URL을 크롤링하여 아티클을 저장합니다.
     * 저장 후 비동기로 AI 단어 추출을 시작합니다.
     *
     * @param url    크롤링할 URL
     * @param userId 사용자 ID
     * @return 저장된 아티클 응답 DTO
     * @throws CrawlingFailedException 크롤링 실패 시
     */
    @Transactional
    public ArticleResponse crawlAndSaveArticle(String url, Long userId) {
        log.info("아티클 크롤링 시작 - URL: {}, UserId: {}", url, userId);

        // 사용자 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 웹 페이지 크롤링
        CrawlingService.CrawledArticle crawled = crawlingService.crawl(url);

        // Article 엔티티 생성 및 저장
        Article article = Article.builder()
            .user(user)
            .title(crawled.getTitle())
            .content(crawled.getContent())
            .originalUrl(url)
            .sourceDomain(crawled.getDomain())
            .build();

        Article savedArticle = articleRepository.save(article);
        log.info("아티클 저장 완료 - ArticleId: {}", savedArticle.getId());

        // 비동기로 AI 단어 추출 시작 (Phase 5에서 구현 예정)
        extractVocabularyAsync(savedArticle.getId());

        return ArticleResponse.from(savedArticle);
    }

    /**
     * 비동기로 AI를 이용하여 아티클에서 단어를 추출합니다.
     *
     * @param articleId 아티클 ID
     */
    @Async
    @Transactional
    public void extractVocabularyAsync(Long articleId) {
        log.info("AI 단어 추출 시작 - ArticleId: {}", articleId);

        try {
            // 아티클 조회
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new IllegalArgumentException("아티클을 찾을 수 없습니다."));

            // AI를 이용한 단어 추출 (최대 10개)
            List<ArticleVocabularyResponse> vocabularyList = vocabularyExtractorService.extractVocabulary(
                    article.getContent(),
                    10
            );

            // ArticleVocabulary 엔티티로 변환 및 저장
            for (ArticleVocabularyResponse vocabResponse : vocabularyList) {
                ArticleVocabulary vocabulary = ArticleVocabulary.builder()
                        .word(vocabResponse.getWord())
                        .definition(vocabResponse.getDefinition())
                        .contextSentence(vocabResponse.getContextSentence())
                        .build();

                article.addVocabulary(vocabulary);
            }

            articleRepository.save(article);
            log.info("AI 단어 추출 완료 - ArticleId: {}, 단어 수: {}", articleId, vocabularyList.size());

        } catch (Exception e) {
            log.error("AI 단어 추출 실패 - ArticleId: {}", articleId, e);
            // 비동기이므로 예외를 삼킴 (서비스 계속 동작)
        }
    }
}
