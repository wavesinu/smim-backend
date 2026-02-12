package com.smim.backend.domain.learning;

import com.smim.backend.domain.learning.dto.QuizAnswerRequest;
import com.smim.backend.domain.learning.dto.QuizGenerateRequest;
import com.smim.backend.domain.learning.dto.QuizGenerateResponse;
import com.smim.backend.domain.learning.dto.QuizQuestionResponse;
import com.smim.backend.domain.learning.dto.QuizResultItem;
import com.smim.backend.domain.learning.dto.QuizSubmitRequest;
import com.smim.backend.domain.learning.dto.QuizSubmitResponse;
import com.smim.backend.domain.learning.dto.UpdatedScheduleItem;
import com.smim.backend.domain.vocabularybook.VocabularyBook;
import com.smim.backend.domain.vocabularybook.VocabularyBookRepository;
import com.smim.backend.domain.vocabularybook.VocabularyEntry;
import com.smim.backend.domain.vocabularybook.VocabularyEntryRepository;
import com.smim.backend.global.error.ErrorCode;
import com.smim.backend.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private static final int DEFAULT_QUIZ_COUNT = 10;

    private final VocabularyBookRepository vocabularyBookRepository;
    private final VocabularyEntryRepository vocabularyEntryRepository;
    private final QuizSessionStore quizSessionStore;
    private final LearningStatsService learningStatsService;

    @Transactional(readOnly = true)
    public QuizGenerateResponse generateQuiz(Long userId, QuizGenerateRequest request) {
        VocabularyBook book = vocabularyBookRepository.findByIdAndUserId(request.getBookId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCABULARY_BOOK_NOT_FOUND));

        List<VocabularyEntry> allEntries = vocabularyEntryRepository
                .findByVocabularyBookIdAndVocabularyBookUserId(book.getId(), userId);

        if (allEntries.size() < 4) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        int count = request.getCount() == null ? DEFAULT_QUIZ_COUNT : request.getCount();
        count = Math.min(count, Math.min(30, allEntries.size()));

        Collections.shuffle(allEntries);
        List<VocabularyEntry> selected = allEntries.subList(0, count);

        List<String> allDefinitions = allEntries.stream()
                .map(VocabularyEntry::getDefinition)
                .distinct()
                .toList();

        List<QuizQuestion> questions = new ArrayList<>();
        List<QuizQuestionResponse> questionResponses = new ArrayList<>();

        int questionId = 1;
        for (VocabularyEntry entry : selected) {
            List<String> options = buildOptions(entry.getDefinition(), allDefinitions);
            String questionText = buildQuestionText(entry, request.getQuizType());
            int correctIndex = options.indexOf(entry.getDefinition());

            QuizQuestion question = QuizQuestion.builder()
                    .questionId(questionId)
                    .entryId(entry.getId())
                    .quizType(request.getQuizType())
                    .questionText(questionText)
                    .options(options)
                    .correctAnswerIndex(correctIndex)
                    .build();
            questions.add(question);

            questionResponses.add(QuizQuestionResponse.builder()
                    .questionId(questionId)
                    .questionType(request.getQuizType())
                    .questionText(questionText)
                    .options(options)
                    .entryId(entry.getId())
                    .build());

            questionId++;
        }

        String sessionId = UUID.randomUUID().toString();
        QuizSession session = QuizSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .bookId(book.getId())
                .createdAt(Instant.now())
                .questions(questions)
                .build();
        quizSessionStore.save(session);

        return QuizGenerateResponse.builder()
                .quizSessionId(sessionId)
                .questions(questionResponses)
                .totalQuestions(questionResponses.size())
                .build();
    }

    @Transactional
    public QuizSubmitResponse submitQuiz(Long userId, String sessionId, QuizSubmitRequest request) {
        QuizSession session = quizSessionStore.find(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (quizSessionStore.isExpired(session)) {
            quizSessionStore.remove(sessionId);
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Map<Integer, QuizQuestion> questionMap = session.getQuestions().stream()
                .collect(Collectors.toMap(QuizQuestion::getQuestionId, Function.identity()));

        List<Long> entryIds = session.getQuestions().stream()
                .map(QuizQuestion::getEntryId)
                .distinct()
                .toList();
        Map<Long, VocabularyEntry> entryMap = vocabularyEntryRepository.findAllById(entryIds).stream()
                .collect(Collectors.toMap(VocabularyEntry::getId, Function.identity()));

        int correctCount = 0;
        List<QuizResultItem> results = new ArrayList<>();
        List<UpdatedScheduleItem> updatedSchedules = new ArrayList<>();

        for (QuizAnswerRequest answer : request.getAnswers()) {
            QuizQuestion question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }

            boolean isCorrect = question.getCorrectAnswerIndex() == answer.getSelectedIndex();
            if (isCorrect) {
                correctCount++;
            }

            VocabularyEntry entry = entryMap.get(question.getEntryId());
            if (entry != null) {
                entry.applyReviewResult(isCorrect);
                updatedSchedules.add(UpdatedScheduleItem.builder()
                        .entryId(entry.getId())
                        .nextReviewAt(entry.getNextReviewAt())
                        .build());
            }

            results.add(QuizResultItem.builder()
                    .questionId(question.getQuestionId())
                    .isCorrect(isCorrect)
                    .correctAnswer(question.getCorrectAnswerIndex())
                    .userAnswer(answer.getSelectedIndex())
                    .build());
        }

        int totalCount = session.getQuestions().size();
        double accuracy = totalCount == 0 ? 0.0 : (double) correctCount / totalCount;

        learningStatsService.recordQuiz(userId, correctCount, totalCount);
        quizSessionStore.remove(sessionId);

        return QuizSubmitResponse.builder()
                .correctCount(correctCount)
                .totalCount(totalCount)
                .accuracy(accuracy)
                .results(results)
                .updatedSchedules(updatedSchedules)
                .build();
    }

    private List<String> buildOptions(String correctDefinition, List<String> allDefinitions) {
        List<String> options = new ArrayList<>();
        options.add(correctDefinition);
        List<String> candidates = allDefinitions.stream()
                .filter(def -> !def.equals(correctDefinition))
                .toList();
        Collections.shuffle(candidates);
        for (String candidate : candidates) {
            if (options.size() >= 4) {
                break;
            }
            if (!options.contains(candidate)) {
                options.add(candidate);
            }
        }
        while (options.size() < 4) {
            options.add(correctDefinition);
        }
        Collections.shuffle(options);
        return options;
    }

    private String buildQuestionText(VocabularyEntry entry, QuizType quizType) {
        return switch (quizType) {
            case FILL_IN_BLANK -> entry.getContextSentence() == null
                    ? String.format("'%s'의 의미를 맞혀보세요.", entry.getWord())
                    : entry.getContextSentence().replace(entry.getWord(), "_____");
            case MEANING_MATCH, MULTIPLE_CHOICE -> String.format("'%s'의 의미는?", entry.getWord());
        };
    }
}
