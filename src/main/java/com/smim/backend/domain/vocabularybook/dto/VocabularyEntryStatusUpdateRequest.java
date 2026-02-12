package com.smim.backend.domain.vocabularybook.dto;

import com.smim.backend.domain.vocabularybook.LearningStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VocabularyEntryStatusUpdateRequest {

    @NotNull(message = "learningStatus는 필수입니다.")
    private LearningStatus learningStatus;
}
