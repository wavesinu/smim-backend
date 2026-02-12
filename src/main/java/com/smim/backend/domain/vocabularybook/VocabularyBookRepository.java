package com.smim.backend.domain.vocabularybook;

import com.smim.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VocabularyBookRepository extends JpaRepository<VocabularyBook, Long> {
    boolean existsByUserAndIsDefaultTrue(User user);

    boolean existsByUserAndName(User user, String name);

    long countByUser(User user);

    Optional<VocabularyBook> findByIdAndUserId(Long id, Long userId);

    List<VocabularyBook> findByUserId(Long userId);
}
