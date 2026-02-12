package com.smim.backend.domain.learning.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UpdatedScheduleItem {
    private Long entryId;
    private Instant nextReviewAt;
}
