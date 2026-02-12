package com.smim.backend.domain.vocabularybook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VocabularyBookCreateRequest {

    @NotBlank(message = "단어장 이름은 필수입니다.")
    @Size(max = 50, message = "단어장 이름은 50자 이하여야 합니다.")
    private String name;

    @Size(max = 200, message = "단어장 설명은 200자 이하여야 합니다.")
    private String description;
}
