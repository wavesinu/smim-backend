package com.smim.backend.domain.vocabularybook.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VocabularyEntryMoveResponse {
    private int movedCount;
    private int duplicateCount;
    private Long sourceBookId;
    private Long targetBookId;
}
