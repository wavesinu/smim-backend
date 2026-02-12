package com.smim.backend.domain.vocabularybook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VocabularyWordManualCreateRequest {

    @NotBlank(message = "word는 필수입니다.")
    @Size(max = 100, message = "word는 100자를 초과할 수 없습니다.")
    private String word;

    @NotBlank(message = "definition은 필수입니다.")
    @Size(max = 500, message = "definition은 500자를 초과할 수 없습니다.")
    private String definition;

    @Size(max = 5000, message = "contextSentence는 5000자를 초과할 수 없습니다.")
    private String contextSentence;
}
