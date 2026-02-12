package com.smim.backend.domain.notification;

import com.smim.backend.domain.notification.dto.NotificationSettingsResponse;
import com.smim.backend.domain.notification.dto.NotificationSettingsUpdateRequest;
import com.smim.backend.domain.notification.dto.NotificationSettingsUpdateResponse;
import com.smim.backend.domain.notification.dto.NotificationTestChannel;
import com.smim.backend.domain.notification.dto.NotificationTestResponse;
import com.smim.backend.domain.user.NotificationChannel;
import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.global.error.ErrorCode;
import com.smim.backend.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getSettings(Long userId) {
        User user = getUserOrThrow(userId);
        return NotificationSettingsResponse.builder()
                .emailEnabled(user.getNotificationChannel().requiresEmail())
                .kakaoEnabled(user.getNotificationChannel().requiresKakao())
                .preferredTime(user.getNotificationTime() == null ? null : user.getNotificationTime().toString())
                .timezone(user.getNotificationTimezone() == null ? "Asia/Seoul" : user.getNotificationTimezone())
                .minWordsForNotification(user.getMinWordsForNotification())
                .build();
    }

    @Transactional
    public NotificationSettingsUpdateResponse updateSettings(Long userId, NotificationSettingsUpdateRequest request) {
        User user = getUserOrThrow(userId);

        boolean currentEmail = user.getNotificationChannel().requiresEmail();
        boolean currentKakao = user.getNotificationChannel().requiresKakao();

        boolean emailEnabled = request.getEmailEnabled() == null ? currentEmail : request.getEmailEnabled();
        boolean kakaoEnabled = request.getKakaoEnabled() == null ? currentKakao : request.getKakaoEnabled();

        if (kakaoEnabled && user.getProvider() != Provider.KAKAO) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        NotificationChannel channel = toChannel(emailEnabled, kakaoEnabled);
        boolean enabled = emailEnabled || kakaoEnabled;

        LocalTime preferredTime = null;
        if (request.getPreferredTime() != null) {
            try {
                preferredTime = LocalTime.parse(request.getPreferredTime());
            } catch (DateTimeParseException ex) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }

        String timezone = null;
        if (request.getTimezone() != null) {
            try {
                ZoneId.of(request.getTimezone());
                timezone = request.getTimezone();
            } catch (Exception ex) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }

        user.updateNotificationSettings(channel, enabled, preferredTime, timezone);

        return NotificationSettingsUpdateResponse.builder()
                .emailEnabled(channel.requiresEmail())
                .kakaoEnabled(channel.requiresKakao())
                .preferredTime(user.getNotificationTime() == null ? null : user.getNotificationTime().toString())
                .timezone(user.getNotificationTimezone())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationTestResponse sendTest(Long userId, NotificationTestChannel channel) {
        User user = getUserOrThrow(userId);
        if (channel == NotificationTestChannel.KAKAO && user.getProvider() != Provider.KAKAO) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return NotificationTestResponse.builder()
                .sent(true)
                .channel(channel)
                .sentAt(Instant.now())
                .build();
    }

    private NotificationChannel toChannel(boolean emailEnabled, boolean kakaoEnabled) {
        if (emailEnabled && kakaoEnabled) {
            return NotificationChannel.BOTH;
        }
        if (emailEnabled) {
            return NotificationChannel.EMAIL;
        }
        if (kakaoEnabled) {
            return NotificationChannel.KAKAO;
        }
        return NotificationChannel.NONE;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
