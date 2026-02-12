package com.smim.backend.domain.usage;

import com.smim.backend.domain.usage.dto.DailyUsageResponse;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailyUsageResponse>> getDailyUsage(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        DailyUsageResponse response = usageService.getDailyUsage(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
