package com.smim.backend.domain.learning;

import com.smim.backend.domain.learning.dto.LearningPeriodStatsResponse;
import com.smim.backend.domain.vocabularybook.VocabularyEntryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("LearningService 테스트")
class LearningServiceTest {

    @InjectMocks
    private LearningService learningService;

    @Mock
    private VocabularyEntryRepository vocabularyEntryRepository;

    @Mock
    private LearningStatsService learningStatsService;

    @Mock
    private QuizHistoryStore quizHistoryStore;

    @Test
    @DisplayName("기간별 학습 통계 계산 성공")
    void getLearningStatsByPeriod_success() {
        given(quizHistoryStore.getHistorySince(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(Instant.class)))
                .willReturn(List.of(
                        QuizHistoryRecord.builder().correctCount(8).totalCount(10).submittedAt(Instant.now()).build(),
                        QuizHistoryRecord.builder().correctCount(6).totalCount(10).submittedAt(Instant.now()).build()
                ));

        LearningPeriodStatsResponse response = learningService.getLearningStatsByPeriod(1L, "WEEKLY", Duration.ofDays(7));

        assertThat(response.getPeriod()).isEqualTo("WEEKLY");
        assertThat(response.getQuizzesTaken()).isEqualTo(2);
        assertThat(response.getTotalQuestions()).isEqualTo(20);
        assertThat(response.getCorrectAnswers()).isEqualTo(14);
        assertThat(response.getAverageAccuracy()).isEqualTo(0.7);
    }
}
