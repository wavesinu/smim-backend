package com.smim.backend.domain.article;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아티클 추출 단어 엔티티
 * AI가 아티클에서 자동으로 추출한 핵심 단어 목록입니다.
 * 사용자의 '내 단어장'과는 별도로, 해당 아티클의 문맥적 힌트를 제공하는 용도입니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "article_vocabularies")
public class ArticleVocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(nullable = false, length = 500)
    private String definition;

    @Column(columnDefinition = "TEXT")
    private String contextSentence;

    @Builder
    public ArticleVocabulary(String word, String definition, String contextSentence) {
        this.word = word;
        this.definition = definition;
        this.contextSentence = contextSentence;
    }

    /**
     * Article과의 연관관계 설정
     * Article.addVocabulary() 또는 Article.updateVocabulary()에서 호출됩니다.
     * @param article 연관된 아티클
     */
    void setArticle(Article article) {
        this.article = article;
    }
}
