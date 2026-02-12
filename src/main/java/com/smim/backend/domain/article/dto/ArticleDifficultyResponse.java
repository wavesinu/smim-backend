package com.smim.backend.domain.article.dto;

import com.smim.backend.domain.user.CefrLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ArticleDifficultyResponse {
    private Long articleId;
    private CefrLevel cefrLevel;
    private Double averageWordDifficulty;
    private Double complexSentenceRatio;
    private Instant analyzedAt;
}
