package com.smim.backend.domain.learning.dto;

import com.smim.backend.domain.vocabularybook.VocabularyEntry;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ReviewWordResponse {
    private Long entryId;
    private String word;
    private String definition;
    private String contextSentence;
    private int reviewCount;
    private Instant nextReviewAt;
    private String bookName;

    public static ReviewWordResponse from(VocabularyEntry entry) {
        return ReviewWordResponse.builder()
                .entryId(entry.getId())
                .word(entry.getWord())
                .definition(entry.getDefinition())
                .contextSentence(entry.getContextSentence())
                .reviewCount(entry.getReviewCount())
                .nextReviewAt(entry.getNextReviewAt())
                .bookName(entry.getVocabularyBook().getName())
                .build();
    }
}
