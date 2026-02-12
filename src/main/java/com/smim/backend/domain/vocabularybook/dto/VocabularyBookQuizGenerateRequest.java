package com.smim.backend.domain.vocabularybook.dto;

import com.smim.backend.domain.learning.QuizType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VocabularyBookQuizGenerateRequest {

    @NotNull(message = "quizType은 필수입니다.")
    private QuizType quizType;

    @Min(value = 1, message = "count는 1 이상이어야 합니다.")
    @Max(value = 30, message = "count는 30 이하이어야 합니다.")
    private Integer count;
}
