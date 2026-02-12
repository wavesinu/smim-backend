package com.smim.backend.domain.vocabularybook.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VocabularyEntrySaveResponse {
    private int savedCount;
    private int duplicateCount;
    private Long bookId;
    private String bookName;
}
