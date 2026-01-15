package com.smim.backend.global.auth.oauth2.user;

import com.smim.backend.domain.user.Provider;
import com.smim.backend.global.error.exception.OAuth2AuthenticationProcessingException;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(Provider.KAKAO.name())) {
            return new KakaoOAuth2UserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationProcessingException("Unsupported provider: " + registrationId);
        }
    }
}
