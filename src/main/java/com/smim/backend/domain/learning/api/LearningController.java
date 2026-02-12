package com.smim.backend.domain.learning.api;

import com.smim.backend.domain.learning.LearningService;
import com.smim.backend.domain.learning.dto.LearningPeriodStatsResponse;
import com.smim.backend.domain.learning.dto.LearningStatsResponse;
import com.smim.backend.domain.learning.dto.ReviewWordsResponse;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.common.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Validated
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;

    @GetMapping("/review-words")
    public ResponseEntity<ApiResponse<ReviewWordsResponse>> getReviewWords(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            @RequestParam(required = false) Long bookId
    ) {
        ReviewWordsResponse response = learningService.getReviewWords(userPrincipal.getId(), limit, bookId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<LearningStatsResponse>> getLearningStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        LearningStatsResponse response = learningService.getLearningStats(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats/daily")
    public ResponseEntity<ApiResponse<LearningPeriodStatsResponse>> getDailyStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        LearningPeriodStatsResponse response = learningService.getLearningStatsByPeriod(
                userPrincipal.getId(),
                "DAILY",
                Duration.ofDays(1)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats/weekly")
    public ResponseEntity<ApiResponse<LearningPeriodStatsResponse>> getWeeklyStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        LearningPeriodStatsResponse response = learningService.getLearningStatsByPeriod(
                userPrincipal.getId(),
                "WEEKLY",
                Duration.ofDays(7)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats/monthly")
    public ResponseEntity<ApiResponse<LearningPeriodStatsResponse>> getMonthlyStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        LearningPeriodStatsResponse response = learningService.getLearningStatsByPeriod(
                userPrincipal.getId(),
                "MONTHLY",
                Duration.ofDays(30)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
