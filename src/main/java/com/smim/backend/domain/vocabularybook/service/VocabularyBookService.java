package com.smim.backend.domain.vocabularybook.service;

import com.smim.backend.domain.user.User;
import com.smim.backend.domain.vocabularybook.VocabularyBook;
import com.smim.backend.domain.vocabularybook.VocabularyBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 단어장 서비스
 */
@Service
@RequiredArgsConstructor
public class VocabularyBookService {

    public static final String DEFAULT_BOOK_NAME = "기본 단어장";

    private final VocabularyBookRepository vocabularyBookRepository;

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
}
