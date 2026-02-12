package com.smim.backend.domain.learning.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizHistoryResponse {
    private Long bookId;
    private int totalCount;
    private List<QuizHistoryItemResponse> quizzes;
}
