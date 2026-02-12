package com.smim.backend.domain.learning;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class QuizHistoryStore {

    private static final int MAX_HISTORY_PER_USER = 200;

    private final ConcurrentHashMap<Long, Deque<QuizHistoryRecord>> histories = new ConcurrentHashMap<>();

    public void record(QuizHistoryRecord record) {
        Deque<QuizHistoryRecord> history = histories.computeIfAbsent(record.getUserId(), ignored -> new ConcurrentLinkedDeque<>());
        history.addFirst(record);
        while (history.size() > MAX_HISTORY_PER_USER) {
            history.pollLast();
        }
    }

    public List<QuizHistoryRecord> getBookHistory(Long userId, Long bookId, int limit) {
        Deque<QuizHistoryRecord> history = histories.get(userId);
        if (history == null || limit <= 0) {
            return List.of();
        }
        List<QuizHistoryRecord> result = new ArrayList<>();
        for (QuizHistoryRecord record : history) {
            if (!Objects.equals(record.getBookId(), bookId)) {
                continue;
            }
            result.add(record);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    public List<QuizHistoryRecord> getHistorySince(Long userId, Instant since) {
        Deque<QuizHistoryRecord> history = histories.get(userId);
        if (history == null) {
            return List.of();
        }
        return history.stream()
                .filter(record -> !record.getSubmittedAt().isBefore(since))
                .toList();
    }
}
