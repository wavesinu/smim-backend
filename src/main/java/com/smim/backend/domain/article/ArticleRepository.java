package com.smim.backend.domain.article;

import com.smim.backend.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Article 엔티티를 위한 Repository
 */
public interface ArticleRepository extends JpaRepository<Article, Long> {

    /**
     * 사용자의 모든 아티클 조회 (최신순)
     */
    List<Article> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 사용자의 아티클 페이징 조회
     */
    Page<Article> findByUser(User user, Pageable pageable);

    /**
     * 사용자 ID로 아티클 조회
     */
    List<Article> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자의 특정 아티클 조회 (소유권 확인용)
     */
    Optional<Article> findByIdAndUser(Long id, User user);

    /**
     * 사용자의 특정 아티클 조회 (userId 기준)
     */
    Optional<Article> findByIdAndUserId(Long id, Long userId);

    /**
     * 사용자의 읽기 완료/미완료 아티클 조회
     */
    List<Article> findByUserAndIsCompletedOrderByCreatedAtDesc(User user, boolean isCompleted);

    /**
     * 특정 도메인의 아티클 검색
     */
    List<Article> findBySourceDomainContaining(String domain);

    /**
     * 사용자의 아티클 개수
     */
    long countByUser(User user);

    /**
     * 아티클과 단어 목록을 함께 조회 (N+1 방지)
     */
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.vocabularyList WHERE a.id = :id")
    Optional<Article> findByIdWithVocabulary(@Param("id") Long id);

    /**
     * 사용자의 아티클과 단어 목록을 함께 조회
     */
    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.vocabularyList WHERE a.user = :user ORDER BY a.createdAt DESC")
    List<Article> findByUserWithVocabulary(@Param("user") User user);
}
