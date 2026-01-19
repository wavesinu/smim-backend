package com.smim.backend.domain.article;

import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Article 엔티티 단위 테스트
 */
class ArticleTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .name("Test User")
                .profileImage("https://example.com/image.jpg")
                .provider(Provider.KAKAO)
                .providerId("12345")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("Article 생성 테스트")
    class ArticleCreationTest {

        @Test
        @DisplayName("Builder 패턴으로 Article 생성")
        void createArticleWithBuilder() {
            // given & when
            Article article = Article.builder()
                    .user(testUser)
                    .title("Test Article Title")
                    .content("This is the article content.")
                    .originalUrl("https://example.com/article")
                    .sourceDomain("example.com")
                    .build();

            // then
            assertThat(article.getUser()).isEqualTo(testUser);
            assertThat(article.getTitle()).isEqualTo("Test Article Title");
            assertThat(article.getContent()).isEqualTo("This is the article content.");
            assertThat(article.getOriginalUrl()).isEqualTo("https://example.com/article");
            assertThat(article.getSourceDomain()).isEqualTo("example.com");
            assertThat(article.isCompleted()).isFalse();
            assertThat(article.getVocabularyList()).isEmpty();
        }

        @Test
        @DisplayName("필수 필드만으로 Article 생성")
        void createArticleWithRequiredFieldsOnly() {
            // given & when
            Article article = Article.builder()
                    .user(testUser)
                    .title("Minimal Article")
                    .content("Minimal content")
                    .build();

            // then
            assertThat(article.getTitle()).isEqualTo("Minimal Article");
            assertThat(article.getContent()).isEqualTo("Minimal content");
            assertThat(article.getOriginalUrl()).isNull();
            assertThat(article.getSourceDomain()).isNull();
        }
    }

    @Nested
    @DisplayName("Article 상태 변경 테스트")
    class ArticleStateChangeTest {

        @Test
        @DisplayName("읽기 완료 상태로 변경")
        void markAsCompleted() {
            // given
            Article article = Article.builder()
                    .user(testUser)
                    .title("Test Article")
                    .content("Content")
                    .build();
            assertThat(article.isCompleted()).isFalse();

            // when
            article.markAsCompleted();

            // then
            assertThat(article.isCompleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("ArticleVocabulary 관계 테스트")
    class VocabularyRelationTest {

        @Test
        @DisplayName("단어 추가")
        void addVocabulary() {
            // given
            Article article = Article.builder()
                    .user(testUser)
                    .title("Test Article")
                    .content("Content")
                    .build();

            ArticleVocabulary vocabulary = ArticleVocabulary.builder()
                    .word("vocabulary")
                    .definition("a list or collection of words")
                    .contextSentence("Building your vocabulary is important.")
                    .build();

            // when
            article.addVocabulary(vocabulary);

            // then
            assertThat(article.getVocabularyList()).hasSize(1);
            assertThat(article.getVocabularyList().get(0)).isEqualTo(vocabulary);
            assertThat(vocabulary.getArticle()).isEqualTo(article);
        }

        @Test
        @DisplayName("단어 목록 업데이트")
        void updateVocabulary() {
            // given
            Article article = Article.builder()
                    .user(testUser)
                    .title("Test Article")
                    .content("Content")
                    .build();

            ArticleVocabulary vocab1 = ArticleVocabulary.builder()
                    .word("word1")
                    .definition("definition1")
                    .build();
            article.addVocabulary(vocab1);

            List<ArticleVocabulary> newVocabs = List.of(
                    ArticleVocabulary.builder()
                            .word("newWord1")
                            .definition("newDefinition1")
                            .build(),
                    ArticleVocabulary.builder()
                            .word("newWord2")
                            .definition("newDefinition2")
                            .build()
            );

            // when
            article.updateVocabulary(newVocabs);

            // then
            assertThat(article.getVocabularyList()).hasSize(2);
            assertThat(article.getVocabularyList())
                    .extracting(ArticleVocabulary::getWord)
                    .containsExactly("newWord1", "newWord2");
            assertThat(article.getVocabularyList())
                    .allSatisfy(v -> assertThat(v.getArticle()).isEqualTo(article));
        }
    }
}
