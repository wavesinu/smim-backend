package com.smim.backend.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationTestResponse {
    private boolean sent;
    private NotificationTestChannel channel;
    private Instant sentAt;
}
