package com.smim.backend.domain.learning;

import com.smim.backend.domain.learning.dto.QuizAnswerRequest;
import com.smim.backend.domain.learning.dto.QuizHistoryResponse;
import com.smim.backend.domain.learning.dto.QuizSubmitRequest;
import com.smim.backend.domain.learning.dto.QuizSubmitResponse;
import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.vocabularybook.LearningStatus;
import com.smim.backend.domain.vocabularybook.VocabularyBook;
import com.smim.backend.domain.vocabularybook.VocabularyBookRepository;
import com.smim.backend.domain.vocabularybook.VocabularyEntry;
import com.smim.backend.domain.vocabularybook.VocabularyEntryRepository;
import com.smim.backend.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuizService 테스트")
class QuizServiceTest {

    @InjectMocks
    private QuizService quizService;

    @Mock
    private VocabularyBookRepository vocabularyBookRepository;

    @Mock
    private VocabularyEntryRepository vocabularyEntryRepository;

    @Mock
    private QuizSessionStore quizSessionStore;

    @Mock
    private QuizHistoryStore quizHistoryStore;

    @Mock
    private LearningStatsService learningStatsService;

    @Test
    @DisplayName("book 경로 submit 시 히스토리 기록")
    void submitQuiz_recordsHistory() {
        QuizSession session = QuizSession.builder()
                .sessionId("session-1")
                .userId(1L)
                .bookId(2L)
                .createdAt(Instant.now())
                .questions(List.of(QuizQuestion.builder()
                        .questionId(1)
                        .entryId(100L)
                        .quizType(QuizType.MULTIPLE_CHOICE)
                        .questionText("question")
                        .options(List.of("A", "B", "C", "D"))
                        .correctAnswerIndex(1)
                        .build()))
                .build();

        VocabularyEntry entry = VocabularyEntry.builder()
                .word("resilient")
                .definition("definition")
                .contextSentence("context")
                .learningStatus(LearningStatus.NEW)
                .build();
        ReflectionTestUtils.setField(entry, "id", 100L);

        given(quizSessionStore.find("session-1")).willReturn(Optional.of(session));
        given(quizSessionStore.isExpired(session)).willReturn(false);
        given(vocabularyEntryRepository.findAllById(List.of(100L))).willReturn(List.of(entry));

        QuizSubmitRequest request = submitRequest(1, 1);
        QuizSubmitResponse response = quizService.submitQuiz(1L, 2L, "session-1", request);

        assertThat(response.getCorrectCount()).isEqualTo(1);
        assertThat(response.getTotalCount()).isEqualTo(1);

        ArgumentCaptor<QuizHistoryRecord> recordCaptor = ArgumentCaptor.forClass(QuizHistoryRecord.class);
        verify(quizHistoryStore).record(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getBookId()).isEqualTo(2L);
        assertThat(recordCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(recordCaptor.getValue().getCorrectCount()).isEqualTo(1);
        verify(quizSessionStore).remove("session-1");
        verify(learningStatsService).recordQuiz(1L, 1, 1);
    }

    @Test
    @DisplayName("book 경로 submit 시 bookId 불일치면 예외")
    void submitQuiz_bookMismatch_throwsException() {
        QuizSession session = QuizSession.builder()
                .sessionId("session-2")
                .userId(1L)
                .bookId(5L)
                .createdAt(Instant.now())
                .questions(List.of())
                .build();
        given(quizSessionStore.find("session-2")).willReturn(Optional.of(session));
        given(quizSessionStore.isExpired(session)).willReturn(false);

        assertThatThrownBy(() -> quizService.submitQuiz(1L, 6L, "session-2", submitRequest(1, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("잘못된 요청");
    }

    @Test
    @DisplayName("quiz history 조회 성공")
    void getQuizHistory_success() {
        VocabularyBook book = VocabularyBook.builder()
                .user(user(1L))
                .name("book")
                .isDefault(false)
                .wordCount(0)
                .build();
        ReflectionTestUtils.setField(book, "id", 2L);

        given(vocabularyBookRepository.findByIdAndUserId(2L, 1L)).willReturn(Optional.of(book));
        given(quizHistoryStore.getBookHistory(1L, 2L, 20)).willReturn(List.of(
                QuizHistoryRecord.builder()
                        .sessionId("session-3")
                        .userId(1L)
                        .bookId(2L)
                        .correctCount(7)
                        .totalCount(10)
                        .accuracy(0.7)
                        .submittedAt(Instant.now())
                        .build()
        ));

        QuizHistoryResponse response = quizService.getQuizHistory(1L, 2L, 20);

        assertThat(response.getBookId()).isEqualTo(2L);
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getQuizzes().get(0).getQuizSessionId()).isEqualTo("session-3");
    }

    private QuizSubmitRequest submitRequest(int questionId, int selectedIndex) {
        QuizAnswerRequest answer = new QuizAnswerRequest();
        ReflectionTestUtils.setField(answer, "questionId", questionId);
        ReflectionTestUtils.setField(answer, "selectedIndex", selectedIndex);

        QuizSubmitRequest request = new QuizSubmitRequest();
        ReflectionTestUtils.setField(request, "answers", List.of(answer));
        return request;
    }

    private User user(Long id) {
        User user = User.builder()
                .email("user@example.com")
                .name("User")
                .provider(Provider.LOCAL)
                .providerId(null)
                .role(Role.USER)
                .password("encoded")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
