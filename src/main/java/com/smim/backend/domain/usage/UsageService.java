package com.smim.backend.domain.usage;

import com.smim.backend.domain.usage.dto.DailyUsageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsageService {

    private final RateLimitService rateLimitService;

    public DailyUsageResponse getDailyUsage(Long userId) {
        RateLimitStatus status = rateLimitService.getStatus(userId);
        return DailyUsageResponse.builder()
                .used(status.getUsed())
                .limit(status.getLimit())
                .remaining(status.getRemaining())
                .isUnlimited(status.isUnlimited())
                .resetAt(status.getResetAt())
                .build();
    }
}
