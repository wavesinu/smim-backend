package com.smim.backend.domain.learning;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LearningStatsService {

    private final Map<Long, LearningStats> statsStore = new ConcurrentHashMap<>();

    public LearningStats getStats(Long userId) {
        return statsStore.getOrDefault(userId, LearningStats.empty());
    }

    public LearningStats recordQuiz(Long userId, int correct, int total) {
        return statsStore.compute(userId, (key, current) -> {
            LearningStats base = current == null ? LearningStats.empty() : current;
            return base.recordQuiz(correct, total);
        });
    }
}
