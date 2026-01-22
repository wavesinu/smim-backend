package com.smim.backend.domain.article.api;

import com.smim.backend.domain.article.dto.ArticleCrawlRequest;
import com.smim.backend.domain.article.dto.ArticleResponse;
import com.smim.backend.domain.article.service.ArticleService;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 아티클 API 컨트롤러
 * 아티클 크롤링, 조회, 관리 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

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
        ArticleResponse response = articleService.crawlAndSaveArticle(
                request.getUrl(),
                userPrincipal.getId()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
