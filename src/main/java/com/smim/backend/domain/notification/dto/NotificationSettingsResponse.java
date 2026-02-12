package com.smim.backend.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationSettingsResponse {
    private boolean emailEnabled;
    private boolean kakaoEnabled;
    private String preferredTime;
    private String timezone;
    private int minWordsForNotification;
}
