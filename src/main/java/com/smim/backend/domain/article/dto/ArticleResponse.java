package com.smim.backend.domain.article.dto;

import com.smim.backend.domain.article.Article;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * 아티클 응답 DTO
 */
@Getter
@AllArgsConstructor
public class ArticleResponse {

    private Long id;
    private String title;
    private String content;
    private String originalUrl;
    private String sourceDomain;
    private boolean isCompleted;
    private Instant createdAt;
    private int vocabularyCount;

    /**
     * Article 엔티티를 DTO로 변환
     * @param article 아티클 엔티티
     * @return 아티클 응답 DTO
     */
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
