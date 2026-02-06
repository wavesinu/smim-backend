package com.smim.backend.domain.user;

/**
 * 알림 수신 채널
 */
public enum NotificationChannel {
    EMAIL,
    KAKAO,
    BOTH,
    NONE;

    public boolean requiresEmail() {
        return this == EMAIL || this == BOTH;
    }

    public boolean requiresKakao() {
        return this == KAKAO || this == BOTH;
    }
}
