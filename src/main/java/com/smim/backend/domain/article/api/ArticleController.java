package com.smim.backend.domain.article.api;

import com.smim.backend.domain.article.dto.ArticleCrawlRequest;
import com.smim.backend.domain.article.dto.ArticleDifficultyResponse;
import com.smim.backend.domain.article.dto.ArticleExtractionStatusResponse;
import com.smim.backend.domain.article.dto.ArticleReextractResponse;
import com.smim.backend.domain.article.dto.ArticleResponse;
import com.smim.backend.domain.article.dto.ArticleSummaryResponse;
import com.smim.backend.domain.user.CefrLevel;
import com.smim.backend.domain.article.service.ArticleService;
import com.smim.backend.domain.usage.RateLimitService;
import com.smim.backend.domain.usage.RateLimitStatus;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.common.response.ApiResponse;
import com.smim.backend.global.common.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 아티클 API 컨트롤러
 * 아티클 크롤링, 조회, 관리 엔드포인트를 제공합니다.
 */
@Validated
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final RateLimitService rateLimitService;

    /**
     * URL 크롤링 및 아티클 생성
     * 외부 URL을 크롤링하여 아티클을 생성하고 AI 단어 추출을 시작합니다.
     *
     * @param request       크롤링 요청 (URL 포함)
     * @param userPrincipal 현재 로그인한 사용자 정보
     * @return 생성된 아티클 정보
     */
    @PostMapping("/crawl")
    public ResponseEntity<ApiResponse<ArticleResponse>> crawlArticle(
            @Valid @RequestBody ArticleCrawlRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        RateLimitStatus rateLimitStatus = rateLimitService.consume(userPrincipal.getId());
        ArticleResponse response = articleService.crawlAndSaveArticle(
                request.getUrl(),
                userPrincipal.getId()
        );
        HttpHeaders headers = rateLimitStatus.toHeaders();
        return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ArticleSummaryResponse>>> getMyArticles(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, toSort(sort));
        Page<ArticleSummaryResponse> articlePage = articleService.getMyArticles(userPrincipal.getId(), pageRequest);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(articlePage)));
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticleDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long articleId
    ) {
        ArticleResponse response = articleService.getArticleDetail(userPrincipal.getId(), articleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<Void> deleteArticle(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long articleId
    ) {
        articleService.deleteArticle(userPrincipal.getId(), articleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{articleId}/re-extract")
    public ResponseEntity<ApiResponse<ArticleReextractResponse>> reextractVocabulary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long articleId
    ) {
        RateLimitStatus rateLimitStatus = rateLimitService.consume(userPrincipal.getId());
        ArticleReextractResponse response = articleService.reextractVocabulary(userPrincipal.getId(), articleId);
        return ResponseEntity.ok().headers(rateLimitStatus.toHeaders()).body(ApiResponse.success(response));
    }

    @GetMapping("/{articleId}/extraction-status")
    public ResponseEntity<ApiResponse<ArticleExtractionStatusResponse>> getExtractionStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long articleId
    ) {
        ArticleExtractionStatusResponse response = articleService.getExtractionStatus(userPrincipal.getId(), articleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{articleId}/difficulty")
    public ResponseEntity<ApiResponse<ArticleDifficultyResponse>> getArticleDifficulty(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long articleId
    ) {
        ArticleDifficultyResponse response = articleService.getArticleDifficulty(userPrincipal.getId(), articleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<PageResponse<ArticleSummaryResponse>>> getRecommendations(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "1") @Min(0) @Max(3) int levelRange
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ArticleSummaryResponse> pageResult = articleService.getRecommendedArticles(
                userPrincipal.getId(),
                pageRequest,
                levelRange
        );
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(pageResult)));
    }

    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<PageResponse<ArticleSummaryResponse>>> getRecommendedArticles(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "1") @Min(0) @Max(3) int levelRange
    ) {
        return getRecommendations(userPrincipal, page, size, levelRange);
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<PageResponse<ArticleSummaryResponse>>> getTrendingArticles(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(required = false) CefrLevel cefrLevel
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ArticleSummaryResponse> pageResult = articleService.getTrendingArticles(pageRequest, cefrLevel);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(pageResult)));
    }

    private Sort toSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, parts[0]);
    }
}
