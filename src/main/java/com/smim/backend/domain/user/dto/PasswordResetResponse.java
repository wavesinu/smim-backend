package com.smim.backend.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PasswordResetResponse {
    private boolean reset;
    private Instant updatedAt;
}
