package com.smim.backend.domain.article.service;

import com.smim.backend.domain.article.Article;
import com.smim.backend.domain.user.CefrLevel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class ArticleDifficultyAnalyzer {

    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z]+");
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("[.!?]+");

    public void updateDifficulty(Article article) {
        String content = Optional.ofNullable(article.getContent()).orElse("");
        List<String> words = extractWords(content);
        double averageWordLength = words.isEmpty()
                ? 0.0
                : words.stream().mapToInt(String::length).average().orElse(0.0);

        double averageWordDifficulty = mapLengthToDifficulty(averageWordLength);
        double complexSentenceRatio = calculateComplexSentenceRatio(content);
        CefrLevel cefrLevel = mapDifficultyToCefr(averageWordDifficulty, complexSentenceRatio);

        article.updateDifficulty(
                cefrLevel,
                averageWordDifficulty,
                complexSentenceRatio,
                Instant.now()
        );
    }

    private List<String> extractWords(String content) {
        List<String> results = new ArrayList<>();
        var matcher = WORD_PATTERN.matcher(content);
        while (matcher.find()) {
            results.add(matcher.group().toLowerCase(Locale.ENGLISH));
        }
        return results;
    }

    private double mapLengthToDifficulty(double averageLength) {
        if (averageLength <= 4) {
            return 1.5;
        }
        if (averageLength <= 5) {
            return 2.5;
        }
        if (averageLength <= 6) {
            return 3.2;
        }
        if (averageLength <= 7) {
            return 4.0;
        }
        return 4.8;
    }

    private double calculateComplexSentenceRatio(String content) {
        String[] sentences = SENTENCE_SPLIT_PATTERN.split(content);
        if (sentences.length == 0) {
            return 0.0;
        }

        int complexCount = 0;
        int sentenceCount = 0;
        for (String sentence : sentences) {
            List<String> words = extractWords(sentence);
            if (words.isEmpty()) {
                continue;
            }
            sentenceCount++;
            if (words.size() >= 20) {
                complexCount++;
            }
        }
        if (sentenceCount == 0) {
            return 0.0;
        }
        return (double) complexCount / sentenceCount;
    }

    private CefrLevel mapDifficultyToCefr(double avgDifficulty, double complexRatio) {
        double score = avgDifficulty + complexRatio * 2;
        if (score <= 1.8) {
            return CefrLevel.A1;
        }
        if (score <= 2.6) {
            return CefrLevel.A2;
        }
        if (score <= 3.3) {
            return CefrLevel.B1;
        }
        if (score <= 4.0) {
            return CefrLevel.B2;
        }
        if (score <= 4.5) {
            return CefrLevel.C1;
        }
        return CefrLevel.C2;
    }
}
