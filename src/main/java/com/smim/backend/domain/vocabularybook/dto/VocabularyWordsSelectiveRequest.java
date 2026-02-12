package com.smim.backend.domain.vocabularybook.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class VocabularyWordsSelectiveRequest {

    @NotNull(message = "articleId는 필수입니다.")
    private Long articleId;

    @NotEmpty(message = "vocabularyIds는 최소 1개 이상이어야 합니다.")
    private List<Long> vocabularyIds;
}
