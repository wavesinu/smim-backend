package com.smim.backend.domain.article.dto;

import com.smim.backend.domain.article.ArticleVocabulary;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 아티클 단어 응답 DTO
 */
@Getter
@AllArgsConstructor
public class ArticleVocabularyResponse {

    private Long id;
    private String word;
    private String definition;
    private String contextSentence;

    /**
     * ArticleVocabulary 엔티티를 DTO로 변환
     * @param vocabulary 아티클 단어 엔티티
     * @return 아티클 단어 응답 DTO
     */
    public static ArticleVocabularyResponse from(ArticleVocabulary vocabulary) {
        return new ArticleVocabularyResponse(
            vocabulary.getId(),
            vocabulary.getWord(),
            vocabulary.getDefinition(),
            vocabulary.getContextSentence()
        );
    }
}
