package com.smim.backend.domain.vocabularybook.dto;

import com.smim.backend.domain.vocabularybook.LearningStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class VocabularyEntryStatusResponse {
    private Long entryId;
    private LearningStatus learningStatus;
    private Instant updatedAt;
}
