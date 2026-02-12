package com.smim.backend.domain.notification.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationSettingsUpdateRequest {
    private Boolean emailEnabled;
    private Boolean kakaoEnabled;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "preferredTime은 HH:mm 형식이어야 합니다.")
    private String preferredTime;

    private String timezone;
}
