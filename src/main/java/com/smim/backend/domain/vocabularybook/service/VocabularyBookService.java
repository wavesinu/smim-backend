package com.smim.backend.domain.vocabularybook.service;

import com.smim.backend.domain.article.Article;
import com.smim.backend.domain.article.ArticleRepository;
import com.smim.backend.domain.article.ArticleVocabulary;
import com.smim.backend.domain.article.ArticleVocabularyRepository;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.domain.vocabularybook.LearningStatus;
import com.smim.backend.domain.vocabularybook.VocabularyBook;
import com.smim.backend.domain.vocabularybook.VocabularyBookRepository;
import com.smim.backend.domain.vocabularybook.VocabularyEntry;
import com.smim.backend.domain.vocabularybook.VocabularyEntryRepository;
import com.smim.backend.domain.vocabularybook.dto.VocabularyBookCreateRequest;
import com.smim.backend.domain.vocabularybook.dto.VocabularyBookResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyBookUpdateRequest;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryCreateRequest;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryMoveRequest;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryMoveResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntrySaveResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryStatusResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryStatusUpdateRequest;
import com.smim.backend.domain.vocabularybook.dto.VocabularyWordManualCreateRequest;
import com.smim.backend.global.error.ErrorCode;
import com.smim.backend.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 단어장 서비스
 */
@Service
@RequiredArgsConstructor
public class VocabularyBookService {

    public static final String DEFAULT_BOOK_NAME = "기본 단어장";
    private static final int MAX_BOOKS = 20;

    private final VocabularyBookRepository vocabularyBookRepository;
    private final VocabularyEntryRepository vocabularyEntryRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final ArticleVocabularyRepository articleVocabularyRepository;

    /**
     * 기본 단어장 생성 (이미 존재하면 생성하지 않음)
     */
    @Transactional
    public void createDefaultBook(User user) {
        if (vocabularyBookRepository.existsByUserAndIsDefaultTrue(user)) {
            return;
        }

        VocabularyBook defaultBook = VocabularyBook.builder()
                .user(user)
                .name(DEFAULT_BOOK_NAME)
                .description(null)
                .isDefault(true)
                .wordCount(0)
                .build();

        vocabularyBookRepository.save(defaultBook);
    }

    @Transactional
    public VocabularyBookResponse createBook(Long userId, VocabularyBookCreateRequest request) {
        User user = getUserOrThrow(userId);
        if (vocabularyBookRepository.countByUser(user) >= MAX_BOOKS) {
            throw new BusinessException(ErrorCode.MAX_BOOKS_EXCEEDED);
        }
        if (vocabularyBookRepository.existsByUserAndName(user, request.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_BOOK_NAME);
        }

        VocabularyBook book = VocabularyBook.builder()
                .user(user)
                .name(request.getName().trim())
                .description(request.getDescription())
                .isDefault(false)
                .wordCount(0)
                .build();
        vocabularyBookRepository.save(book);
        return VocabularyBookResponse.from(book);
    }

