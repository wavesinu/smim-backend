package com.smim.backend.domain.vocabularybook.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VocabularyBookUpdateRequest {

    @Size(min = 1, max = 50, message = "단어장 이름은 1자 이상 50자 이하여야 합니다.")
    private String name;

    @Size(max = 200, message = "단어장 설명은 200자 이하여야 합니다.")
    private String description;
}
