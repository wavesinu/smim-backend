package com.smim.backend.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordResetRequestResponse {
    private boolean requested;
    private long expiresInSeconds;
}
