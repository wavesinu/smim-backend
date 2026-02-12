package com.smim.backend.domain.learning;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QuizSessionStore {

    private static final Duration EXPIRATION = Duration.ofMinutes(30);

    private final Map<String, QuizSession> sessions = new ConcurrentHashMap<>();

    public void save(QuizSession session) {
        sessions.put(session.getSessionId(), session);
    }

    public Optional<QuizSession> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public boolean isExpired(QuizSession session) {
        return Instant.now().isAfter(session.getCreatedAt().plus(EXPIRATION));
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }
}
