package com.smim.backend.domain.article;

import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ArticleRepository 통합 테스트
 * H2 인메모리 DB를 사용하여 실제 DB 저장/조회를 테스트합니다.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ArticleRepositoryTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleVocabularyRepository articleVocabularyRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .profileImage("https://example.com/image.jpg")
                .provider(Provider.KAKAO)
                .providerId("12345")
                .role(Role.USER)
                .build();
        savedUser = userRepository.save(user);
    }

    @Nested
    @DisplayName("Article 저장 테스트")
    class SaveArticleTest {

        @Test
        @DisplayName("Article 저장 성공")
        void saveArticle() {
            // given
            Article article = Article.builder()
                    .user(savedUser)
                    .title("Test Article Title")
                    .content("This is the article content.")
                    .originalUrl("https://example.com/article")
                    .sourceDomain("example.com")
                    .build();

            // when
            Article savedArticle = articleRepository.save(article);

            // then
            assertThat(savedArticle.getId()).isNotNull();
            assertThat(savedArticle.getTitle()).isEqualTo("Test Article Title");
            assertThat(savedArticle.getCreatedAt()).isNotNull();
            assertThat(savedArticle.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Article과 Vocabulary 함께 저장 (Cascade)")
        void saveArticleWithVocabulary() {
            // given
            Article article = Article.builder()
                    .user(savedUser)
                    .title("Article with Vocabulary")
                    .content("Content with vocabulary")
                    .build();

            ArticleVocabulary vocab1 = ArticleVocabulary.builder()
                    .word("ephemeral")
                    .definition("lasting for a very short time")
                    .contextSentence("The ephemeral beauty of cherry blossoms.")
                    .build();

            ArticleVocabulary vocab2 = ArticleVocabulary.builder()
                    .word("ubiquitous")
                    .definition("present, appearing, or found everywhere")
                    .contextSentence("Smartphones have become ubiquitous.")
                    .build();

            article.addVocabulary(vocab1);
            article.addVocabulary(vocab2);

            // when
            Article savedArticle = articleRepository.save(article);

            // then
            assertThat(savedArticle.getVocabularyList()).hasSize(2);
            assertThat(savedArticle.getVocabularyList())
                    .allSatisfy(v -> assertThat(v.getId()).isNotNull());
        }
    }

    @Nested
    @DisplayName("Article 조회 테스트")
    class FindArticleTest {

        @Test
        @DisplayName("ID로 Article 조회")
        void findById() {
            // given
            Article article = articleRepository.save(Article.builder()
                    .user(savedUser)
                    .title("Find By Id Test")
                    .content("Content")
                    .build());

            // when
            Optional<Article> found = articleRepository.findById(article.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Find By Id Test");
        }

        @Test
        @DisplayName("사용자별 Article 목록 조회 (최신순)")
        void findByUserOrderByCreatedAtDesc() {
            // given
            articleRepository.save(Article.builder()
                    .user(savedUser)
                    .title("Article 1")
                    .content("Content 1")
                    .build());

            articleRepository.save(Article.builder()
                    .user(savedUser)
                    .title("Article 2")
                    .content("Content 2")
                    .build());

            articleRepository.save(Article.builder()
                    .user(savedUser)
                    .title("Article 3")
                    .content("Content 3")
                    .build());

            // when
            List<Article> articles = articleRepository.findByUserOrderByCreatedAtDesc(savedUser);

            // then
            assertThat(articles).hasSize(3);
            assertThat(articles.get(0).getTitle()).isEqualTo("Article 3");
        }

        @Test
        @DisplayName("사용자별 Article 페이징 조회")
        void findByUserWithPaging() {
            // given
            for (int i = 1; i <= 15; i++) {
                articleRepository.save(Article.builder()
                        .user(savedUser)
                        .title("Article " + i)
                        .content("Content " + i)
                        .build());
            }

            // when
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Article> page = articleRepository.findByUser(savedUser, pageRequest);

            // then
            assertThat(page.getContent()).hasSize(10);
            assertThat(page.getTotalElements()).isEqualTo(15);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("Article과 Vocabulary 함께 조회 (Fetch Join)")
        void findByIdWithVocabulary() {
            // given
            Article article = Article.builder()
                    .user(savedUser)
                    .title("Article with Vocab")
                    .content("Content")
                    .build();
            article.addVocabulary(ArticleVocabulary.builder()
                    .word("test")
                    .definition("a procedure")
                    .build());
            Article savedArticle = articleRepository.save(article);

            // when
            Optional<Article> found = articleRepository.findByIdWithVocabulary(savedArticle.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getVocabularyList()).hasSize(1);
            assertThat(found.get().getVocabularyList().get(0).getWord()).isEqualTo("test");
        }

        @Test
        @DisplayName("사용자와 ID로 Article 조회 (소유권 확인)")
        void findByIdAndUser() {
            // given
            Article article = articleRepository.save(Article.builder()
                    .user(savedUser)
                    .title("Owner Check Test")
                    .content("Content")
                    .build());

            // when
            Optional<Article> found = articleRepository.findByIdAndUser(article.getId(), savedUser);
            Optional<Article> notFound = articleRepository.findByIdAndUserId(article.getId(), 9999L);

            // then
            assertThat(found).isPresent();
            assertThat(notFound).isEmpty();
        }

        @Test
        @DisplayName("읽기 완료/미완료 아티클 조회")
        void findByUserAndIsCompleted() {
            // given
            Article completed = Article.builder()
                    .user(savedUser)
                    .title("Completed Article")
                    .content("Content")
                    .build();
            completed.markAsCompleted();
            articleRepository.save(completed);

            articleRepository.save(Article.builder()
                    .user(savedUser)
                    .title("Not Completed Article")
                    .content("Content")
                    .build());

            // when
            List<Article> completedList = articleRepository
                    .findByUserAndIsCompletedOrderByCreatedAtDesc(savedUser, true);
            List<Article> notCompletedList = articleRepository
                    .findByUserAndIsCompletedOrderByCreatedAtDesc(savedUser, false);

            // then
            assertThat(completedList).hasSize(1);
            assertThat(completedList.get(0).getTitle()).isEqualTo("Completed Article");
            assertThat(notCompletedList).hasSize(1);
            assertThat(notCompletedList.get(0).getTitle()).isEqualTo("Not Completed Article");
        }
    }

    @Nested
    @DisplayName("ArticleVocabulary 저장/조회 테스트")
    class VocabularyTest {

        @Test
        @DisplayName("Article ID로 Vocabulary 목록 조회")
        void findVocabularyByArticleId() {
            // given
            Article article = Article.builder()
                    .user(savedUser)
                    .title("Vocab Test Article")
                    .content("Content")
                    .build();
            article.addVocabulary(ArticleVocabulary.builder()
                    .word("serendipity")
                    .definition("the occurrence of events by chance in a happy way")
                    .build());
            article.addVocabulary(ArticleVocabulary.builder()
                    .word("eloquent")
                    .definition("fluent or persuasive in speaking or writing")
                    .build());
            Article savedArticle = articleRepository.save(article);

            // when
            List<ArticleVocabulary> vocabularies = articleVocabularyRepository
                    .findByArticleId(savedArticle.getId());

            // then
            assertThat(vocabularies).hasSize(2);
            assertThat(vocabularies)
                    .extracting(ArticleVocabulary::getWord)
                    .containsExactlyInAnyOrder("serendipity", "eloquent");
        }

        @Test
        @DisplayName("단어 검색")
        void findByWordContaining() {
            // given
            Article article = Article.builder()
                    .user(savedUser)
                    .title("Search Test")
                    .content("Content")
                    .build();
            article.addVocabulary(ArticleVocabulary.builder()
                    .word("programming")
                    .definition("the process of writing computer programs")
                    .build());
            article.addVocabulary(ArticleVocabulary.builder()
                    .word("programmer")
                    .definition("a person who writes computer programs")
                    .build());
            article.addVocabulary(ArticleVocabulary.builder()
                    .word("design")
                    .definition("a plan or drawing")
                    .build());
            articleRepository.save(article);

            // when
            List<ArticleVocabulary> result = articleVocabularyRepository
                    .findByWordContainingIgnoreCase("program");

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Article 삭제 테스트")
    class DeleteArticleTest {

        @Test
        @DisplayName("Article 삭제 시 Vocabulary도 함께 삭제 (OrphanRemoval)")
        void deleteArticleCascadesVocabulary() {
            // given
            Article article = Article.builder()
                    .user(savedUser)
                    .title("Delete Test")
                    .content("Content")
                    .build();
            article.addVocabulary(ArticleVocabulary.builder()
                    .word("test")
                    .definition("test definition")
                    .build());
            Article savedArticle = articleRepository.save(article);
            Long articleId = savedArticle.getId();

            assertThat(articleVocabularyRepository.findByArticleId(articleId)).hasSize(1);

            // when
            articleRepository.delete(savedArticle);

            // then
            assertThat(articleRepository.findById(articleId)).isEmpty();
            assertThat(articleVocabularyRepository.findByArticleId(articleId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Article 카운트 테스트")
    class CountArticleTest {

        @Test
        @DisplayName("사용자별 Article 개수 조회")
        void countByUser() {
            // given
            articleRepository.save(Article.builder()
                    .user(savedUser)
                    .title("Article 1")
                    .content("Content 1")
                    .build());
            articleRepository.save(Article.builder()
                    .user(savedUser)
                    .title("Article 2")
                    .content("Content 2")
                    .build());

            // when
            long count = articleRepository.countByUser(savedUser);

            // then
            assertThat(count).isEqualTo(2);
        }
    }
}
