package com.smim.backend.domain.vocabularybook.service;

import com.smim.backend.domain.article.Article;
import com.smim.backend.domain.article.ArticleRepository;
import com.smim.backend.domain.article.ArticleVocabulary;
import com.smim.backend.domain.article.ArticleVocabularyRepository;
import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.domain.vocabularybook.VocabularyBook;
import com.smim.backend.domain.vocabularybook.VocabularyBookRepository;
import com.smim.backend.domain.vocabularybook.VocabularyEntry;
import com.smim.backend.domain.vocabularybook.VocabularyEntryRepository;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntrySaveResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyWordManualCreateRequest;
import com.smim.backend.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("VocabularyBookService 테스트")
class VocabularyBookServiceTest {

    @InjectMocks
    private VocabularyBookService vocabularyBookService;

    @Mock
    private VocabularyBookRepository vocabularyBookRepository;

    @Mock
    private VocabularyEntryRepository vocabularyEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleVocabularyRepository articleVocabularyRepository;

    @Test
    @DisplayName("선택 단어 저장 성공")
    void addSelectiveWords_success() {
        User user = user(1L);
        VocabularyBook book = book(10L, user);
        Article article = completedArticle(100L, user);
        ArticleVocabulary av1 = articleVocabulary(1000L, article, "word-one");

        given(vocabularyBookRepository.findById(10L)).willReturn(Optional.of(book));
        given(articleRepository.findById(100L)).willReturn(Optional.of(article));
        given(articleVocabularyRepository.findAllById(List.of(1000L))).willReturn(List.of(av1));
        given(vocabularyEntryRepository.existsByVocabularyBookIdAndWordIgnoreCase(10L, "word-one")).willReturn(false);

        VocabularyEntrySaveResponse response = vocabularyBookService.addSelectiveWords(
                1L,
                10L,
                100L,
                List.of(1000L)
        );

        assertThat(response.getSavedCount()).isEqualTo(1);
        assertThat(response.getDuplicateCount()).isEqualTo(0);
        assertThat(book.getWordCount()).isEqualTo(1);
        verify(vocabularyEntryRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("전체 단어 저장 시 중복 단어는 제외")
    void addBulkWords_withDuplicate() {
        User user = user(1L);
        VocabularyBook book = book(20L, user);
        Article article = completedArticle(200L, user);
        ArticleVocabulary av1 = articleVocabulary(2001L, article, "duplicate");
        ArticleVocabulary av2 = articleVocabulary(2002L, article, "fresh");

        given(vocabularyBookRepository.findById(20L)).willReturn(Optional.of(book));
        given(articleRepository.findById(200L)).willReturn(Optional.of(article));
        given(articleVocabularyRepository.findByArticleId(200L)).willReturn(List.of(av1, av2));
        given(vocabularyEntryRepository.existsByVocabularyBookIdAndWordIgnoreCase(20L, "duplicate"))
                .willReturn(true);
        given(vocabularyEntryRepository.existsByVocabularyBookIdAndWordIgnoreCase(20L, "fresh"))
                .willReturn(false);

        VocabularyEntrySaveResponse response = vocabularyBookService.addBulkWords(1L, 20L, 200L);

        assertThat(response.getSavedCount()).isEqualTo(1);
        assertThat(response.getDuplicateCount()).isEqualTo(1);
        assertThat(book.getWordCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("수동 단어 추가 성공")
    void addManualWord_success() {
        User user = user(1L);
        VocabularyBook book = book(30L, user);
        VocabularyWordManualCreateRequest request = manualRequest("  resilient  ", "able to recover", "She is resilient.");

        given(vocabularyBookRepository.findById(30L)).willReturn(Optional.of(book));
        given(vocabularyEntryRepository.existsByVocabularyBookIdAndWordIgnoreCase(30L, "resilient"))
                .willReturn(false);
        given(vocabularyEntryRepository.save(org.mockito.ArgumentMatchers.any(VocabularyEntry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        VocabularyEntryResponse response = vocabularyBookService.addManualWord(1L, 30L, request);

        assertThat(response.getWord()).isEqualTo("resilient");
        assertThat(response.getDefinition()).isEqualTo("able to recover");
        assertThat(book.getWordCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("수동 단어 추가 시 중복 단어면 예외")
    void addManualWord_duplicate_throwsException() {
        User user = user(1L);
        VocabularyBook book = book(40L, user);
        VocabularyWordManualCreateRequest request = manualRequest("resilient", "able to recover", null);

        given(vocabularyBookRepository.findById(40L)).willReturn(Optional.of(book));
        given(vocabularyEntryRepository.existsByVocabularyBookIdAndWordIgnoreCase(40L, "resilient"))
                .willReturn(true);

        assertThatThrownBy(() -> vocabularyBookService.addManualWord(1L, 40L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 단어장에 존재하는 단어");
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

    private VocabularyBook book(Long id, User user) {
        VocabularyBook book = VocabularyBook.builder()
                .user(user)
                .name("기본 단어장")
                .isDefault(false)
                .wordCount(0)
                .build();
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private Article completedArticle(Long id, User user) {
        Article article = Article.builder()
                .user(user)
                .title("Article")
                .content("content")
                .originalUrl("https://example.com")
                .sourceDomain("example.com")
                .build();
        article.markAsCompleted();
        ReflectionTestUtils.setField(article, "id", id);
        return article;
    }

    private ArticleVocabulary articleVocabulary(Long id, Article article, String word) {
        ArticleVocabulary vocabulary = ArticleVocabulary.builder()
                .word(word)
                .definition(word + " definition")
                .contextSentence("context")
                .build();
        ReflectionTestUtils.setField(vocabulary, "id", id);
        ReflectionTestUtils.setField(vocabulary, "article", article);
        return vocabulary;
    }

    private VocabularyWordManualCreateRequest manualRequest(String word, String definition, String context) {
        VocabularyWordManualCreateRequest request = new VocabularyWordManualCreateRequest();
        ReflectionTestUtils.setField(request, "word", word);
        ReflectionTestUtils.setField(request, "definition", definition);
        ReflectionTestUtils.setField(request, "contextSentence", context);
        return request;
    }
}
