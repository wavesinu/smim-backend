package com.smim.backend.domain.learning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class QuizSubmitRequest {
    @Valid
    @NotEmpty(message = "answers는 비어 있을 수 없습니다.")
    private List<QuizAnswerRequest> answers;
}
