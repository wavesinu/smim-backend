package com.smim.backend.domain.usage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class DailyUsageResponse {
    private int used;
    private int limit;
    private int remaining;
    @JsonProperty("isUnlimited")
    private boolean isUnlimited;
    private OffsetDateTime resetAt;
}
