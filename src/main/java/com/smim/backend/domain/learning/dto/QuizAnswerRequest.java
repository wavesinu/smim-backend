package com.smim.backend.domain.learning.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuizAnswerRequest {
    @Min(value = 1, message = "questionId는 1 이상이어야 합니다.")
    private int questionId;

    @Min(value = 0, message = "selectedIndex는 0 이상이어야 합니다.")
    private int selectedIndex;
}
