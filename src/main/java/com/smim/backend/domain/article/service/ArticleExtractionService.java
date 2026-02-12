package com.smim.backend.domain.article.service;

import com.smim.backend.domain.article.Article;
import com.smim.backend.domain.article.ArticleRepository;
import com.smim.backend.domain.article.ArticleVocabulary;
import com.smim.backend.domain.article.dto.ArticleVocabularyResponse;
import com.smim.backend.global.error.ErrorCode;
import com.smim.backend.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleExtractionService {

    private final ArticleRepository articleRepository;
    private final VocabularyExtractorService vocabularyExtractorService;
    private final ArticleDifficultyAnalyzer articleDifficultyAnalyzer;

    @Async
    @Transactional
    public void extractVocabularyAsync(Long articleId) {
        log.info("AI 단어 추출 시작 - ArticleId: {}", articleId);

        try {
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

            List<ArticleVocabularyResponse> vocabularyList = vocabularyExtractorService.extractVocabulary(
                    article.getContent(),
                    10);

            for (ArticleVocabularyResponse vocabResponse : vocabularyList) {
                ArticleVocabulary vocabulary = ArticleVocabulary.builder()
                        .word(vocabResponse.getWord())
                        .definition(vocabResponse.getDefinition())
                        .contextSentence(vocabResponse.getContextSentence())
                        .build();

                article.addVocabulary(vocabulary);
            }

            article.markAsCompleted();
            articleDifficultyAnalyzer.updateDifficulty(article);
            articleRepository.save(article);
            log.info("AI 단어 추출 완료 - ArticleId: {}, 단어 수: {}", articleId, vocabularyList.size());

        } catch (Exception e) {
            log.error("AI 단어 추출 실패 - ArticleId: {}", articleId, e);
            articleRepository.findById(articleId).ifPresent(article -> {
                article.markExtractionFailed();
                articleRepository.save(article);
            });
        }
    }
}
