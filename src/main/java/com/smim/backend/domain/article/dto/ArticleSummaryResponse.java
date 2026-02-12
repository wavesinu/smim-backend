package com.smim.backend.domain.article.dto;

import com.smim.backend.domain.article.Article;
import com.smim.backend.domain.user.CefrLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ArticleSummaryResponse {

    private Long id;
    private String title;
    private String originalUrl;
    private String sourceDomain;
    @JsonProperty("isCompleted")
    private boolean isCompleted;
    private int vocabularyCount;
    private CefrLevel cefrLevel;
    private Instant createdAt;

    public static ArticleSummaryResponse from(Article article) {
        return ArticleSummaryResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .originalUrl(article.getOriginalUrl())
                .sourceDomain(article.getSourceDomain())
                .isCompleted(article.isCompleted())
                .vocabularyCount(article.getVocabularyList().size())
                .cefrLevel(article.getCefrLevel())
                .createdAt(article.getCreatedAt())
                .build();
    }
}
