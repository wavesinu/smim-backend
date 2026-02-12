package com.smim.backend.domain.learning;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class LearningStats {
    private int totalQuizzesTaken;
    private int totalCorrect;
    private int totalQuestions;
    private int studyStreak;
    private LocalDate lastStudyDate;

    public static LearningStats empty() {
        return LearningStats.builder()
                .totalQuizzesTaken(0)
                .totalCorrect(0)
                .totalQuestions(0)
                .studyStreak(0)
                .lastStudyDate(null)
                .build();
    }

    public LearningStats recordQuiz(int correct, int total) {
        int updatedTotalQuizzes = totalQuizzesTaken + 1;
        int updatedCorrect = totalCorrect + correct;
        int updatedQuestions = totalQuestions + total;

        LocalDate today = LocalDate.now();
        int updatedStreak = studyStreak;
        if (lastStudyDate == null) {
            updatedStreak = 1;
        } else if (lastStudyDate.plusDays(1).equals(today)) {
            updatedStreak = studyStreak + 1;
        } else if (!lastStudyDate.equals(today)) {
            updatedStreak = 1;
        }

        return LearningStats.builder()
                .totalQuizzesTaken(updatedTotalQuizzes)
                .totalCorrect(updatedCorrect)
                .totalQuestions(updatedQuestions)
                .studyStreak(updatedStreak)
                .lastStudyDate(today)
                .build();
    }
}
