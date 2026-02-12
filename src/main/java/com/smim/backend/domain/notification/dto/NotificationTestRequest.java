package com.smim.backend.domain.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationTestRequest {
    @NotNull(message = "channel은 필수입니다.")
    private NotificationTestChannel channel;
}
