package com.smim.backend.domain.article;

import com.smim.backend.domain.common.BaseEntity;
import com.smim.backend.domain.user.CefrLevel;
import com.smim.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 아티클 엔티티
 * 사용자가 URL을 통해 생성했거나 읽은 아티클 정보를 저장합니다.
 * 원문 내용과 Gemini가 요약/가공한 내용을 포함합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "articles",
       indexes = @Index(name = "idx_article_user_created", columnList = "user_id, created_at"))
public class Article extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 2000)
    private String originalUrl;

    @Column(length = 100)
    private String sourceDomain;

    @Column(name = "is_completed")
    private boolean isCompleted;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private CefrLevel cefrLevel;

    private Double averageWordDifficulty;

    private Double complexSentenceRatio;

    private Instant analyzedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExtractionStatus extractionStatus;

    private Instant extractionStartedAt;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArticleVocabulary> vocabularyList = new ArrayList<>();

    @Builder
    public Article(User user, String title, String content, String originalUrl, String sourceDomain) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.originalUrl = originalUrl;
        this.sourceDomain = sourceDomain;
        this.isCompleted = false;
        this.extractionStatus = ExtractionStatus.EXTRACTING;
        this.extractionStartedAt = Instant.now();
    }

    /**
     * 아티클 읽기 완료 처리
     */
    public void markAsCompleted() {
        this.isCompleted = true;
        this.extractionStatus = ExtractionStatus.COMPLETED;
    }

    public void markExtractionFailed() {
        this.extractionStatus = ExtractionStatus.FAILED;
    }

    public void startExtraction() {
        this.isCompleted = false;
        this.extractionStatus = ExtractionStatus.EXTRACTING;
        this.extractionStartedAt = Instant.now();
        this.vocabularyList.clear();
    }

    /**
     * AI가 추출한 단어 목록 업데이트
     * @param vocabularies 새로운 단어 목록
     */
    public void updateVocabulary(List<ArticleVocabulary> vocabularies) {
        this.vocabularyList.clear();
        this.vocabularyList.addAll(vocabularies);
        vocabularies.forEach(v -> v.setArticle(this));
    }

    /**
     * 단어 추가
     * @param vocabulary 추가할 단어
     */
    public void addVocabulary(ArticleVocabulary vocabulary) {
        this.vocabularyList.add(vocabulary);
        vocabulary.setArticle(this);
    }

    public void updateDifficulty(CefrLevel cefrLevel, Double averageWordDifficulty, Double complexSentenceRatio, Instant analyzedAt) {
        this.cefrLevel = cefrLevel;
        this.averageWordDifficulty = averageWordDifficulty;
        this.complexSentenceRatio = complexSentenceRatio;
        this.analyzedAt = analyzedAt;
    }
}