    @Transactional(readOnly = true)
    public List<VocabularyBookResponse> getBooks(Long userId) {
        List<VocabularyBook> books = vocabularyBookRepository.findByUserId(userId);
        return books.stream().map(VocabularyBookResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VocabularyBookResponse getBook(Long userId, Long bookId) {
        return VocabularyBookResponse.from(getBookOrThrow(userId, bookId));
    }

    @Transactional
    public VocabularyBookResponse updateBook(Long userId, Long bookId, VocabularyBookUpdateRequest request) {
        VocabularyBook book = getBookOrThrow(userId, bookId);
        String newName = request.getName();
        if (book.isDefault() && newName != null && !newName.equals(book.getName())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (newName != null && !newName.equals(book.getName())) {
            if (vocabularyBookRepository.existsByUserAndName(book.getUser(), newName)) {
                throw new BusinessException(ErrorCode.DUPLICATE_BOOK_NAME);
            }
        }
        book.update(newName, request.getDescription());
        return VocabularyBookResponse.from(book);
    }

    @Transactional
    public void deleteBook(Long userId, Long bookId) {
        VocabularyBook book = getBookOrThrow(userId, bookId);
        if (book.isDefault()) {
            throw new BusinessException(ErrorCode.DEFAULT_BOOK_CANNOT_BE_DELETED);
        }
        vocabularyBookRepository.delete(book);
    }

    @Transactional
    public VocabularyEntrySaveResponse addEntries(Long userId, Long bookId, VocabularyEntryCreateRequest request) {
        boolean saveAll = Boolean.TRUE.equals(request.getSaveAll());
        return addEntriesFromArticle(userId, bookId, request.getArticleId(), request.getVocabularyIds(), saveAll);
    }

    @Transactional
    public VocabularyEntrySaveResponse addSelectiveWords(
            Long userId,
            Long bookId,
            Long articleId,
            List<Long> vocabularyIds
    ) {
        return addEntriesFromArticle(userId, bookId, articleId, vocabularyIds, false);
    }

    @Transactional
    public VocabularyEntrySaveResponse addBulkWords(Long userId, Long bookId, Long articleId) {
        return addEntriesFromArticle(userId, bookId, articleId, null, true);
    }

    @Transactional
    public VocabularyEntryResponse addManualWord(Long userId, Long bookId, VocabularyWordManualCreateRequest request) {
        VocabularyBook book = getBookOrThrow(userId, bookId);
        String normalizedWord = request.getWord().trim();
        if (vocabularyEntryRepository.existsByVocabularyBookIdAndWordIgnoreCase(book.getId(), normalizedWord)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ENTRY);
        }

        VocabularyEntry entry = VocabularyEntry.builder()
                .vocabularyBook(book)
                .word(normalizedWord)
                .definition(request.getDefinition().trim())
                .contextSentence(request.getContextSentence())
                .learningStatus(LearningStatus.NEW)
                .build();
        VocabularyEntry saved = vocabularyEntryRepository.save(entry);
        book.increaseWordCount(1);
        return VocabularyEntryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<VocabularyEntryResponse> getEntries(Long userId, Long bookId, Pageable pageable, String keyword) {
        getBookOrThrow(userId, bookId);
        Page<VocabularyEntry> page;
        if (keyword == null || keyword.isBlank()) {
            page = vocabularyEntryRepository.findByVocabularyBookIdAndVocabularyBookUserId(bookId, userId, pageable);
        } else {
            page = vocabularyEntryRepository.findByVocabularyBookIdAndVocabularyBookUserIdAndWordContainingIgnoreCase(
                    bookId,
                    userId,
                    keyword,
                    pageable
            );
        }
        return page.map(VocabularyEntryResponse::from);
    }

    @Transactional
    public void deleteEntry(Long userId, Long bookId, Long entryId) {
        VocabularyBook book = getBookOrThrow(userId, bookId);
        VocabularyEntry entry = vocabularyEntryRepository
                .findByIdAndVocabularyBookIdAndVocabularyBookUserId(entryId, book.getId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        vocabularyEntryRepository.delete(entry);
        book.increaseWordCount(-1);
    }

    @Transactional
    public VocabularyEntryMoveResponse moveEntries(Long userId, Long sourceBookId, VocabularyEntryMoveRequest request) {
        if (Objects.equals(sourceBookId, request.getTargetBookId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getEntryIds() == null || request.getEntryIds().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        VocabularyBook sourceBook = getBookOrThrow(userId, sourceBookId);
        VocabularyBook targetBook = getBookOrThrow(userId, request.getTargetBookId());

        List<VocabularyEntry> entries = vocabularyEntryRepository
                .findByIdInAndVocabularyBookIdAndVocabularyBookUserId(request.getEntryIds(), sourceBookId, userId);

        int movedCount = 0;
        int duplicateCount = 0;

        for (VocabularyEntry entry : entries) {
            if (vocabularyEntryRepository.existsByVocabularyBookIdAndWordIgnoreCase(targetBook.getId(), entry.getWord())) {
                duplicateCount++;
                continue;
            }
            entry.updateVocabularyBook(targetBook);
            movedCount++;
        }

        sourceBook.increaseWordCount(-movedCount);
        targetBook.increaseWordCount(movedCount);

        return VocabularyEntryMoveResponse.builder()
                .movedCount(movedCount)
                .duplicateCount(duplicateCount)
                .sourceBookId(sourceBook.getId())
                .targetBookId(targetBook.getId())
                .build();
    }

    @Transactional
    public VocabularyEntryStatusResponse updateEntryStatus(
            Long userId,
            Long bookId,
            Long entryId,
            VocabularyEntryStatusUpdateRequest request
    ) {
        getBookOrThrow(userId, bookId);
        VocabularyEntry entry = vocabularyEntryRepository
                .findByIdAndVocabularyBookIdAndVocabularyBookUserId(entryId, bookId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        entry.updateLearningStatus(request.getLearningStatus());
        return VocabularyEntryStatusResponse.builder()
                .entryId(entry.getId())
                .learningStatus(entry.getLearningStatus())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private VocabularyBook getBookOrThrow(Long userId, Long bookId) {
        VocabularyBook book = vocabularyBookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCABULARY_BOOK_NOT_FOUND));
        if (!Objects.equals(book.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return book;
    }

    private void validateArticleOwnership(Long userId, Article article) {
        if (!Objects.equals(article.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private VocabularyEntrySaveResponse addEntriesFromArticle(
            Long userId,
            Long bookId,
            Long articleId,
            List<Long> vocabularyIds,
            boolean saveAll
    ) {
        VocabularyBook book = getBookOrThrow(userId, bookId);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        validateArticleOwnership(userId, article);

        if (!article.isCompleted()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (!saveAll && (vocabularyIds == null || vocabularyIds.isEmpty())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        List<ArticleVocabulary> vocabularies = saveAll
                ? articleVocabularyRepository.findByArticleId(article.getId())
                : articleVocabularyRepository.findAllById(vocabularyIds)
                .stream()
                .filter(vocab -> Objects.equals(vocab.getArticle().getId(), article.getId()))
                .toList();

        int savedCount = 0;
        int duplicateCount = 0;
        List<VocabularyEntry> toSave = new ArrayList<>();

        for (ArticleVocabulary vocab : vocabularies) {
            if (vocabularyEntryRepository.existsByVocabularyBookIdAndWordIgnoreCase(book.getId(), vocab.getWord())) {
                duplicateCount++;
                continue;
            }
            VocabularyEntry entry = VocabularyEntry.builder()
                    .vocabularyBook(book)
                    .word(vocab.getWord())
                    .definition(vocab.getDefinition())
                    .contextSentence(vocab.getContextSentence())
                    .sourceArticle(article)
                    .learningStatus(LearningStatus.NEW)
                    .build();
            toSave.add(entry);
            savedCount++;
        }

        vocabularyEntryRepository.saveAll(toSave);
        book.increaseWordCount(savedCount);

        return VocabularyEntrySaveResponse.builder()
                .savedCount(savedCount)
                .duplicateCount(duplicateCount)
                .bookId(book.getId())
                .bookName(book.getName())
                .build();
    }
}
