package com.smim.backend.domain.article.dto;

import com.smim.backend.domain.article.Article;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * 아티클 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class ArticleResponse {

    private Long id;
    private String title;
    private String content;
    private String originalUrl;
    private String sourceDomain;
    @JsonProperty("isCompleted")
    private boolean isCompleted;
    private List<ArticleVocabularyResponse> vocabularyList;
    private Instant createdAt;

    /**
     * Article 엔티티를 DTO로 변환
     * @param article 아티클 엔티티
     * @return 아티클 응답 DTO
     */
    public static ArticleResponse from(Article article) {
        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .originalUrl(article.getOriginalUrl())
                .sourceDomain(article.getSourceDomain())
                .isCompleted(article.isCompleted())
                .vocabularyList(article.getVocabularyList().stream()
                        .map(ArticleVocabularyResponse::from)
                        .toList())
                .createdAt(article.getCreatedAt())
                .build();
    }
}
