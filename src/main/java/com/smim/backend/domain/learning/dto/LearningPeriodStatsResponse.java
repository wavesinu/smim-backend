package com.smim.backend.domain.learning.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class LearningPeriodStatsResponse {
    private String period;
    private Instant from;
    private Instant to;
    private int quizzesTaken;
    private int totalQuestions;
    private int correctAnswers;
    private double averageAccuracy;
}
