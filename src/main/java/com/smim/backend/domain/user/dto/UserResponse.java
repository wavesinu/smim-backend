package com.smim.backend.domain.user.dto;

import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 사용자 정보 응답 DTO
 * API Spec: 6.2.1 Get My Profile Response
 */
@Getter
@Builder
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private String profileImage;
    private Provider provider;
    private Role role;
    private Instant createdAt;

    public static UserResponse from(User user) {
        // Builder 패턴을 사용하도록 수정
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImage(),
                user.getProvider(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
