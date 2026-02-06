package com.smim.backend.domain.vocabularybook;

import com.smim.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabularyBookRepository extends JpaRepository<VocabularyBook, Long> {
    boolean existsByUserAndIsDefaultTrue(User user);
}
