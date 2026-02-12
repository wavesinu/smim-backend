package com.smim.backend.domain.learning.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizResultItem {
    private int questionId;
    @JsonProperty("isCorrect")
    private boolean isCorrect;
    private int correctAnswer;
    private Integer userAnswer;
}
