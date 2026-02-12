package com.smim.backend.domain.article.dto;

import com.smim.backend.domain.article.ExtractionStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArticleExtractionStatusResponse {
    private Long articleId;
    private ExtractionStatus status;
    private int vocabularyCount;
}
