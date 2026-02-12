package com.smim.backend.domain.learning;

import com.smim.backend.domain.learning.dto.LearningStatsResponse;
import com.smim.backend.domain.learning.dto.LearningPeriodStatsResponse;
import com.smim.backend.domain.learning.dto.ReviewWordResponse;
import com.smim.backend.domain.learning.dto.ReviewWordsResponse;
import com.smim.backend.domain.vocabularybook.LearningStatus;
import com.smim.backend.domain.vocabularybook.VocabularyEntry;
import com.smim.backend.domain.vocabularybook.VocabularyEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningService {

    private final VocabularyEntryRepository vocabularyEntryRepository;
    private final LearningStatsService learningStatsService;
    private final QuizHistoryStore quizHistoryStore;

    public ReviewWordsResponse getReviewWords(Long userId, int limit, Long bookId) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<VocabularyEntry> entries = vocabularyEntryRepository.findReviewEntries(
                userId,
                bookId,
                Instant.now(),
                pageRequest
        );
        List<ReviewWordResponse> reviewWords = entries.stream()
                .map(ReviewWordResponse::from)
                .toList();
        return ReviewWordsResponse.builder()
                .reviewWords(reviewWords)
                .totalCount(reviewWords.size())
                .build();
    }

    public LearningStatsResponse getLearningStats(Long userId) {
        long totalWords = vocabularyEntryRepository.countByVocabularyBookUserId(userId);
        long masteredWords = vocabularyEntryRepository.countByVocabularyBookUserIdAndLearningStatus(userId, LearningStatus.MASTERED);
        long learningWords = vocabularyEntryRepository.countByVocabularyBookUserIdAndLearningStatus(userId, LearningStatus.LEARNING)
                + vocabularyEntryRepository.countByVocabularyBookUserIdAndLearningStatus(userId, LearningStatus.REVIEWING);
        long newWords = vocabularyEntryRepository.countByVocabularyBookUserIdAndLearningStatus(userId, LearningStatus.NEW);

        LearningStats stats = learningStatsService.getStats(userId);
        double accuracy = stats.getTotalQuestions() == 0 ? 0.0 : (double) stats.getTotalCorrect() / stats.getTotalQuestions();

        return LearningStatsResponse.builder()
                .totalWords(totalWords)
                .masteredWords(masteredWords)
                .learningWords(learningWords)
                .newWords(newWords)
                .averageAccuracy(accuracy)
                .totalQuizzesTaken(stats.getTotalQuizzesTaken())
                .studyStreak(stats.getStudyStreak())
                .build();
    }

    public LearningPeriodStatsResponse getLearningStatsByPeriod(Long userId, String period, Duration window) {
        Instant now = Instant.now();
        Instant from = now.minus(window);
        List<QuizHistoryRecord> records = quizHistoryStore.getHistorySince(userId, from);

        int quizzesTaken = records.size();
        int totalQuestions = records.stream().mapToInt(QuizHistoryRecord::getTotalCount).sum();
        int correctAnswers = records.stream().mapToInt(QuizHistoryRecord::getCorrectCount).sum();
        double averageAccuracy = totalQuestions == 0 ? 0.0 : (double) correctAnswers / totalQuestions;

        return LearningPeriodStatsResponse.builder()
                .period(period)
                .from(from)
                .to(now)
                .quizzesTaken(quizzesTaken)
                .totalQuestions(totalQuestions)
                .correctAnswers(correctAnswers)
                .averageAccuracy(averageAccuracy)
                .build();
    }
}
