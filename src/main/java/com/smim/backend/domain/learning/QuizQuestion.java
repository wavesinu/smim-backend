package com.smim.backend.domain.learning;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizQuestion {
    private int questionId;
    private Long entryId;
    private QuizType quizType;
    private String questionText;
    private List<String> options;
    private int correctAnswerIndex;
}
