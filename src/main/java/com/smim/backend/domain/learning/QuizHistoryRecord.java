package com.smim.backend.domain.learning;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class QuizHistoryRecord {
    private String sessionId;
    private Long userId;
    private Long bookId;
    private int correctCount;
    private int totalCount;
    private double accuracy;
    private Instant submittedAt;
}
