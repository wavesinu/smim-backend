package com.smim.backend.domain.learning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuizHistoryStore 테스트")
class QuizHistoryStoreTest {

    private final QuizHistoryStore quizHistoryStore = new QuizHistoryStore();

    @Test
    @DisplayName("book 기준 히스토리 조회 시 limit 적용")
    void getBookHistory_appliesLimit() {
        quizHistoryStore.record(record("s1", 1L, 10L, Instant.now().minusSeconds(10)));
        quizHistoryStore.record(record("s2", 1L, 10L, Instant.now().minusSeconds(5)));
        quizHistoryStore.record(record("s3", 1L, 10L, Instant.now()));

        List<QuizHistoryRecord> history = quizHistoryStore.getBookHistory(1L, 10L, 2);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getSessionId()).isEqualTo("s3");
        assertThat(history.get(1).getSessionId()).isEqualTo("s2");
    }

    @Test
    @DisplayName("기간 히스토리 조회는 since 이후 데이터만 반환")
    void getHistorySince_filtersByTimestamp() {
        Instant now = Instant.now();
        quizHistoryStore.record(record("old", 2L, 20L, now.minusSeconds(3600)));
        quizHistoryStore.record(record("new", 2L, 20L, now.minusSeconds(60)));

        List<QuizHistoryRecord> history = quizHistoryStore.getHistorySince(2L, now.minusSeconds(300));

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getSessionId()).isEqualTo("new");
    }

    private QuizHistoryRecord record(String sessionId, Long userId, Long bookId, Instant submittedAt) {
        return QuizHistoryRecord.builder()
                .sessionId(sessionId)
                .userId(userId)
                .bookId(bookId)
                .correctCount(1)
                .totalCount(1)
                .accuracy(1.0)
                .submittedAt(submittedAt)
                .build();
    }
}
