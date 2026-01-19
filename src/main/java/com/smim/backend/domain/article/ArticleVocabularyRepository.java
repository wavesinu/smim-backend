package com.smim.backend.domain.article;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * ArticleVocabulary 엔티티를 위한 Repository
 */
public interface ArticleVocabularyRepository extends JpaRepository<ArticleVocabulary, Long> {

    /**
     * 특정 아티클의 모든 단어 조회
     */
    List<ArticleVocabulary> findByArticle(Article article);

    /**
     * 특정 아티클의 단어 조회 (Article ID 기준)
     */
    List<ArticleVocabulary> findByArticleId(Long articleId);

    /**
     * 특정 단어가 포함된 모든 단어 조회
     */
    List<ArticleVocabulary> findByWordContainingIgnoreCase(String word);

    /**
     * 특정 아티클의 단어 삭제
     */
    void deleteByArticle(Article article);

    /**
     * 특정 아티클의 단어 개수
     */
    long countByArticle(Article article);
}
