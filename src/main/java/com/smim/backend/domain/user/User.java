package com.smim.backend.domain.user;

import com.smim.backend.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 사용자 엔티티
 * OAuth2 소셜 로그인 사용자 정보를 저장합니다.
 * BaseEntity를 상속받아 생성일시/수정일시가 자동으로 관리됩니다.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_provider_provider_id", columnNames = {"provider", "providerId"})
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이메일 주소 (선택, 소셜 로그인 제공자 정책에 따라 제공 여부 결정) */
    @Column(unique = true)
    private String email;

    /** 사용자 이름 (필수) */
    @Column(nullable = false)
    private String name;

    /** 프로필 이미지 URL */
    private String profileImage;

    /** OAuth2 제공자 (GOOGLE, KAKAO, etc.) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    /** OAuth2 제공자에서 제공하는 사용자 고유 ID */
    private String providerId;

    /** 사용자 권한 (USER, ADMIN) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** 향후 이메일 로그인을 위한 비밀번호 필드 (현재는 nullable) */
    private String password;

    /** 사용자 목표 CEFR 레벨 */
    @Enumerated(EnumType.STRING)
    private CefrLevel targetCefrLevel;

    /** 알림 활성화 여부 */
    @Column(nullable = false)
    private boolean notificationEnabled;

    /** 알림 수신 채널 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel notificationChannel;

    /** 알림 수신 시간 (HH:mm) */
    private LocalTime notificationTime;

    /** 알림 타임존 */
    @Column(length = 50)
    private String notificationTimezone;

    /** 알림 최소 단어 수 */
    @Column(nullable = false)
    private int minWordsForNotification;

    /**
     * User 엔티티 생성자
     * @param email 이메일 주소
     * @param name 사용자 이름
     * @param profileImage 프로필 이미지 URL
     * @param provider OAuth2 제공자
     * @param providerId 제공자별 사용자 고유 ID
     * @param role 사용자 권한
     */
    @Builder
    public User(String email, String name, String profileImage,
                Provider provider, String providerId, Role role,
                String password, CefrLevel targetCefrLevel,
                Boolean notificationEnabled, NotificationChannel notificationChannel,
                LocalTime notificationTime,
                String notificationTimezone,
                Integer minWordsForNotification) {
        this.email = email;
        this.name = name;
        this.profileImage = profileImage;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
        this.password = password;
        this.targetCefrLevel = targetCefrLevel;
        this.notificationEnabled = notificationEnabled != null && notificationEnabled;
        this.notificationChannel = notificationChannel == null ? NotificationChannel.NONE : notificationChannel;
        this.notificationTime = notificationTime;
        this.notificationTimezone = notificationTimezone == null ? "Asia/Seoul" : notificationTimezone;
        this.minWordsForNotification = minWordsForNotification == null ? 3 : minWordsForNotification;
    }

    /**
     * 사용자 정보 업데이트
     * OAuth2 로그인 시 사용자 정보가 변경되었을 때 업데이트합니다.
     * @param name 새로운 이름
     * @param profileImage 새로운 프로필 이미지 URL
     * @return 업데이트된 User 엔티티
     */
    public User update(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
        return this;
    }

    /**
     * 사용자 프로필 정보 부분 업데이트
     */
    public void updateProfile(String name, String profileImage) {
        if (name != null) {
            this.name = name;
        }
        if (profileImage != null) {
            this.profileImage = profileImage;
        }
    }

    public void updateNotificationSettings(
            NotificationChannel notificationChannel,
            Boolean notificationEnabled,
            LocalTime notificationTime,
            String notificationTimezone
    ) {
        if (notificationChannel != null) {
            this.notificationChannel = notificationChannel;
        }
        if (notificationEnabled != null) {
            this.notificationEnabled = notificationEnabled;
        }
        if (notificationTime != null) {
            this.notificationTime = notificationTime;
        }
        if (notificationTimezone != null) {
            this.notificationTimezone = notificationTimezone;
        }
    }

    public void updateMinWordsForNotification(Integer minWordsForNotification) {
        if (minWordsForNotification != null) {
            this.minWordsForNotification = minWordsForNotification;
        }
    }

    /**
     * CEFR 레벨 업데이트
     */
    public void updateCefrLevel(CefrLevel targetCefrLevel) {
        this.targetCefrLevel = targetCefrLevel;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
