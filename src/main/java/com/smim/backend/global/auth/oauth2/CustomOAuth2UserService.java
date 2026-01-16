package com.smim.backend.global.auth.oauth2;

import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.auth.oauth2.user.OAuth2UserInfo;
import com.smim.backend.global.auth.oauth2.user.OAuth2UserInfoFactory;
import com.smim.backend.global.error.exception.OAuth2AuthenticationProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 커스텀 OAuth2 사용자 서비스
 * OAuth2 로그인 성공 시 호출되어 사용자 정보를 처리합니다.
 * - 신규 사용자: DB에 저장
 * - 기존 사용자: 정보 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * OAuth2 사용자 정보 로드
     * Spring Security가 OAuth2 인증 후 자동으로 호출합니다.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationProcessingException(ex.getMessage(), ex);
        }
    }

    /**
     * OAuth2 사용자 정보 처리
     * Provider별로 적절한 UserInfo 구현체를 생성하고 사용자를 등록/업데이트합니다.
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId,
                oAuth2User.getAttributes()
        );

        Provider provider = Provider.valueOf(registrationId.toUpperCase());
        User user = registerOrUpdateUser(oAuth2UserInfo, provider);

        return UserPrincipal.create(user);
    }

    /**
     * 사용자 등록 또는 업데이트
     * providerId로 기존 사용자를 찾아 업데이트하거나, 없으면 신규 등록합니다.
     */
    private User registerOrUpdateUser(OAuth2UserInfo oAuth2UserInfo, Provider provider) {
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

        return userRepository.save(user);
    }
}
