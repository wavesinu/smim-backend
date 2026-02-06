package com.smim.backend.domain.user.service;

import com.smim.backend.domain.user.CefrLevel;
import com.smim.backend.domain.user.NotificationChannel;
import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.domain.user.dto.UpdateCefrLevelRequest;
import com.smim.backend.domain.user.dto.UpdateCefrLevelResponse;
import com.smim.backend.domain.user.dto.UpdateUserRequest;
import com.smim.backend.domain.user.dto.UserResponse;
import com.smim.backend.global.error.ErrorCode;
import com.smim.backend.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 사용자 프로필 서비스
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final DateTimeFormatter NOTIFICATION_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Long userId) {
        User user = getUserOrThrow(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyProfile(Long userId, UpdateUserRequest request) {
        User user = getUserOrThrow(userId);

        String name = normalizeOptional(request.getName(), "name");
        String profileImage = normalizeOptional(request.getProfileImage(), "profileImage");
        String cefrLevelValue = normalizeOptional(request.getTargetCefrLevel(), "targetCefrLevel");
        String channelValue = normalizeOptional(request.getNotificationChannel(), "notificationChannel");
        String notificationTimeValue = normalizeOptional(request.getNotificationTime(), "notificationTime");

        CefrLevel targetCefrLevel = parseCefrLevel(cefrLevelValue);
        NotificationChannel notificationChannel = parseNotificationChannel(channelValue);
        LocalTime notificationTime = parseNotificationTime(notificationTimeValue);

        validateNotificationChannel(user, notificationChannel);

        user.updateProfile(
                name,
                profileImage,
                targetCefrLevel,
                request.getNotificationEnabled(),
                notificationChannel,
                notificationTime
        );

        return UserResponse.from(user);
    }

    @Transactional
    public UpdateCefrLevelResponse updateCefrLevel(Long userId, UpdateCefrLevelRequest request) {
        User user = getUserOrThrow(userId);
        CefrLevel targetCefrLevel = parseCefrLevel(request.getCefrLevel());
        if (targetCefrLevel == null) {
            throw new BusinessException(ErrorCode.INVALID_CEFR_LEVEL);
        }

        user.updateCefrLevel(targetCefrLevel);
        userRepository.save(user);
        return new UpdateCefrLevelResponse(user.getTargetCefrLevel(), user.getUpdatedAt());
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalizeOptional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, fieldName + "은(는) 빈 문자열일 수 없습니다.");
        }
        return value.trim();
    }

    private CefrLevel parseCefrLevel(String value) {
        if (value == null) {
            return null;
        }
        try {
            return CefrLevel.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_CEFR_LEVEL);
        }
    }

    private NotificationChannel parseNotificationChannel(String value) {
        if (value == null) {
            return null;
        }
        try {
            return NotificationChannel.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private LocalTime parseNotificationTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalTime.parse(value, NOTIFICATION_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private void validateNotificationChannel(User user, NotificationChannel notificationChannel) {
        if (notificationChannel == null) {
            return;
        }

        if (notificationChannel.requiresKakao() && user.getProvider() != Provider.KAKAO) {
            throw new BusinessException(ErrorCode.KAKAO_NOT_LINKED);
        }
        if (notificationChannel.requiresEmail() && (user.getEmail() == null || user.getEmail().isBlank())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }
}
