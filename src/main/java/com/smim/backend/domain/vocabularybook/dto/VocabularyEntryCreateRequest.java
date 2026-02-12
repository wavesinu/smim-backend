package com.smim.backend.domain.vocabularybook.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class VocabularyEntryCreateRequest {
    @NotNull(message = "articleId는 필수입니다.")
    private Long articleId;
    private List<Long> vocabularyIds;
    private Boolean saveAll;
}
