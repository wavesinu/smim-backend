package com.smim.backend.domain.vocabularybook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smim.backend.domain.vocabularybook.VocabularyBook;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class VocabularyBookResponse {
    private Long id;
    private String name;
    private String description;
    @JsonProperty("isDefault")
    private boolean isDefault;
    private int wordCount;
    private Instant createdAt;

    public static VocabularyBookResponse from(VocabularyBook book) {
        return VocabularyBookResponse.builder()
                .id(book.getId())
                .name(book.getName())
                .description(book.getDescription())
                .isDefault(book.isDefault())
                .wordCount(book.getWordCount())
                .createdAt(book.getCreatedAt())
                .build();
    }
}
