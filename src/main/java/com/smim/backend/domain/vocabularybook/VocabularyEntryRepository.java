package com.smim.backend.domain.vocabularybook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VocabularyEntryRepository extends JpaRepository<VocabularyEntry, Long> {

    Page<VocabularyEntry> findByVocabularyBookIdAndVocabularyBookUserId(Long bookId, Long userId, Pageable pageable);

    List<VocabularyEntry> findByVocabularyBookIdAndVocabularyBookUserId(Long bookId, Long userId);

    Page<VocabularyEntry> findByVocabularyBookIdAndVocabularyBookUserIdAndWordContainingIgnoreCase(
            Long bookId,
            Long userId,
            String keyword,
            Pageable pageable
    );

    Optional<VocabularyEntry> findByIdAndVocabularyBookIdAndVocabularyBookUserId(Long entryId, Long bookId, Long userId);

    List<VocabularyEntry> findByIdInAndVocabularyBookIdAndVocabularyBookUserId(List<Long> entryIds, Long bookId, Long userId);

    boolean existsByVocabularyBookIdAndWordIgnoreCase(Long bookId, String word);

    long countByVocabularyBookUserId(Long userId);

    long countByVocabularyBookUserIdAndLearningStatus(Long userId, LearningStatus status);

    @Query("SELECT e FROM VocabularyEntry e WHERE e.vocabularyBook.user.id = :userId " +
            "AND (:bookId IS NULL OR e.vocabularyBook.id = :bookId) " +
            "AND (e.nextReviewAt IS NOT NULL AND e.nextReviewAt <= :now) " +
            "ORDER BY e.nextReviewAt ASC")
    List<VocabularyEntry> findReviewEntries(@Param("userId") Long userId,
                                            @Param("bookId") Long bookId,
                                            @Param("now") Instant now,
                                            Pageable pageable);
}
