package com.smim.backend.domain.learning;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class QuizSession {
    private String sessionId;
    private Long userId;
    private Long bookId;
    private Instant createdAt;
    private List<QuizQuestion> questions;
}
