package com.smim.backend.domain.user.service;

import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.auth.oauth2.user.OAuth2UserInfo;
import com.smim.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사용자 인증 관련 서비스
 * global.auth 계층에서 User 도메인에 접근할 때 사용합니다.
 * Repository 직접 참조 대신 이 서비스를 통해 간접 참조하여 순환 의존성을 방지합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPrincipalService {

    private final UserRepository userRepository;

    /**
     * 사용자 ID로 UserPrincipal 조회
     * JwtAuthenticationFilter에서 사용
     *
     * @param userId 사용자 ID
     * @return UserPrincipal
     * @throws IllegalArgumentException 사용자를 찾을 수 없는 경우
     */
    public UserPrincipal loadUserPrincipalById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        ErrorCode.USER_NOT_FOUND.getMessage()));
        return UserPrincipal.create(user);
    }

    /**
     * OAuth2 사용자 등록 또는 업데이트
     * CustomOAuth2UserService에서 사용
     *
     * @param oAuth2UserInfo OAuth2 사용자 정보
     * @param provider OAuth2 제공자
     * @return UserPrincipal
     */
    @Transactional
    public UserPrincipal registerOrUpdateOAuth2User(OAuth2UserInfo oAuth2UserInfo, Provider provider) {
        Optional<User> userOptional = userRepository.findByProviderAndProviderId(
                provider,
                oAuth2UserInfo.getProviderId()
        );

        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user = user.update(oAuth2UserInfo.getName(), oAuth2UserInfo.getImageUrl());
        } else {
            user = User.builder()
                    .email(oAuth2UserInfo.getEmail())
                    .name(oAuth2UserInfo.getName())
                    .profileImage(oAuth2UserInfo.getImageUrl())
                    .provider(provider)
                    .providerId(oAuth2UserInfo.getProviderId())
                    .role(Role.USER)
                    .build();
        }

        user = userRepository.save(user);
        return UserPrincipal.create(user);
    }
}
