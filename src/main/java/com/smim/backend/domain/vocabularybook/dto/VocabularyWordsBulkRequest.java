package com.smim.backend.domain.vocabularybook.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VocabularyWordsBulkRequest {

    @NotNull(message = "articleId는 필수입니다.")
    private Long articleId;
}
