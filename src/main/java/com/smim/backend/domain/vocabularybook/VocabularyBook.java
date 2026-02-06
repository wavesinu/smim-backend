package com.smim.backend.domain.vocabularybook;

import com.smim.backend.domain.common.BaseEntity;
import com.smim.backend.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 단어장 엔티티 (기본 단어장 포함)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vocabulary_books",
       uniqueConstraints = @UniqueConstraint(name = "uk_user_book_name", columnNames = {"user_id", "name"}),
       indexes = {
               @Index(name = "idx_vocabulary_books_user_id", columnList = "user_id")
       })
public class VocabularyBook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "word_count", nullable = false)
    private int wordCount;

    @Builder
    public VocabularyBook(User user, String name, String description, boolean isDefault, int wordCount) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.isDefault = isDefault;
        this.wordCount = wordCount;
    }
}
