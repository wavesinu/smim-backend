package com.smim.backend.domain.learning.dto;

import com.smim.backend.domain.learning.QuizType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizQuestionResponse {
    private int questionId;
    private QuizType questionType;
    private String questionText;
    private List<String> options;
    private Long entryId;
}
