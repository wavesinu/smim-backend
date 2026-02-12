package com.smim.backend.domain.learning.dto;

import com.smim.backend.domain.learning.QuizHistoryRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class QuizHistoryItemResponse {
    private String quizSessionId;
    private int correctCount;
    private int totalCount;
    private double accuracy;
    private Instant submittedAt;

    public static QuizHistoryItemResponse from(QuizHistoryRecord record) {
        return QuizHistoryItemResponse.builder()
                .quizSessionId(record.getSessionId())
                .correctCount(record.getCorrectCount())
                .totalCount(record.getTotalCount())
                .accuracy(record.getAccuracy())
                .submittedAt(record.getSubmittedAt())
                .build();
    }
}
