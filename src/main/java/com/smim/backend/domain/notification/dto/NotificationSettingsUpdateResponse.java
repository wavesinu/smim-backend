package com.smim.backend.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationSettingsUpdateResponse {
    private boolean emailEnabled;
    private boolean kakaoEnabled;
    private String preferredTime;
    private String timezone;
    private Instant updatedAt;
}
