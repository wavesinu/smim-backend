package com.smim.backend.domain.vocabularybook;

import com.smim.backend.domain.article.Article;
import com.smim.backend.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vocabulary_entries",
        indexes = {
                @Index(name = "idx_vocabulary_entry_book", columnList = "vocabulary_book_id"),
                @Index(name = "idx_vocabulary_entry_word", columnList = "word")
        })
public class VocabularyEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_book_id", nullable = false)
    private VocabularyBook vocabularyBook;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(nullable = false, length = 500)
    private String definition;

    @Column(columnDefinition = "TEXT")
    private String contextSentence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_article_id")
    private Article sourceArticle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearningStatus learningStatus;

    @Column(nullable = false)
    private int reviewCount;

    @Column(nullable = false)
    private int reviewInterval;

    private Instant nextReviewAt;

    private Instant lastReviewedAt;

    @Column(nullable = false)
    private int correctCount;

    @Column(nullable = false)
    private int incorrectCount;

    @Builder
    public VocabularyEntry(
            VocabularyBook vocabularyBook,
            String word,
            String definition,
            String contextSentence,
            Article sourceArticle,
            LearningStatus learningStatus,
            Integer reviewCount,
            Integer reviewInterval,
            Instant nextReviewAt,
            Instant lastReviewedAt,
            Integer correctCount,
            Integer incorrectCount
    ) {
        this.vocabularyBook = vocabularyBook;
        this.word = word;
        this.definition = definition;
        this.contextSentence = contextSentence;
        this.sourceArticle = sourceArticle;
        this.learningStatus = learningStatus == null ? LearningStatus.NEW : learningStatus;
        this.reviewCount = reviewCount == null ? 0 : reviewCount;
        this.reviewInterval = reviewInterval == null ? 1 : reviewInterval;
        this.nextReviewAt = nextReviewAt == null ? Instant.now() : nextReviewAt;
        this.lastReviewedAt = lastReviewedAt;
        this.correctCount = correctCount == null ? 0 : correctCount;
        this.incorrectCount = incorrectCount == null ? 0 : incorrectCount;
    }

    public void updateLearningStatus(LearningStatus status) {
        if (status != null) {
            this.learningStatus = status;
        }
    }

    public void updateVocabularyBook(VocabularyBook vocabularyBook) {
        this.vocabularyBook = vocabularyBook;
    }

    public void applyReviewResult(boolean correct) {
        this.lastReviewedAt = Instant.now();
        if (correct) {
            this.correctCount++;
            this.reviewCount++;
            this.reviewInterval = Math.min(this.reviewInterval * 2, 60);
            this.learningStatus = this.learningStatus == LearningStatus.NEW
                    ? LearningStatus.LEARNING
                    : this.learningStatus;
        } else {
            this.incorrectCount++;
            this.reviewInterval = 1;
            this.learningStatus = LearningStatus.LEARNING;
        }
        this.nextReviewAt = Instant.now().plusSeconds(this.reviewInterval * 86400L);
        if (this.reviewCount >= 5 && correct) {
            this.learningStatus = LearningStatus.MASTERED;
        } else if (this.learningStatus == LearningStatus.LEARNING && this.reviewCount >= 2) {
            this.learningStatus = LearningStatus.REVIEWING;
        }
    }
}
