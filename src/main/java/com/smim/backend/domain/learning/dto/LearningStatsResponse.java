package com.smim.backend.domain.learning.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LearningStatsResponse {
    private long totalWords;
    private long masteredWords;
    private long learningWords;
    private long newWords;
    private double averageAccuracy;
    private int totalQuizzesTaken;
    private int studyStreak;
}
