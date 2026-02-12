package com.smim.backend.domain.learning.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizGenerateResponse {
    private String quizSessionId;
    private List<QuizQuestionResponse> questions;
    private int totalQuestions;
}
