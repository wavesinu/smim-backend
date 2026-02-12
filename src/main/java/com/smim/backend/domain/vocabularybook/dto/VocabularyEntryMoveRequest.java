package com.smim.backend.domain.vocabularybook.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class VocabularyEntryMoveRequest {
    private Long targetBookId;
    private List<Long> entryIds;
}
