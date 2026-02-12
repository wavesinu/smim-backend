package com.smim.backend.domain.vocabularybook.dto;

import com.smim.backend.domain.vocabularybook.VocabularyEntry;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class VocabularyEntryResponse {
    private Long id;
    private String word;
    private String definition;
    private String contextSentence;
    private SourceArticleInfo sourceArticle;
    private Instant createdAt;

    public static VocabularyEntryResponse from(VocabularyEntry entry) {
        SourceArticleInfo source = entry.getSourceArticle() == null ? null : SourceArticleInfo.builder()
                .id(entry.getSourceArticle().getId())
                .title(entry.getSourceArticle().getTitle())
                .build();
        return VocabularyEntryResponse.builder()
                .id(entry.getId())
                .word(entry.getWord())
                .definition(entry.getDefinition())
                .contextSentence(entry.getContextSentence())
                .sourceArticle(source)
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
